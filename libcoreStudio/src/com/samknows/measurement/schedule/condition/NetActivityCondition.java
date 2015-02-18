package com.samknows.measurement.schedule.condition;

import org.w3c.dom.Element;

import com.samknows.measurement.environment.TrafficData;
import com.samknows.measurement.environment.TrafficStatsCollector;
import com.samknows.measurement.test.TestContext;
import com.samknows.measurement.util.XmlUtils;

public class NetActivityCondition extends Condition{
	public static final String TYPE_VALUE= "NETACTIVITY";
	public static final String JSON_MAXBYTESIN = "maxbytesin";
	public static final String JSON_MAXBYTESOUT = "maxbytesout";
	public static final String JSON_BYTESIN = "bytesin";
	public static final String JSON_BYTESOUT = "bytesout";
	
	private static final long serialVersionUID = 1L;

	private int maxByteIn;
	private int maxByteOut;
	private long time;
	
	public static NetActivityCondition parseXml(Element node) {
		NetActivityCondition c = new NetActivityCondition();
		c.maxByteIn = Integer.valueOf(node.getAttribute("maxByteIn"));
		c.maxByteOut = Integer.valueOf(node.getAttribute("maxByteOut"));
		String time = node.getAttribute("time");
		c.time = XmlUtils.convertTime(time);
		return c;
	}

	@Override
	public boolean needSeparateThread() {
		return true;
	}

	@Override
	public ConditionResult doTestBefore(TestContext tc) {
		TrafficData data = TrafficStatsCollector.collectAll(time);
		
		ConditionResult result = new ConditionResult(data.checkCondition(maxByteIn, maxByteOut), failQuiet);
		result.setJSONFields(JSON_MAXBYTESIN, JSON_MAXBYTESOUT, JSON_BYTESIN, JSON_BYTESOUT);
		result.generateOut(TYPE_VALUE, maxByteIn, maxByteOut, data.totalRxBytes, data.totalTxBytes);
		return result;
	}

	@Override
	public String getConditionStringForReportingFailedCondition() {
		return TYPE_VALUE;
	}
}
