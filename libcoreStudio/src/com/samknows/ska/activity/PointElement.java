package com.samknows.ska.activity;


//This class represents each of the points in the graph


public class PointElement
{
	private boolean active;
	private int numberOfElements;
	private float sum;
	
	public boolean isActive()
	{
		return active;
	}
	
	public void setActive(boolean active)
	{
		this.active = active;
	}
	
	public int getNumberOfElements()
	{
		return numberOfElements;
	}
	
	public void setNumberOfElements(int numberOfElements)
	{
		this.numberOfElements = numberOfElements;
	}
	
	public float getSum()
	{
		return sum;
	}
	
	public void setSum(float sum)
	{
		this.sum = sum;
	}	
}
