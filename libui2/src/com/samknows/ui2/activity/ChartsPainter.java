package com.samknows.ui2.activity;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

import com.samknows.ska.activity.PointElement;
import com.samknows.libui2.R;

/**
 * This class is responsible for drawing the charts.
 * 
 * All rights reserved SamKnows
 * @author pablo@samknows.com
 */


public class ChartsPainter extends View
{	
	// *** CONSTANTS *** //
	public int C_X_INSET_LEFT;							// (80) 27 Left margin of the graph in relation with the layout which hosts it
	public int C_X_INSET_RIGHT;							// (50) 17 Right margin of the graph in relation with the layout which hosts it
    private int C_Y_INSET_BOTTOM;						// (60) 20 Bottom margin of the graph in relation with the layout which hosts it
    private int C_Y_INSET_TOP;							// (100) 33 Top margin of the graph in relation with the layout which hosts it

    private int C_X_SCALEMARKER_SIZE;					// (10) 3 Size of the markers of the X axis
    private int C_Y_SCALEMARKER_SIZE;					// (10) 3 Size of the markers of the Y axis
    
    private int C_X_AXIS_LABEL_MARGIN;					// (30) 10 X axis label margin from the marker
    private int C_Y_AXIS_LABEL_MARGIN;					// (30) 12 Y axis label margin from the marker
    
    private final String C_DOWNLOAD_UNITS = "Mb/s";		// Download measurements units
    private final String C_UPLOAD_UNITS = "Mb/s";		// Upload measurements units
    private final String C_LATENCY_UNITS = "ms";		// Latency measurements units
    private final String C_PACKET_LOSS_UNITS = "%";		// Packet loss measurements units
    private final String C_JITTER_UNITS = "ms";			// Packet loss measurements units

    // *** VARIABLES *** //    
    private float yAxisMinValue = 0;					// Max value of the Y axis. This help us to write the correct scale
    public float yAxisMaxValue = 0;						// Max value of the X axis. This help us to write the correct scale
    private String timePeriodString;					// Period time in string. This help us to write the title of the chart

    private String chartTitle = "";						// Title of the chart
    private String axisYTitle = "";						// Time axis title
    private String axisXTitle = "";						// Units axis title
    
    public ArrayList<PointElement> pointElementsList = new ArrayList<PointElement>();	// List of the points of the graph
    private ArrayList<String> arrLabelsX = new ArrayList<String>();						// Contains the values of the X axis   
    private ArrayList<String> arrLabelsY = new ArrayList<String>();						// Contains the values of the Y axis

    // Drawing objects
    private Paint paint_Axis, paint_Text, paint_Line_Border, paint_Line;		// Painters for the axis, texts and lines and other chart elements
    private Path path_lines;													// Path to draw the vertical lines on each x axis label

    // Fonts
    private Typeface typeface_Roboto_Thin;										// Typography of the chart text


    // *** CONSTRUCTORS *** //
    // Class main constructor
    public ChartsPainter(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        
        // Set up resources
        setUpResources(context);
    }    
    
    // *** CUSTOM METHODS *** //
    /**
     * Create, bind and set up the resources
     */
    private void setUpResources(final Context pContext)
    {
    	
    	C_X_INSET_LEFT = (int) convertDpToPixel(27, pContext);					
    	C_X_INSET_RIGHT = (int) convertDpToPixel(17, pContext);				
        C_Y_INSET_BOTTOM = (int) convertDpToPixel(20, pContext);				
        C_Y_INSET_TOP = (int) convertDpToPixel(33, pContext);			

        C_X_SCALEMARKER_SIZE = (int) convertDpToPixel(3, pContext);			
        C_Y_SCALEMARKER_SIZE = (int) convertDpToPixel(3, pContext);			
        
        C_X_AXIS_LABEL_MARGIN = (int) convertDpToPixel(10, pContext);			
        C_Y_AXIS_LABEL_MARGIN = (int) convertDpToPixel(12, pContext);
        
    	// Set up fonts
    	typeface_Roboto_Thin = Typeface.createFromAsset(getContext().getAssets(), "fonts/roboto_thin.ttf");

    	// Paint to draw the axis
        paint_Axis = new Paint()
        {{
        	// Set up the paint
            setStrokeWidth(convertDpToPixel(1, pContext));		// (3) 1 Paint stroke width
            setStyle(Paint.Style.STROKE);						// Paint style
            setColor(getResources().getColor(R.color.white));	// Paint colour
        }};

        // Paint to draw the texts
        paint_Text = new Paint()
        {{
            setColor(getResources().getColor(R.color.white));	// Paint colour
            setTypeface(typeface_Roboto_Thin);					// Paint type face
            setTextAlign(Align.CENTER);							// Text align
        }};

        // Paint to draw the main chart line
    	paint_Line_Border = new Paint()
        {{
                setAntiAlias(true);											// Set anti alias to true
                setStrokeWidth(convertDpToPixel(1, pContext));				// Set the stroke width
                setStrokeCap(Cap.ROUND);									// Set the paint's line cap style
                setStyle(Paint.Style.STROKE);								// Paint style
                setColor(getResources().getColor(R.color.cornflower));		// Set the paint colour
        }};

        // Paint to draw the closed path with the gradient inside
        paint_Line = new Paint()
        {{
                
                setAntiAlias(true);								// Set anti alias to true
                setStrokeWidth(convertDpToPixel(1, pContext));	// Set the stroke width
                setStyle(Style.FILL_AND_STROKE);				// Set the painter style
                setColor(Color.parseColor("#555555"));			// Set the paint colour
        }};
    }
    
    /**
     * Get the time period in String format
     * 
     * @return timePeriodString, the period time
     */
    private String getDataPeriodType()
    {
    	return timePeriodString;
    }
    
    /**
     * Set the time period for chart. This is useful for showing the chart title
     * 
     * @param pTimePeriod
     */
    public void setDataPeriodType(int pTimePeriod)
    {
    	switch (pTimePeriod)
    	{
    		// 1 Day
	    	case 0:
				timePeriodString = "1 day";			
				break;
			// 1 Week
	    	case 1:
				timePeriodString = "1 week";			
				break;
			// 1 Month
	    	case 2:
				timePeriodString = "1 month";			
				break;
			// 3 Months
	    	case 3:
				timePeriodString = "3 months";			
				break;
			// 1 Year
			case 4:
				timePeriodString = "1 year";			
				break;
			// Default case: 1 week
			default:
				timePeriodString = "1 week";
				break;
		}    	
    }

    /**
     * Set the type of chart. This will set the chart title and the axis Y units
     * 
     * @param pChartType
     */
    public void setChartTitle(int pChartType)
    {
    	switch (pChartType)
    	{
    		// Download
			case 0:
				this.chartTitle = "Download chart for " + getDataPeriodType() + " (" + C_DOWNLOAD_UNITS + ")";
				break;
			// Upload
			case 1:
				this.chartTitle = "Upload chart for " + getDataPeriodType() + " (" + C_UPLOAD_UNITS + ")";
				break;
			// Latency
			case 2:
				this.chartTitle = "Latency chart for " + getDataPeriodType() + " (" + C_LATENCY_UNITS + ")";
				break;
				
			// Packet Loss
			case 3:
				this.chartTitle = "Packet loss chart for " + getDataPeriodType() + " (" + C_PACKET_LOSS_UNITS + ")";
				break;
				
			// Packet Jitter
			case 4:
				this.chartTitle = "Jitter chart for " + getDataPeriodType() + " (" + C_JITTER_UNITS + ")";
				break;
	
			// Default case will be download
			default:
				break;
		}
    	
    	invalidate();
    }
    
    /**
     * Create and initialise the array of values.
     * 
     * @param pChartLayoutWidthInPixels
     */
	public void createAndInitialiseArrayOfValues(int pChartLayoutWidthInPixels)
    {		
    	pointElementsList = new ArrayList<PointElement>(pChartLayoutWidthInPixels);		// Initialise the list of points
    	
    	for (int i = 0; i < pChartLayoutWidthInPixels; i++)
    	{
    		pointElementsList.add(i, new PointElement());		// Insert a point element
    		pointElementsList.get(i).setActive(false);			// Set it like inactive
    		pointElementsList.get(i).setSum(0);					// Set sum to 0
    		pointElementsList.get(i).setNumberOfElements(0);	// Set number of elements to 0
		}    	
    }
    
	/**
	 * Set up the Y axis
	 */
    public void setUpYAxis()
    {
    	// Reset the array
    	this.arrLabelsY.clear();
    	
    	this.yAxisMaxValue = (float) Math.ceil(yAxisMaxValue);
    	
    	float axisYNumberOfLabels;		// Number of labels of the Y axis
    	
    	// If Y axis max value is 0 (often in packet loss), the Y axis values will be between 0 and 1
    	if (this.yAxisMaxValue == 0)
    	{
    		axisYNumberOfLabels = 2;
    		this.yAxisMaxValue = 1;    		
		}
    	// If max value is greater than 0 and lower or equal to 4
    	else if (this.yAxisMaxValue <=4)
    	{
    		axisYNumberOfLabels = this.yAxisMaxValue * 4;			
		}
    	// If max value is between 4 and 20
    	else if (this.yAxisMaxValue < 20)
    	{
    		axisYNumberOfLabels = this.yAxisMaxValue;    		
    	}
    	// If max value is between 20 and 100
    	else if (this.yAxisMaxValue <= 100)
    	{
    		this.yAxisMaxValue = (float) (10 * Math.ceil(this.yAxisMaxValue / 10));
    		axisYNumberOfLabels = this.yAxisMaxValue / 10;    		
    	}
    	// If max value is between 100 and 1000
    	else if (this.yAxisMaxValue <= 1000)
    	{
    		this.yAxisMaxValue = (float) (50 * Math.ceil(this.yAxisMaxValue / 50));
    		axisYNumberOfLabels = this.yAxisMaxValue / 50;
    	}
    	// Any other case
    	else
    	{
    		axisYNumberOfLabels = 10;    		
    	}
    	
    	// Fill the array of Y axis labels
    	for (float chartYLabel = 0; chartYLabel <= axisYNumberOfLabels; chartYLabel++)
    	{
    		float currentLabel = this.yAxisMaxValue * chartYLabel / axisYNumberOfLabels;
    		DecimalFormat decimalFormat;
    		
    		if (currentLabel == 0)
    		{
    			decimalFormat = new DecimalFormat("#");    			    							
			}
    		else if (currentLabel < 10)
    		{
    			decimalFormat = new DecimalFormat("#.#");				
			}
    		else if (currentLabel < 100)
    		{
    			decimalFormat = new DecimalFormat("##");				
			}
    		else
    		{
    			decimalFormat = new DecimalFormat("###");				
			}
    		
    		this.arrLabelsY.add(String.valueOf(decimalFormat.format(this.yAxisMaxValue * chartYLabel / axisYNumberOfLabels)));			
		}
    }
    
    /**
     * Set up the X axis
     * 
     * @param pDataPeriod
     */
    public void setUpXAxis(int pDataPeriod)
    {
    	float axisXNumberOfLabels;		// Number of labels of the X axis
    	this.arrLabelsX.clear();		// Clear the x labels array
    	
    	long currentTimeInSeconds = System.currentTimeMillis()/1000;	// Current time in seconds
    	float stepBetweenLabelsInSeconds;	// Number of seconds between the x axis labels
    	float currentLabel;					// Number of seconds of the current label
    	
    	SimpleDateFormat df;				// Date formatter
    	
    	switch (pDataPeriod)
    	{
    		// 1 Day
    		case 0:    			
    			axisXNumberOfLabels = 12;											// Number of labels (divisions) for 1 day
    			stepBetweenLabelsInSeconds = 3600 * 24 / axisXNumberOfLabels;		// Distance between labels in seconds
    			df = new SimpleDateFormat("HH:mm");    								// Define the format for the label	
    			
    			// Iteration adding the labels to the X axis
    			for (int i = 0; i <= axisXNumberOfLabels; i++)
    	    	{    				
    				currentLabel = currentTimeInSeconds - 3600 * 24 + ((i) * stepBetweenLabelsInSeconds);		// Calculate the time in seconds for label in this iteration
    	    		this.arrLabelsX.add(df.format(new Date((long) (currentLabel * 1000))));						// Add the label to the array with the right format
    	        }
    			break;
    		// 1 Week
    		case 1:    			
    			axisXNumberOfLabels = 7;												// Number of labels (divisions) for 1 week
    			stepBetweenLabelsInSeconds = 3600 * 24 * 7 / axisXNumberOfLabels;		// Distance between labels in seconds
    			df = new SimpleDateFormat("MM-dd");										// Define the format for the label    			
    			
    			// Iteration adding the labels to the X axis
    			for (int i = 0; i <= axisXNumberOfLabels; i++)
    	    	{    				
    				currentLabel = currentTimeInSeconds - 3600 * 24 * 7 + ((i) * stepBetweenLabelsInSeconds);	// Calculate the time in seconds for label in this iteration
    	    		this.arrLabelsX.add(df.format(new Date((long) (currentLabel * 1000))));						// Add the label to the array with the right format
    	        }
    			break;
			// 1 Month
    		case 2:
    			axisXNumberOfLabels = 6;											// Number of labels (divisions) for 1 month   			
    			stepBetweenLabelsInSeconds = 3600 * 24 * 31 / axisXNumberOfLabels;	// Distance between labels in seconds
    			df = new SimpleDateFormat("MM-dd");									// Define the format for the label    			
    			
    			// Iteration adding the labels to the X axis
    			for (int i = 0; i <= axisXNumberOfLabels; i++)
    	    	{    				
    				currentLabel = currentTimeInSeconds - 3600 * 24 * 31 + ((i) * stepBetweenLabelsInSeconds);	// Calculate the time in seconds for label in this iteration    				
    	    		this.arrLabelsX.add(df.format(new Date((long) (currentLabel * 1000))));						// Add the label to the array with the right format
    	        }
    			break;
    		// 3 Months
    		case 3:
    			axisXNumberOfLabels = 6;												// Number of labels (divisions) for 3 months
    			stepBetweenLabelsInSeconds = 3600 * 24 * 31 * 3 / axisXNumberOfLabels;	// Distance between labels in seconds
    			df = new SimpleDateFormat("MM-dd");										// Define the format for the label    			
    			
    			// Iteration adding the labels to the X axis
    			for (int i = 0; i <= axisXNumberOfLabels; i++)
    	    	{    				
    				currentLabel = currentTimeInSeconds - 3600 * 24 * 31 * 3+ ((i) * stepBetweenLabelsInSeconds);	// Calculate the time in seconds for label in this iteration
    	    		this.arrLabelsX.add(df.format(new Date((long) (currentLabel * 1000))));							// Add the label to the array with the right format
    	        }
    			break;
    		// 1 Year
    		case 4:
    			axisXNumberOfLabels = 12;											// Number of labels (divisions) for 1 year   			
    			stepBetweenLabelsInSeconds = 3600 * 24 * 365 / axisXNumberOfLabels;	// Distance between labels in seconds
    			df = new SimpleDateFormat("yy-MM");									// Define the format for the label    			
    			
    			// Iteration adding the labels to the X axis
    			for (int i = 0; i <= axisXNumberOfLabels; i++)
    	    	{    				
    				currentLabel = currentTimeInSeconds - 3600 * 24 * 364 + ((i) * stepBetweenLabelsInSeconds);	// Calculate the time in seconds for label in this iteration		
    	    		this.arrLabelsX.add(df.format(new Date((long) (currentLabel*1000))));						// Add the label to the array with the right format
    	        }
    			break;

    		// The default case is 1 week
			default:
				axisXNumberOfLabels = 7;												// Number of labels (divisions) for 1 week
    			stepBetweenLabelsInSeconds = 3600 * 24 * 7 / axisXNumberOfLabels;		// Distance between labels in seconds
    			df = new SimpleDateFormat("MM-dd");										// Define the format for the label    			
    			
    			// Iteration adding the labels to the X axis
    			for (int i = 0; i <= axisXNumberOfLabels; i++)
    	    	{    				
    				currentLabel = currentTimeInSeconds - 3600 * 24 * 7 + ((i) * stepBetweenLabelsInSeconds);	// Calculate the time in seconds for label in this iteration    						
    	    		this.arrLabelsX.add(df.format(new Date((long) (currentLabel*1000))));						// Add the label to the array with the right format															
    	        }
				break;
		}
    }
    
    /**
     * Draw the X axis and the labels
     * 
     * @param canvas
     */
    private void drawXAxis(Canvas canvas)
    {
        int numberOfXAxisLabels = this.arrLabelsX.size();		// Number of points in the X axis labels array
        
        if (numberOfXAxisLabels == 0)
    	{
    		return;
    	}
        
        // Distance in pixels between X axis labels. Canvas width minus insets divided by number of divisions in the axis
        float pixelsBetweenXAxisLabels = (canvas.getWidth() - C_X_INSET_LEFT - C_X_INSET_RIGHT) / (numberOfXAxisLabels - 1);
        
        path_lines = new Path();			// Path to draw the vertical lines on each x axis label
        
        // Draw the X axis
        canvas.drawLine(C_X_INSET_LEFT, canvas.getHeight() - C_Y_INSET_BOTTOM, canvas.getWidth() - C_X_INSET_RIGHT, canvas.getHeight() - C_Y_INSET_BOTTOM, paint_Axis);   
        
        // Draw the X axis labels
        for (int i = 0; i < numberOfXAxisLabels; i++)
        {
        	// Draw the X axis markers
        	canvas.drawLine(C_X_INSET_LEFT + (i * pixelsBetweenXAxisLabels), canvas.getHeight() - C_Y_INSET_BOTTOM, C_X_INSET_LEFT + i * (pixelsBetweenXAxisLabels), canvas.getHeight() - C_Y_INSET_BOTTOM + C_X_SCALEMARKER_SIZE, paint_Axis);
        	        	
        	paint_Text.setTextSize(getResources().getDimensionPixelSize(R.dimen.text_size_micro));		// Modify the painter text size to draw the X axis labels
        	// Draw the X axis labels
            canvas.drawText(arrLabelsX.get(i), C_X_INSET_LEFT + i * (pixelsBetweenXAxisLabels), canvas.getHeight() - C_Y_INSET_BOTTOM + C_X_SCALEMARKER_SIZE + C_X_AXIS_LABEL_MARGIN, paint_Text);
            
            // Specify the vertical lines path on each X axis label
            path_lines.moveTo(C_X_INSET_LEFT + (i + 1) * (pixelsBetweenXAxisLabels), canvas.getHeight() - C_Y_INSET_BOTTOM + C_X_SCALEMARKER_SIZE);            
            path_lines.lineTo(C_X_INSET_LEFT + (i + 1) * (pixelsBetweenXAxisLabels), 0);
        }
    }
    
    /**
     * Draw the Y axis and the labels
     * 
     * @param canvas
     */
    private void drawYAxis(Canvas canvas)
    {
        int numberOfYAxisLabels = this.arrLabelsY.size();		// Number of points in the Y axis labels array
        
        if (numberOfYAxisLabels == 0)
        {
        	return;
        }
        
        // Distance in pixels between Y axis labels. Canvas height minus insets divided by number of divisions in the axis
        float pixelsBetweenYAxisLabels = (canvas.getHeight() - C_Y_INSET_TOP - C_Y_INSET_BOTTOM) / (numberOfYAxisLabels - 1);

        // Draw the Y axis
        canvas.drawLine(C_X_INSET_LEFT, C_Y_INSET_TOP, C_X_INSET_LEFT, canvas.getHeight() - C_Y_INSET_BOTTOM, paint_Axis);

        // Draw the Y axis labels
        for (int i = 0; i < numberOfYAxisLabels; i++)
        {
        	// Draw the Y axis markers
        	canvas.drawLine(C_X_INSET_LEFT, canvas.getHeight() - C_Y_INSET_BOTTOM - i * (pixelsBetweenYAxisLabels), C_X_INSET_LEFT - C_Y_SCALEMARKER_SIZE, canvas.getHeight() - C_Y_INSET_BOTTOM - i * (pixelsBetweenYAxisLabels), paint_Axis);
        	
        	paint_Text.setTextSize(getResources().getDimensionPixelSize(R.dimen.text_size_micro));		// Modify the painter text size to draw the Y axis labels
        	// Draw the Y axis labels
        	canvas.drawText(arrLabelsY.get(i), C_X_INSET_LEFT - C_Y_AXIS_LABEL_MARGIN, canvas.getHeight() - C_Y_INSET_BOTTOM - i * (pixelsBetweenYAxisLabels), paint_Text);
        }
    }

    /**
     * Draw the chart
     * 
     * @param canvas
     */
    private void drawChart(Canvas canvas)
    { 
    	Path closedPath = new Path();		// The closed path that enclosure the shape
    	Path superiorPath = new Path();   	// The path that remarks the main chart line

    	PointElement dataValue;				// Each of the points in the array of values 
    	Point point = new Point(0,0);		// Each of the points in the chart
    	Point firstPoint = new Point(0,0);  // First point in the chart
    	
    	boolean started = false;			// Determines if we have started drawing the chart or we must draw the first point.

    	float xAxisLength = canvas.getWidth() - C_X_INSET_LEFT - C_X_INSET_RIGHT;		// Length of the X axis
    	float yAxisLength = canvas.getHeight() - C_Y_INSET_TOP - C_Y_INSET_BOTTOM;		// Length of the Y axis
    	
        // Draw the chart itself
    	for (int i = 0; i < pointElementsList.size(); i++)
    	{
    		// Iterate the point element list
    		dataValue = pointElementsList.get(i);

    		// If the point is active (has data)
    		if (dataValue.isActive())
    		{
    			point = new Point((int)(xAxisLength * i / pointElementsList.size() + C_X_INSET_LEFT),(int)( -dataValue.getSum() / dataValue.getNumberOfElements() * yAxisLength / (yAxisMaxValue - yAxisMinValue) + canvas.getHeight() - C_Y_INSET_BOTTOM));
    			
    			// It's not the first point
    			if (started)
    			{
    				closedPath.lineTo(point.x, point.y);		// Draw the closed path that enclosures the graph area
    				superiorPath.lineTo(point.x, point.y);		// Draw the path that remarks the main chart line
				}
    			// It's the first point
    			else
    			{
    				started = true;								// Set started to true, so it's not the first item anymore after this
    				closedPath.reset();							// Reset the closed path because we are starting a new graph
    				closedPath.moveTo(point.x, point.y);		// Move the closed path to the first point
    				
    				superiorPath.reset();						// Reset the main path because we are starting a new graph
    				superiorPath.moveTo(point.x, point.y);		// Move the main path to the first point
    				
    				firstPoint = point;		// Save the first point
    			}
			}    		
		}
    	
    	// Adjust the y value to make it vertical to the bottom axis
    	point.y =  canvas.getHeight() - C_Y_INSET_BOTTOM;
    	
    	// Close the graph area
    	closedPath.lineTo(point.x, point.y);										// Move the closed path to the bottom right corner (end of the graph)    	
    	closedPath.lineTo(firstPoint.x, canvas.getHeight() - C_Y_INSET_BOTTOM);		// Move the closed path to the beginning and bottom of the graph
    	closedPath.lineTo(firstPoint.x, firstPoint.y);								// Closed the closed path moving it to the first point (the beginning)
    	
    	// Add the gradient to fill the chart area
    	paint_Line.setShader(new RadialGradient(C_X_INSET_LEFT + ((canvas.getWidth() - C_X_INSET_LEFT - C_X_INSET_RIGHT) / 2) , C_Y_INSET_TOP + ((canvas.getHeight() - C_Y_INSET_TOP - C_Y_INSET_BOTTOM) / 2) , canvas.getHeight(), getResources().getColor(R.color.orange_light), getResources().getColor(R.color.orange), Shader.TileMode.MIRROR));
    	
    	// Draw the closed path
    	canvas.drawPath(closedPath, paint_Line);
    	
    	// Draw the superior path (just the graph line)
    	canvas.drawPath(superiorPath,paint_Line_Border);
    	
    	// Clip the closed path
    	canvas.clipPath(closedPath);
    	
    	// Draw the vertical lines
    	canvas.drawPath(path_lines, paint_Line_Border);
	}
    
    /**
     * Draw the titles
     * 
     * @param canvas
     */
    private void drawTitles(Canvas canvas)
    {
    	paint_Text.setTextSize(getResources().getDimensionPixelSize(R.dimen.text_size_small));						// Modify the text size to draw the title of the chart and the Y axis title
    	canvas.drawText(getChartTitle() ,canvas.getWidth() / 2, C_Y_INSET_TOP / 2,paint_Text);						// Draw the chart title
    }
    
    /**
     * Implement this to do your drawing.
     */
    @Override
    protected void onDraw(Canvas canvas)
    {
    	super.onDraw(canvas);
    	
    	drawXAxis(canvas);		// Draw X axis    	
    	drawYAxis(canvas);		// Draw Y axis
    	drawTitles(canvas);		// Draw titles of the graph and the Y axis
    	
    	// If there is any information to draw, draw the chart
    	if (this.pointElementsList.size() > 0)
    	{
    		drawChart(canvas);
		}
    }

    /**
     * Get the chart title
     * 
     * @return chartTitle
     */
    public String getChartTitle()
    {
		return chartTitle;
	}

	/**
	 * Get the Y axis title
	 * 
	 * @return axisYTitle
	 */
	public String getAxisYTitle()
	{
		return axisYTitle;
	}

	/**
	 * Get the X axis title
	 * 
	 * @return axisXTitle
	 */
	public String getAxisXTitle()
	{
		return axisXTitle;
	}

	/**
	 * Set the X axis title
	 * 
	 * @param pAxisXTitle
	 */
	public void setAxisXTitle(String pAxisXTitle)
	{
		this.axisXTitle = pAxisXTitle;
	}
	
	/**
     * This method converts dp unit to equivalent pixels, depending on device density. 
     * 
     * @param dp A value in dp (density independent pixels) unit. Which we need to convert into pixels
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent px equivalent to dp depending on device density
     */
    public static float convertDpToPixel(float dp, Context context)
    {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return px;
    }

    /**
     * This method converts device specific pixels to density independent pixels.
     * 
     * @param px A value in px (pixels) unit. Which we need to convert into db
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent dp equivalent to px value
     */
    public static float convertPixelsToDp(float px, Context context)
    {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float dp = px / (metrics.densityDpi / 160f);
        return dp;
    } 
}
