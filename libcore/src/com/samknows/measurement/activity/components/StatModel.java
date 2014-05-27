package com.samknows.measurement.activity.components;

import org.json.JSONException;
import org.json.JSONObject;

public class StatModel{
	
	private String mockaverage;
	private String mockdatagrid;
	private String mockdatagridpage2;
	private String mockgraph;
	private String mockgraph2;
	private String archivedata;
	
	public static final int UPLOAD_TEST = 0;
	public static final int DOWNLOAD_TEST = 1;
	public static final int LATENCY_TEST = 2;
	public static final int PACKETLOSS_TEST = 3;
	public static final int JITTER_TEST = 4;
	
	/*

graphdata = 
	{ "type":0,
	  "y_label":"ms", 
	   "end_date": "2011-04-10",
       "start_date": "2011-04-01",
       "results":[
       		{"dtime": "32892398",   "value": "37.44"},
       		{"dtime": "983283982",   "value": "37.44"}
       ]
       }
       	
 
griddata = {
"type":0,
"results": [
		 	{
			"archiveindex":"9",
			"dtime":393930,
			"location":"London",
			"result":36.55523,
			"success":1,
			"hrresult":"1 mbps 2.2%"
			},
			{	
			"archiveindex":"10",
			"dtime":393932,
			"location":"London",
			"result":32.55523,
			"success":1,
			"hrresult":"1 mbps 2.2%"
			}
		]
	}
	


"averagedata" = [
	{	"test":"0", "value":"19.2 Mbs"},
	{	"test":"1", "value":"190.2 ms"}
]


"archivedata" =
 {	"index":"9"
     "dtime":"1235455",
	"activemetrics":[
		{"test":"0",
		"location":"London",
		"hrresult":"12.0 Mbps",
		"success":"1"
		},
		{"test":"1",
		"location":"London",
		"hrresult":"12 ms",
		"success":"0"
		},.....
		
	],
	"passivemetrics":[
	{
		"metric":"gsmlac",
		"type":"boolean",
		"value":"1"
	},
	{
		"metric":"1",
		"type":"String",
		"value":"-13 dbm"
	},
	......
	
	]
}



archivedatasummary = {
	"counter":"50",
	"startdate":"2012-11-12",
	"enddate":"2013-11-12"
}



testname:
	1:uploadresult
	2:download_result
	3:packetloss_result
	4:latency_results
	5:jitterresults
	 */
	
	public StatModel(){
		mockJSON();
	}
	
	public JSONObject getGrid(int testnumber,int offset, int limit){
		JSONObject data = null;
		try {
			
			if (offset>4) {
				data = new JSONObject(mockdatagridpage2);
			}
			else
			{
				data = new JSONObject(mockdatagrid);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return data;
	}
	
	public JSONObject getGraph(int testnumber,long dtime_start, long dtime_end){
		JSONObject data = null;
		try {
			if (dtime_start-dtime_end<2*7*24*60*60*1000) {
				data = new JSONObject(mockgraph);
			}
			else
			{
				data = new JSONObject(mockgraph2);
			}
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return data;
	}
	
	public JSONObject getArchive(int index){
		JSONObject data = null;
		try {

				data = new JSONObject(archivedata);
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return data;
	}

	
	public JSONObject getAverage(int testnumber,long dtime_start,long dtime_end) {
		JSONObject data = null;
		try {
			data = new JSONObject(mockaverage);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return data;
	}
	
	
	private void mockJSON(){
		mockaverage="{test1:'19.2 Mbps',test2:'3.3 Mbps',test3:'57 Ms',test4:'99 %',test5:'302 Ms'}";	
		mockdatagrid="{results:[" +
				"{'archiveindex':'1',type':'0','location':'London','success':'1','result':'37.44','dtime':'04/02/11, 00:00'}," +
				"{'archiveindex':'2','type':'0','location':'London','success':'0','result':'Failed','dtime':'04/02/11, 08:00'}," +
				"{'archiveindex':'3','type':'0','location':'London','success':'1','result':'17.44','dtime':'10/02/11, 00:00'}," +
				"{'archiveindex':'4','type':'0','location':'London','success':'1','result':'27.44','dtime':'04/02/11, 00:00'}," +
				"{'archiveindex':'5','type':'0','location':'London','success':'1','result':'17.44','dtime':'04/02/11, 08:02'}" +
				"]}";
		mockdatagridpage2="{results:[" +
				"{'archiveindex':'6','type':'0','location':'London P2','success':'1','result':'7.44','dtime':'04/09/11, 00:00'}," +
				"{'archiveindex':'7','type':'0','location':'London','success':'0','result':'Failed','dtime':'04/09/11, 08:00'}," +
				"{'archiveindex':'8',type':'0','location':'London','success':'1','result':'12.44','dtime':'04/1011, 08:02'}" +
				"]}";
		
		mockgraph="{ 'type':0," +
				"'y_label':'ms'," +
				"'end_date': '2011-04-10'," +
				"'start_date': '2011-04-01'," +
				"'results':[" +
				"	{'datetime': '2011-04-02 00:00:00',   'value': '37.44'}," +
				"	{'datetime': '2011-04-05 08:00:00',   'value': '17.44'}," +
				"	{'datetime': '2011-04-07 08:00:00',   'value': '27.44'}," +
				"	{'datetime': '2011-04-08 08:00:00',   'value': '17.44'}," +
				"	{'datetime': '2011-04-09 08:00:00',   'value': '7.44'}," +
				"	{'datetime': '2011-04-10 08:00:00',   'value': '12.44'}" +
				"]" +
				"}";
		
		mockgraph2="{ 'type':0," +
				"'y_label':'ms'," +
				"'end_date': '2011-04-10'," +
				"'start_date': '2011-04-01'," +
				"'results':[" +
				"	{'datetime': '2011-04-02 00:00:00',   'value': '7.44'}," +
				"	{'datetime': '2011-04-08 08:00:00',   'value': '97.44'}" +
				"]" +
				"}";
		
		
		archivedata="{'index':'9'," +
				"'dtime':'1235455'," +
	"'datetime':'2012-11-11 8:00'," +
	"'activemetrics':[" +
	"	{'test':'0'," +
	"	'location':'London'," +
	"	'hrresult':'37.44 Mbps'," +
	"	'success':'1'" +
	"	}," +
	"	{'test':'1'," +
	"	'location':'London'," +
	"	'hrresult':'37.44 Mbps'," +
	"	'success':'1'" +
	"	}," +
	"	{'test':'2'," +
	"	'location':'London'," +
	"	'hrresult':'37.44 Mbps'," +
	"	'success':'1'" +
	"	}," +
	"	{'test':'3'," +
	"	'location':'London'," +
	"	'hrresult':'37.44 Mbps'," +
	"	'success':'1'" +
	"	}," +
	"	{'test':'4'," +
	"	'location':'London'," +
	"	'hrresult':'12 ms'," +
	"	'success':'0'" +
	"	}" +		
	"]," +
	"'passivemetric':[" +
	"{" +
	"	'metric':'0'," +
	"	'type':'boolean'," +
	"	'value':'1'" +
	"}," +
	"{" +
	"	'metric':'1'," +
	"	'type':'boolean'," +
	"	'value':'1'" +
	"}," +
	"{" +
	"	'metric':'2'," +
	"	'type':'String'," +
	"	'value':'Here is a bunch of numbers 32322'" +
	"}," +
	"{" +
	"	'metric':'3'," +
	"	'type':'String'," +
	"	'value':'Here is a bunch of numbers 32322'" +
	"}," +
	"{" +
	"	'metric':'4'," +
	"	'type':'String'," +
	"	'value':'Here is a bunch of numbers 32322'" +
	"}," +
	"{" +
	"	'metric':'5'," +
	"	'type':'String'," +
	"	'value':'Here is a bunch of numbers 32322'" +
	"}," +
	"{" +
	"	'metric':'6'," +
	"	'type':'String'," +
	"	'value':'Here is a bunch of numbers 32322'" +
	"}," +
	"{" +
	"	'metric':'7'," +
	"	'type':'String'," +
	"	'value':'Here is a bunch of numbers 32322'" +
	"}," +
	"{" +
	"	'metric':'8'," +
	"	'type':'String'," +
	"	'value':'Here is a bunch of numbers 32322'" +
	"}," +
	"{" +
	"	'metric':'9'," +
	"	'type':'String'," +
	"	'value':'Here is a bunch of numbers 32322'" +
	"}," +
	"{" +
	"	'metric':'10'," +
	"	'type':'String'," +
	"	'value':'Here is a bunch of numbers 32322'" +
	"}," +
	"{" +
	"	'metric':'11'," +
	"	'type':'String'," +
	"	'value':'-13 dbm a really long string'" +
	"}" +
	"]" +
"}";
		
		
		
		
		
		
	}
}
