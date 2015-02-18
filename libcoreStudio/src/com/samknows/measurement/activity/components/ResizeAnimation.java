package com.samknows.measurement.activity.components;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;

public class ResizeAnimation extends Animation {

    int originalHeight;
    int targetHeight;
    int offsetHeight;
    int adjacentHeightIncrement;
    View view, adjacentView;
    boolean down;

    //This constructor makes the animation start from height 0px
    public ResizeAnimation(View view, int offsetHeight, boolean down) {
        this.view           = view;
        this.originalHeight = 0;
        this.targetHeight   = 0;
        this.offsetHeight   = offsetHeight;
        this.down           = down;
    }
    
    //This constructor allow us to set a starting height
    public ResizeAnimation(View view, int originalHeight, int targetHeight, boolean down) {
        this.view           = view;
        this.originalHeight = originalHeight;
        this.targetHeight   = targetHeight;
        this.offsetHeight   = targetHeight - originalHeight;
        this.down           = down;
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        int newHeight;
        if (down) 
            newHeight = (int) (offsetHeight * interpolatedTime);
        
        else 
            newHeight = (int) (offsetHeight * (1 - interpolatedTime));
        
        //The new view height is based on start height plus the height increment
        view.getLayoutParams().height = newHeight + originalHeight;
        view.requestLayout();
        
        if (adjacentView != null) {
                        //This line is only triggered to animate and adjacent view 
            adjacentView.getLayoutParams().height = view.getLayoutParams().height + adjacentHeightIncrement;
            adjacentView.requestLayout();
        }
    }

    @Override
    public void initialize(int width, int height, int parentWidth,
            int parentHeight) {
        super.initialize(width, height, parentWidth, parentHeight);
    }

    @Override
    public boolean willChangeBounds() {
        return true;
    }
    
    public void setAdjacentView(View adjacentView) {
        this.adjacentView = adjacentView;
    }
    
    public void setAdjacentHeightIncrement(int adjacentHeightIncrement) {
        this.adjacentHeightIncrement = adjacentHeightIncrement;
    }
}