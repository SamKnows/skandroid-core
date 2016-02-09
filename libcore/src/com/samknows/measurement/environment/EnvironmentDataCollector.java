package com.samknows.measurement.environment;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import com.samknows.measurement.SK2AppSettings;
import com.samknows.measurement.TestRunner.TestContext;

public class EnvironmentDataCollector extends BaseDataCollector{
	private static final long serialVersionUID = 1L;
	
	//List<String> result = new ArrayList<String>();
	private List<DCSData> data = new ArrayList<>();
		
	@Override
	public void start(TestContext ctx) {
		super.start(ctx);
		if(isEnabled){
			// This is synchronized, as data might mutate while we're enumerating!
			synchronized (this) {
				data.add(new PhoneIdentityDataCollector(ctx.getContext()).collect());
				// A consequence of the system "collecting" metrics both when we start *and* stop a test, is that 
				// this leads to multiple rows in the passive_metric table, with the same batch_id and metric...!
				data.add(new NetworkDataCollector(ctx.getContext()).collect());
				data.add(new CellTowersDataCollector(ctx.getContext()).collect());
			}
		}
	}
	
	@Override
	public void clearData(){
		// This is synchronized, as data might mutate while we're enumerating!
		synchronized (this) {
			data.clear();
		}
	}
	
	@Override
	public void stop(TestContext ctx){
 		// A consequence of the system "collecting" metrics both when we start *and* stop a test, is that 
 		// this leads to multiple rows in the passive_metric table, with the same batch_id and metric...!
		
		// This is synchronized, as data might mutate while we're enumerating!
		synchronized (this) {
			data.add(new NetworkDataCollector(ctx.getContext()).collect());
			data.add(new CellTowersDataCollector(ctx.getContext()).collect());

			//collect the traffic data if enabled in the properties file
			if(SK2AppSettings.getSK2AppSettingsInstance().collect_traffic_data){
				DCSData nud = new NetUsageCollector(ctx.getContext()).collect();
				if(nud != null){
					data.add(nud);
				}
			}
		}
	}

//	@Override
//	public List<String> getOutput() {
//		List<String> ret = new ArrayList<String>();
//
//    	// This is synchronized, as data might mutate while we're enumerating!
//		synchronized (this) {
//			for(DCSData d:data){
//				ret.addAll(d.convert());
//			}
//
////      DCSStringBuilder builder = new DCSStringBuilder();
////      builder.append(ID_SIM_OP);
////      builder.append(time/1000);
////      builder.append(simOperatorCode);
////      builder.append(simOperatorName);
////      list.add(builder.build());
//		}
//		return ret;
//	}

	@Override
	public List<JSONObject> getPassiveMetric() {
		List<JSONObject> ret = new ArrayList<>();
		
    	// This is synchronized, as data might mutate while we're enumerating!
		synchronized (this) {
			
			for(DCSData d:data){
				ret.addAll(d.getPassiveMetric());
			}
		}
		return ret;
	}
	
	@Override
	public List<JSONObject> getJSONOutput(){
		List<JSONObject> ret = new ArrayList<>();
    	// This is synchronized, as data might mutate while we're enumerating!
		synchronized (this) {
			for(DCSData d:data){
				ret.addAll(d.convertToJSON());
			}
		}
		return ret;
	}
}
