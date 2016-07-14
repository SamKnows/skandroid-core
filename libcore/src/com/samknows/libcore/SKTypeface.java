package com.samknows.libcore;

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
    } else if (typefacePathInAssets.equals("fonts/roboto_regular.ttf")) {
    } else if (typefacePathInAssets.equals("fonts/Lato_Medium.ttf")) {
    } else if (typefacePathInAssets.equals("fonts/Lato_MediumItalic.ttf")) {
    } else if (typefacePathInAssets.equals("fonts/Lato_Thin.ttf")) {
    } else if (typefacePathInAssets.equals("fonts/Lato_ThinItalic.ttf")) {
    } else if (typefacePathInAssets.equals("fonts/Lato_Light.ttf")) {
    } else if (typefacePathInAssets.equals("fonts/Lato_LightItalic.ttf")) {
    } else if (typefacePathInAssets.equals("typewriter.ttf")) {
//      Log.d("SKTypefaceUtil", "typewriter.ttf!");
//      SKLogger.sAssert(false);
    } else {
      Log.d("SKTypefaceUtil", "App requested custom font path (" + typefacePathInAssets + ")");
    }

    // e.g. could override here! typefacePathInAssets = "typewriter.ttf";

    try {
      result = SKApplication.getAppInstance().createTypefaceFromAsset(typefacePathInAssets);
//      Context context = SKApplication.getAppInstance().getApplicationContext();
//      result = Typeface.createFromAsset(context.getAssets(), typefacePathInAssets);

    } catch (Exception e) {
      Log.d("SKTypefaceUtil", "Cannot find custom font " + typefacePathInAssets);
      SKPorting.sAssert(false);
    }

    return result;
  }

  public static void sSetTypefaceForTextView(TextView textView, Typeface typeface) {
    //textView.setPaintFlags(textView.getPaintFlags() | Paint.SUBPIXEL_TEXT_FLAG | Paint.DEV_KERN_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
    textView.getPaint().setSubpixelText(true);
    textView.setTypeface(typeface);
  }


  private static void setTypefaceForViewIfPossible(View theView, Typeface font) {
    try {
      Object[] nullArgs = null;
      Method methodTypeFace = theView.getClass().getMethod("setTypeface", new Class[]{Typeface.class, Integer.TYPE});
      //With getTypefaca we'll get back the style (Bold, Italic...) set in XML
      Method methodGetTypeFace = theView.getClass().getMethod("getTypeface", new Class[]{});
      Typeface typeFace = ((Typeface) methodGetTypeFace.invoke(theView, nullArgs));
      //Invoke the method and apply the new font with the defined style to the view if the method exists (textview,...)
      methodTypeFace.invoke(theView, new Object[]{font, typeFace == null ? 0 : typeFace.getStyle()});
    } catch (Exception e) {
      //  Ignore!
    }
  }

  // http://stackoverflow.com/questions/2711858/is-it-possible-to-set-font-for-entire-application
  private static void sChangeChildrenFont(View view, Typeface font) {

    if (view instanceof ViewGroup) {
      // Iterate through the view group!
      ViewGroup vg = (ViewGroup) view;

      for (int i = 0; i < vg.getChildCount(); i++) {
        // For the ViewGroup, use recursion.
        sChangeChildrenFont(vg.getChildAt(i), font);
      }
    } else {
      // Simply a view (e.g. TextView etc. ...) - try setting DIRECTLY.
      setTypefaceForViewIfPossible(view, font);
    }
  }

  private static Typeface FONT_REGULAR = null;

  private static void sInitializeFonts() {
    //FONT_REGULAR = SKTypeface.sGetTypefaceWithPathInAssets("typewriter.ttf");

    if (FONT_REGULAR != null) {
      // Already got it!
      return;
    }

    // This call will return null *if* the app doesn't provide a specific override.

    if (SKApplication.getAppInstance() == null) {
      FONT_REGULAR = Typeface.DEFAULT;
    } else {
      FONT_REGULAR = SKApplication.getAppInstance().getDefaultTypeface();

      if (FONT_REGULAR == null) {
        FONT_REGULAR = Typeface.DEFAULT;
      }
    }
  }

  public static Typeface sGetDefaultTypeface() {
    if (FONT_REGULAR == null) {
      sInitializeFonts();
    }
    return FONT_REGULAR;
  }

  public static void sChangeChildrenToDefaultFontTypeface(View v) {

    SKPorting.sAssert(v != null);

    //overrideFonts(SKApplication.getAppInstance().getApplicationContext(), v);

    Typeface defaultTypeface = sGetDefaultTypeface();
    sChangeChildrenFont(v, defaultTypeface);
  }

}