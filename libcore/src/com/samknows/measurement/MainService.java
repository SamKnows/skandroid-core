package com.samknows.measurement;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import com.samknows.libcore.SKLogger;
import com.samknows.libcore.SKConstants;
import com.samknows.measurement.TestRunner.SKTestRunner;
import com.samknows.measurement.environment.TrafficStatsCollector;
import com.samknows.measurement.schedule.ScheduleConfig;
import com.samknows.measurement.TestRunner.BackgroundTestRunner;
import com.samknows.measurement.statemachine.state.StateEnum;
import com.samknows.measurement.util.OtherUtils;

public class MainService extends IntentService {
  static final String TAG = "MainService";

	public static final String FORCE_EXECUTION_EXTRA	= "force_execution";		
	public static final String EXECUTE_CONTINUOUS_EXTRA	= "execute_continuous";
  private PowerManager.WakeLock wakeLock;
	private TrafficStatsCollector collector;
	private SK2AppSettings appSettings;
	private static boolean isExecuting;
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
		Log.d(TAG, "+++++DEBUG+++++ MainService onBind");
		return mMainServiceBinder;
		
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "+++++DEBUG+++++ MainService onCreate");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "+++++DEBUG+++++ MainService onDestroy");
	}

	long accumulatedTestBytes = 0L;
	
	@Override
	protected void onHandleIntent(Intent intent) {
		
		accumulatedTestBytes = 0L;
		
		Log.d(TAG, "+++++DEBUG+++++ MainService onHandleIntent" + intent.toString());
		
		boolean force_execution = intent.getBooleanExtra(FORCE_EXECUTION_EXTRA, false);

		try {
			appSettings = SK2AppSettings.getSK2AppSettingsInstance();
			
			ScheduleConfig config = CachingStorage.getInstance().loadScheduleConfig();
			
			// This looks first at user preferences - if the user has set a true/false value, that value is returned.
			// otherwise, we return the "background_test" value last delivered from the config/schedule.
			boolean backgroundTest = SKApplication.getAppInstance().getIsBackgroundTestingEnabledInUserPreferences();
			
			onBegin();
			/* 
			 * The tests have to be executed when background test is set in the config file and the service
			 * is enabled in the app settings.
			 * Moreover the tests have executed whenever the app has forced the activation (force_execution = true)
			 * In case the device is in roaming the, the test manager shouldn't run any test
			 * 
			 * if the intent contains the EXECUTE_CONTINUOUS_EXTRA  run the continuous testing procedure and ignore the rest.
			 * 
			 */
			if((backgroundTest && SKApplication.getAppInstance().getIsBackgroundTestingEnabledInUserPreferences()) || force_execution ) {
				if (appSettings.run_in_roaming || !OtherUtils.isRoaming(this)) {
          SKTestRunner.SKTestRunnerObserver observerNull = null;
          BackgroundTestRunner backgroundTestRunner = new BackgroundTestRunner(observerNull);
         	accumulatedTestBytes = backgroundTestRunner.startTestRunning_RunToEndBlocking_ReturnNumberOfTestBytes();
				} else {
					Log.d(TAG, "+++++DEBUG+++++ Service disabled(roaming), exiting.");
					OtherUtils.reschedule(this,	SKConstants.SERVICE_RESCHEDULE_IF_ROAMING_OR_DATACAP);
				}
			} else {
				if(!backgroundTest)
					Log.d(TAG, "+++++DEBUG+++++ Service disabled(config file), exiting.");
				if (!SKApplication.getAppInstance().getIsBackgroundTestingEnabledInUserPreferences())
					Log.d(TAG, "+++++DEBUG+++++ Service disabled(manual), exiting.");
			}
		} catch (Throwable th) {
			//if an error happened we want to restart from StateEnum.NONE
      SKLogger.sAssert(false);
			appSettings.saveState(StateEnum.NONE);
			Log.d(TAG, "+++++DEBUG+++++ caught throwable, th=" + th.toString());
			Log.d(TAG, "+++++DEBUG+++++ call OtherUtils.rescheduleWakeup");
			OtherUtils.rescheduleWakeup(this, appSettings.rescheduleTime);
			SKLogger.e(this, "failed in service ", th);
		} finally {
			onEnd();
		}
	}
	

	public static boolean isExecuting(){
		synchronized(sync){
			return isExecuting;
		}
	}
	
	public void onBegin() {
		Log.d(TAG, "+++++DEBUG+++++ MainService onBegin (begin)");
		
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
		
		Log.d(TAG, "+++++DEBUG+++++ MainService onBegin - create collector and call start()");
		collector = new TrafficStatsCollector(this);
		collector.start();
		Log.d(TAG, "+++++DEBUG+++++ MainService onBegin (end)");
	}

	private void onEnd() {
		Log.d(TAG, "+++++DEBUG+++++ MainService onEnd (begin)");
	
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
		
		if(!SKApplication.getAppInstance().getIsBackgroundTestingEnabledInUserPreferences()){
			Log.d(TAG, "+++++DEBUG+++++ MainService onEnd, service not enabled - cancelling alarm");
			OtherUtils.cancelAlarm(this);
		}
		synchronized(sync){
			isExecuting = false;
		}
		Log.d(TAG, "+++++DEBUG+++++ MainService onEnd... (end)");
	}

	//Start service
	public static void poke(Context ctx) {
		ctx.startService(new Intent(ctx, MainService.class));
	}

}
