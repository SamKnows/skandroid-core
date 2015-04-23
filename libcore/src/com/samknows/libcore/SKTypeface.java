package com.samknows.libcore;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.samknows.measurement.SKApplication;

import java.lang.reflect.Method;

public class SKTypeface {

  public static Typeface sGetTypefaceWithPathInAssets(String typefacePathInAssets) {
    Typeface result = null;

    // TODO - allow the app to override these!
    if (typefacePathInAssets.equals("fonts/roboto_condensed_regular.ttf")) {
    } else if (typefacePathInAssets.equals("fonts/roboto_light.ttf")) {
    } else if (typefacePathInAssets.equals("fonts/roboto_thin.ttf")) {
    } else if (typefacePathInAssets.equals("fonts/roboto_bold.ttf")) {
    } else if (typefacePathInAssets.equals("fonts/roboto_condensed_regular.ttf")) {
//    } else if (typefacePathInAssets.equals("typewriter.ttf")) {
//      Log.d("SKTypefaceUtil", "typewriter.ttf!");
//      SKLogger.sAssert(false);
    } else {
      Log.d("SKTypefaceUtil", "Unexpected font path " + typefacePathInAssets);
      SKLogger.sAssert(false);
    }

    // e.g. could override here! typefacePathInAssets = "typewriter.ttf";

    try {
      Context context = SKApplication.getAppInstance().getApplicationContext();
      result = Typeface.createFromAsset(context.getAssets(), typefacePathInAssets);

    } catch (Exception e) {
      Log.d("SKTypefaceUtil", "Cannot find custom font " + typefacePathInAssets);
      SKLogger.sAssert(false);
    }

    return result;
  }

  // http://stackoverflow.com/questions/2711858/is-it-possible-to-set-font-for-entire-application
  public static void sChangeChildrenFont(ViewGroup v, Typeface font) {
    for (int i = 0; i < v.getChildCount(); i++) {

      // For the ViewGroup, we'll have to use recursivity
      if (v.getChildAt(i) instanceof ViewGroup) {
        sChangeChildrenFont((ViewGroup) v.getChildAt(i), font);
      } else {
        try {
          Object[] nullArgs = null;
          //Test wether setTypeface and getTypeface methods exists
          Method methodTypeFace = v.getChildAt(i).getClass().getMethod("setTypeface", new Class[]{Typeface.class, Integer.TYPE});
          //With getTypefaca we'll get back the style (Bold, Italic...) set in XML
          Method methodGetTypeFace = v.getChildAt(i).getClass().getMethod("getTypeface", new Class[]{});
          Typeface typeFace = ((Typeface) methodGetTypeFace.invoke(v.getChildAt(i), nullArgs));
          //Invoke the method and apply the new font with the defined style to the view if the method exists (textview,...)
          methodTypeFace.invoke(v.getChildAt(i), new Object[]{font, typeFace == null ? 0 : typeFace.getStyle()});
        }
        //Will catch the view with no such methods (listview...)
        catch (Exception e) {
          SKLogger.sAssert(false);
        }
      }
    }
  }

  private static Typeface FONT_REGULAR = null;

  public static void initializeFonts() {
    //FONT_REGULAR = SKTypeface.sGetTypefaceWithPathInAssets("typewriter.ttf");
    FONT_REGULAR = SKApplication.getAppInstance().getDefaultTypeface();

//    if (FONT_REGULAR == null) {
//      // By default, we allow overriding from typewriter.ttf ..!
//      FONT_REGULAR = SKTypeface.sGetTypefaceWithPathInAssets("typewriter.ttf");
//    }

    if (FONT_REGULAR == null) {
      FONT_REGULAR = Typeface.DEFAULT;
    }
  }

  public static Typeface sGetDefaultTypeface() {
    if (FONT_REGULAR == null) {
      initializeFonts();
    }
    return FONT_REGULAR;
  }

  public static void sChangeChildrenToDefaultFontTypeface(ViewGroup v) {

    Typeface typeface = FONT_REGULAR;
    sChangeChildrenFont(v, typeface);
  }

  public static void overrideFonts(final Context context, final View v) {
    Typeface defaultTypeface = sGetDefaultTypeface();

    try {
      if (v instanceof ViewGroup) {
        ViewGroup vg = (ViewGroup) v;
        for (int i = 0; i < vg.getChildCount(); i++) {
          View child = vg.getChildAt(i);
          overrideFonts(context, child);
        }
      } else if (v instanceof TextView) {
        ((TextView) v).setTypeface(defaultTypeface);
      }
    } catch (Exception e) {
      SKLogger.sAssert(false);
    }
  }
}