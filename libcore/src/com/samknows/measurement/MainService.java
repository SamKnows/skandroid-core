package com.samknows.measurement;

import org.json.JSONObject;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;

import com.samknows.libcore.SKLogger;
import com.samknows.libcore.SKConstants;
import com.samknows.measurement.activity.components.UIUpdate;
import com.samknows.measurement.environment.TrafficStatsCollector;
import com.samknows.measurement.schedule.ScheduleConfig;
import com.samknows.measurement.statemachine.ContinuousTesting;
import com.samknows.measurement.statemachine.State;
import com.samknows.measurement.statemachine.ScheduledTestStateMachine;
import com.samknows.measurement.statemachine.state.ExecuteScheduledTestQueueState;
import com.samknows.measurement.test.ScheduledTestExecutionQueue;
import com.samknows.measurement.util.OtherUtils;

public class MainService extends IntentService {
	public static final String FORCE_EXECUTION_EXTRA	= "force_execution";		
	public static final String EXECUTE_CONTINUOUS_EXTRA	= "execute_continuous";
	public static final String FORCE_EXECUTION_NOW_EXTRA	= "force_execution_NOW";		
	private PowerManager.WakeLock wakeLock;
	private TrafficStatsCollector collector;
	private SK2AppSettings appSettings;
	private static boolean isExecuting;
	private static boolean isExecutingContinuous;
	private static Handler mActivationHandler = null;
	private static Handler mContinuousHandler = null;
	private static Object sync = new Object();
	public MainService() {
		super(MainService.class.getName());
	}
	
	
	
	
	//the binder is used by the continuous testing in order to stop
	//the testing.
	private final IBinder mMainServiceBinder = new MainServiceBinder();
	
	public class MainServiceBinder extends Binder{
		public MainService getService(){
			return MainService.this;
		}
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		SKLogger.d(this, "+++++DEBUG+++++ MainService onBind");
		return mMainServiceBinder;
		
	}

	@Override
	public void onCreate() {
		super.onCreate();
		SKLogger.d(this, "+++++DEBUG+++++ MainService onCreate");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		SKLogger.d(this, "+++++DEBUG+++++ MainService onDestroy");
	}

	long accumulatedTestBytes = 0L;
	
	@Override
	protected void onHandleIntent(Intent intent) {
		
		accumulatedTestBytes = 0L;
		
		SKLogger.d(this, "+++++DEBUG+++++ MainService onHandleIntent" + intent.toString());
		
		boolean force_execution = intent.getBooleanExtra(FORCE_EXECUTION_EXTRA, false);
		isExecutingContinuous = intent.getBooleanExtra(EXECUTE_CONTINUOUS_EXTRA, false);
		
		boolean force_execution_NOW = intent.getBooleanExtra(FORCE_EXECUTION_NOW_EXTRA, false);
		if (force_execution_NOW == true) {
			ScheduledTestExecutionQueue.sbForceCanExecuteNow = true;
		}
		
		try {
			appSettings = SK2AppSettings.getSK2AppSettingsInstance();
			
			ScheduleConfig config = CachingStorage.getInstance().loadScheduleConfig();
			
			// This looks first at user preferences - if the user has set a true/false value, that value is returned.
			// otherwise, we return the "background_test" value last delivered from the config/schedule.
			boolean backgroundTest = SK2AppSettings.getSK2AppSettingsInstance().getIsBackgroundTestingEnabledInUserPreferences();
			
			onBegin();
			/* 
			 * The state machine has to be executed when background test is set in the config file and the service
			 * is enabled in the app settings.
			 * Moreover the state machine has to be executed whenever the user force the activation (force_execution = true)
			 * In case the device is in roaming the state machine shouldn't run any test
			 * 
			 * if the intent contains the EXECUTE_CONTINUOUS_EXTRA  run the continuous testing procedure and ignore the rest.
			 * 
			 */
			if(isExecutingContinuous){
				continuousStarted();
				new ContinuousTesting(this).execute();
			}
			else if((backgroundTest && appSettings.getIsBackgroundTestingEnabledInUserPreferences()) || force_execution ) {
				if (appSettings.run_in_roaming || !OtherUtils.isRoaming(this)) {
         			accumulatedTestBytes = new ScheduledTestStateMachine(this).executeRoutine();
				} else {
					SKLogger.d(this, "+++++DEBUG+++++ Service disabled(roaming), exiting.");
					OtherUtils.reschedule(this,	SKConstants.SERVICE_RESCHEDULE_IF_ROAMING_OR_DATACAP);
				}
			} else {
				if(!backgroundTest)
					SKLogger.d(this, "+++++DEBUG+++++ Service disabled(config file), exiting.");
				if (!appSettings.getIsBackgroundTestingEnabledInUserPreferences())
					SKLogger.d(this, "+++++DEBUG+++++ Service disabled(manual), exiting.");
			}
		} catch (Throwable th) {
			//if an error happened we want to restart from State.NONE
			appSettings.saveState(State.NONE);
			SKLogger.d(this, "+++++DEBUG+++++ caught throwable, th=" + th.toString());
			SKLogger.d(this, "+++++DEBUG+++++ call OtherUtils.rescheduleWakeup");
			OtherUtils.rescheduleWakeup(this, appSettings.rescheduleTime);
			SKLogger.e(this, "failed in service ", th);
		} finally {
			ScheduledTestExecutionQueue.sbForceCanExecuteNow = false;
			onEnd();
		}
	}
	

	public static boolean isExecuting(){
		synchronized(sync){
			return isExecuting;
		}
	}
	
	public void onBegin() {
		SKLogger.d(this, "+++++DEBUG+++++ MainService onBegin (begin)");
		
		synchronized(sync){
			isExecuting = true;
		}

		// obtain wake lock, other way our service may stop executing
		PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
		wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
				MainService.class.getName());
		wakeLock.acquire();

		// reschedule service in the beginning to ensure it will be started if
		// killed.
		OtherUtils.rescheduleRTC(this, appSettings.rescheduleServiceTime);
		
		SKLogger.d(this, "+++++DEBUG+++++ MainService onBegin - create collector and call start()");
		collector = new TrafficStatsCollector(this);
		collector.start();
		SKLogger.d(this, "+++++DEBUG+++++ MainService onBegin (end)");
	}

	private void onEnd() {
		SKLogger.d(this, "+++++DEBUG+++++ MainService onEnd (begin)");
	
		if (wakeLock != null) {
			synchronized (this) {
				if (wakeLock.isHeld()) {
					// http://stackoverflow.com/questions/12140844/java-lang-runtimeexception-wakelock-under-locked-c2dm-lib
					wakeLock.release();
				}
			}
		}
		
		long bytes = collector.finish();
		if (bytes == 0) {
			// If data logging via Android TrafficStats failed to return anything, then
			// fall-back to using accumulatedTestBytes, which is an accumulation of data 
			// from all tests run, where the values used to accumulated are based on estimated values
			// in the schedule XML.
         	bytes = accumulatedTestBytes;
		}
		appSettings.appendUsedBytes(bytes);
		
		if(!appSettings.getIsBackgroundTestingEnabledInUserPreferences()){
			SKLogger.d(this, "+++++DEBUG+++++ MainService onEnd, service not enabled - cancelling alarm");
			OtherUtils.cancelAlarm(this);
		}
		synchronized(sync){
			publish(UIUpdate.completed());
			isExecuting = false;
		}
		continuousStopped();
		SKLogger.d(this, "+++++DEBUG+++++ MainService onEnd... (end)");
	}
	
	//methods to get and modify the execution status of continuous testing
	public boolean isExecutingContinuous(){
		return isExecutingContinuous;
	}
	
	
	
	//Start service
	public static void poke(Context ctx) {
		ctx.startService(new Intent(ctx, MainService.class));
	}
	
	public static void force_poke(Context ctx){
		Intent intent = new Intent(ctx, MainService.class);
		intent.putExtra(FORCE_EXECUTION_EXTRA,true);
		ctx.startService(intent);
	}

	// Debug time, use this to force a background test...
	public static void sForceBackgroundTest(Context ctx){
		Intent intent = new Intent(ctx, MainService.class);
		intent.putExtra(FORCE_EXECUTION_EXTRA,true);
		intent.putExtra(FORCE_EXECUTION_NOW_EXTRA,true);
		ctx.startService(intent);
	}	
	
	public enum  ContinuousState{ STOPPED, STARTING, EXECUTING, STOPPING};
	//start continuous testing
	public static void poke_continuous(Context ctx){
		SKLogger.d(MainService.class, "poke_continous");
		Intent intent = new Intent(ctx, MainService.class);
		intent.putExtra(EXECUTE_CONTINUOUS_EXTRA, true);
		ctx.startService(intent);
	}
	
	public static void registerContinuousHandler(Context ctx, Handler handler){
		synchronized(sync){
			mContinuousHandler = handler;
			poke_continuous(ctx);
			
		}
	}
	
	public static void unregisterContinuousHandler(){
		synchronized(sync){
			mContinuousHandler = null;
		}
	}
	
	public void continuousStarted(){
		synchronized(sync){
			if(mContinuousHandler !=null){
				Message msg = new Message();
				msg.obj = ContinuousState.EXECUTING;
				mContinuousHandler.sendMessage(msg);
			}
		}
	}
	
	private void continuousStopped(){
		synchronized(sync){
			if(mContinuousHandler !=null){
				Message msg = new Message();
				msg.obj = ContinuousState.STOPPED;
				mContinuousHandler.sendMessage(msg);
			}
		}
	}
	
	public static void stopContinuousExecution(){
		isExecutingContinuous = false;
	}
	
	
	//Register the handler to update the UI
	public static boolean registerActivationHandler(Context ctx, Handler handler){
		// The Main Service MUST be running for activation to work.
		// However, there is a delay from the request to start the activity, to 
		// actually registering the activity.
		
		synchronized(sync){
			mActivationHandler = handler;
			force_poke(ctx);
		}
		
		return true;
	}
	
	//Unregister current handler
	public static void unregisterActivationHandler(){
		synchronized(sync){
			mActivationHandler = null;
		}
	}
	
	//Send a JSONObject to the registered handler, if any
	public void publish(JSONObject jobj){
		synchronized(sync){
			if(mActivationHandler != null && jobj != null){
				Message msg = new Message();
				msg.obj = jobj;
				mActivationHandler.sendMessage(msg);
			}
		}
	}
	
}
