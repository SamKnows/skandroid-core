package com.samknows.measurement.schedule.condition;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import android.util.Log;

import com.samknows.measurement.schedule.failaction.RetryFailAction;
import com.samknows.measurement.test.TestContext;

public class ConditionGroup extends Condition implements Serializable {
	private static final long serialVersionUID = 1L;

	public String id;
	public List<Condition> conditions = new ArrayList<Condition>();
	public RetryFailAction failAction;

	public static ConditionGroup parseXml(Element node) {
		ConditionGroup cg = new ConditionGroup();
		cg.id = node.getAttribute("id");

		NodeList conditions = node.getElementsByTagName("condition");
		for (int i = 0; i < conditions.getLength(); i++) {
			Element condition = (Element) conditions.item(i);
			cg.conditions.add(Condition.parseXml(condition));
		}

		NodeList list = node.getElementsByTagName("action");
		if (cg != null && list.getLength() > 0) {
			cg.failAction = RetryFailAction.parseXml((Element) list.item(0));
		}
		return cg;
	}

	@Override
	public String getConditionStringForReportingFailedCondition() {
		Log.e(this.getClass().toString(),
				"getConditionStringForReportingFailedCondition - unexpected call!");

		return "CONDITION_GROUP";
	}

	@Override
	public ConditionGroupResult doTestBefore(TestContext tc) {
		Executor executor = Executors.newCachedThreadPool();
		ConditionGroupResult result = new ConditionGroupResult();
		List<Future<ConditionResult>> futureResults = new ArrayList<Future<ConditionResult>>();

		// request for result from all conditions

		for (Condition c : conditions) {
			try {
				Future<ConditionResult> cr = c.testBefore(tc);

				// // ENABLE THIS IF YOU WANT TO FORCE CONDITION FAILURE AT
				// RUNTIME IN DEBUGGER (START)
				// cr.get().isSuccess = false;
				// // ENABLE THIS IF YOU WANT TO FORCE CONDITION FAILURE AT
				// RUNTIME IN DEBUGGER (END)
				futureResults.add(cr);
				if (!cr.isDone()) {
					executor.execute((FutureTask<?>) cr);
				} else if (!cr.get().isSuccess && !c.failQuiet) { // if we have
																	// result
																	// and it
																	// fails
																	// than skip
																	// all the
																	// rest
																	// conditions
					tc.resultsContainer.addFailedCondition(c);
				}
			} catch (Exception e) {
				e.printStackTrace();
				result.isSuccess = false;
				tc.resultsContainer.addFailedCondition(c);
			} finally {
			}
		}
		for (Future<ConditionResult> future : futureResults) {
			try {
				result.add(future.get());
				if (!future.get().isSuccess && !future.get().isFailQuiet()) {
					tc.resultsContainer.addFailedCondition(future.get()
							.getType());
				}
			} catch (Exception e) {
				e.printStackTrace();
				result.isSuccess = false;
				tc.resultsContainer
						.addFailedCondition(ConditionResult.JSON_CRASH);
			} finally {
			}
		}

		return result;
	}

	@Override
	public ConditionGroupResult testAfter(TestContext tc) {
		ConditionGroupResult result = new ConditionGroupResult();
		for (Condition c : conditions) {
			result.add(c.testAfter(tc));
		}
		return result;
	}

	@Override
	public void release(TestContext tc) {
		for (Condition c : conditions)
			c.release(tc);
	}

	@Override
	public boolean needSeparateThread() {
		return false;
	}
}
