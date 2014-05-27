package com.samknows.measurement.schedule.condition;

import org.w3c.dom.Element;

import com.samknows.measurement.test.TestContext;
import com.samknows.measurement.util.XmlUtils;

public class ParamExpiredCondition extends Condition{
	private static final long serialVersionUID = 1L;
	private String paramName;
	private long expireTime;
//	public boolean failQuiet = true;

	@Override
	public ConditionResult doTestBefore(TestContext tc) {
		boolean expired = tc.paramsManager.isExpired(paramName, expireTime);
		ConditionResult res = new ConditionResult(expired);
		res.generateOut("PARAM_EXPIRED", paramName);
		res.setFailQuiet(true);
		return res;
	}

	@Override
	protected boolean needSeparateThread() {
		return false;
	}

	public static ParamExpiredCondition parseXml(Element node) {
		ParamExpiredCondition c = new ParamExpiredCondition();
		c.paramName = node.getAttribute("paramName");
		c.expireTime = XmlUtils.convertTime(node.getAttribute("expireTime"));
//		String fq = node.getAttribute("fail-quiet");
//		if (fq != null && !fq.isEmpty()) {
//			c.failQuiet = Boolean.valueOf(fq);
//		}
		return c;
	}
	
	
	@Override
	public String getConditionStringForReportingFailedCondition() {
		return "PARAM_EXPIRED";
	}	
}
