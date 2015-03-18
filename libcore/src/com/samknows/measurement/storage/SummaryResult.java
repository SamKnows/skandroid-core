package com.samknows.measurement.storage;

import com.samknows.measurement.storage.StorageTestResult.*;

/**
 * All rights reserved SamKnows
 * Written by pablo@samknows.com
 */


/**
 * This class fragment is responsible for:
 * * Store average result
 */

public class SummaryResult
{
	private DETAIL_TEST_ID testType;
	private float max, min, average;
	
	public SummaryResult()
	{
		
	}
	
	public SummaryResult(DETAIL_TEST_ID pTestType, float pAverage, float pMax, float pMin)
	{
		this.testType = pTestType;
		this.max = pMax;
		this.min = pMin;
		this.average = pAverage;
	}
	
	public DETAIL_TEST_ID getTestType()
	{
		return testType;
	}
	
	public void setTestType(DETAIL_TEST_ID testType)
	{
		this.testType = testType;
	}
	
	public float getMax()
	{
		return max;
	}
	
	public void setMax(float max)
	{
		this.max = max;
	}
	
	public float getMin()
	{
		return min;
	}
	
	public void setMin(float min)
	{
		this.min = min;
	}
	
	public float getAverage()
	{
		return average;
	}
	
	public void setAverage(float average)
	{
		this.average = average;
	}	
}
