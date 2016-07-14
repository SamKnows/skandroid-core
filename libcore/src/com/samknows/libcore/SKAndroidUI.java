package com.samknows.libcore;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

public class SKAndroidUI {
  /**
   * This method converts dp unit to equivalent pixels, depending on device density.
   *
   * @param dp      A value in dp (density independent pixels) unit. Which we need to convert into pixels
   * @param context Context to get resources and device specific display metrics
   * @return A float value to represent px equivalent to dp depending on device density
   */
  public static float sConvertDpToPixels(float dp, Context context) {
    Resources resources = context.getResources();
    DisplayMetrics metrics = resources.getDisplayMetrics();
    float px = dp * (metrics.densityDpi / 160f);
    return px;
  }

  /**
   * This method converts device specific pixels to density independent pixels.
   *
   * @param px      A value in px (pixels) unit. Which we need to convert into db
   * @param context Context to get resources and device specific display metrics
   * @return A float value to represent dp equivalent to px value
   */
  public static float sConvertPixelsToDp(float px, Context context) {
    Resources resources = context.getResources();
    DisplayMetrics metrics = resources.getDisplayMetrics();
    float dp = px / (metrics.densityDpi / 160f);
    return dp;
  }



  //
  // Helpful dialogs for prompting the user...
  //

  private static class DialogActionClickListener implements DialogInterface.OnClickListener
  {
    private Runnable mActionRunnable;

    private DialogActionClickListener( Runnable actionRunnable )
    {
      mActionRunnable = actionRunnable;
    }

    @Override
    public void onClick( DialogInterface dialog, int which )
    {
      dialog.dismiss();

      // If no action runnable is supplied, the dialog will simply be dismissed
      // and no further action will be taken.
      if ( mActionRunnable != null ) mActionRunnable.run();
    }
  }

  // Displays an alert dialog with two buttons.
  public static void displayPositiveNegativeAlertDialog( Context context, int titleResourceId, int iconResourceId, int messageResourceId, int negativeButtonTextResourceId, Runnable negativeRunnable, int positiveButtonTextResourceId, Runnable positiveRunnable )
  {
    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder( context );

    if ( titleResourceId != 0 )
    {
      dialogBuilder.setTitle( titleResourceId );

      // The icon won't work without a title
      if ( iconResourceId != 0 ) dialogBuilder.setIcon( iconResourceId );
    }

    if ( negativeButtonTextResourceId != 0 ) dialogBuilder.setNegativeButton( negativeButtonTextResourceId, new DialogActionClickListener( negativeRunnable ) );
    if ( positiveButtonTextResourceId != 0 ) dialogBuilder.setPositiveButton( positiveButtonTextResourceId, new DialogActionClickListener( positiveRunnable ) );

    dialogBuilder
        .setMessage( messageResourceId )
        .create()
        .show();
  }


  private static final int       NO_ICON                           = 0;
  private static final int       NO_BUTTON                         = 0;
  private static final Runnable  NO_ACTION                         = null;
  private static final Runnable  DISMISS_DIALOG                    = null;

  public void displayPositiveNegativeAlertDialog( Context context, int titleResourceId, int messageResourceId, int negativeButtonTextResourceId, Runnable negativeRunnable, int positiveButtonTextResourceId, Runnable positiveRunnable )
  {
    displayPositiveNegativeAlertDialog( context, titleResourceId, NO_ICON, messageResourceId, negativeButtonTextResourceId, negativeRunnable, positiveButtonTextResourceId, positiveRunnable );
  }


  public void displayActionDismissAlertDialog( Context context, int titleResourceId, int iconResourceId, int messageResourceId, int dismissButtonTextResourceId, int actionButtonTextResourceId, Runnable actionRunnable )
  {
    displayPositiveNegativeAlertDialog( context, titleResourceId, iconResourceId, messageResourceId, dismissButtonTextResourceId, null, actionButtonTextResourceId, actionRunnable );
  }


  public static void displayActionDismissAlertDialog( Context context, int titleResourceId, int messageResourceId, int dismissButtonTextResourceId, int actionButtonTextResourceId, Runnable actionRunnable )
  {
    displayPositiveNegativeAlertDialog( context, titleResourceId, NO_ICON, messageResourceId, dismissButtonTextResourceId, null, actionButtonTextResourceId, actionRunnable );
  }


  public void displayDismissAlertDialog( Context context, int titleResourceId, int iconResourceId, int messageResourceId, int dismissButtonTextResourceId )
  {
    displayPositiveNegativeAlertDialog( context, titleResourceId, iconResourceId, messageResourceId, dismissButtonTextResourceId, DISMISS_DIALOG, NO_BUTTON, NO_ACTION );
  }

  public void displaySingleButtonAlertDialog(Context context, int messageResourceId, int actionButtonTextResourceId) {
    displayActionDismissAlertDialog(context, 0, 0, messageResourceId, actionButtonTextResourceId, 0, null);
  }

  public static void displayDismissAlertDialog( Context context, int titleResourceId, int messageResourceId, int dismissButtonTextResourceId )
  {
    displayPositiveNegativeAlertDialog( context, titleResourceId, NO_ICON, messageResourceId, dismissButtonTextResourceId, DISMISS_DIALOG, NO_BUTTON, NO_ACTION );
  }

  public void displayActionAlertDialog( Context context, int titleResourceId, int iconResourceId, int messageResourceId, int actionButtonTextResourceId, Runnable actionRunnable )
  {
    displayPositiveNegativeAlertDialog( context, titleResourceId, iconResourceId, messageResourceId, NO_BUTTON, NO_ACTION, actionButtonTextResourceId, actionRunnable );
  }

  public void displayActionAlertDialog( Context context, int titleResourceId, int messageResourceId, int actionButtonTextResourceId, Runnable actionRunnable )
  {
    displayPositiveNegativeAlertDialog( context, titleResourceId, NO_ICON, messageResourceId, NO_BUTTON, NO_ACTION, actionButtonTextResourceId, actionRunnable );
  }

  public void displayPopupMessage( Context context, CharSequence messageText )
  {
    Toast.makeText( context, messageText, Toast.LENGTH_LONG ).show();
  }

  public void displayPopupMessage( Context context, int messageResourceId )
  {
    Toast.makeText( context, messageResourceId, Toast.LENGTH_LONG ).show();
  }
}
