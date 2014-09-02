package com.samknows.measurement.storage;

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
	private int testType;
	private float max, min, average;
	
	public SummaryResult()
	{
		
	}
	
	public SummaryResult(int pTestType, float pAverage, float pMax, float pMin)
	{
		this.testType = pTestType;
		this.max = pMax;
		this.min = pMin;
		this.average = pAverage;
	}
	
	public int getTestType()
	{
		return testType;
	}
	
	public void setTestType(int testType)
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
