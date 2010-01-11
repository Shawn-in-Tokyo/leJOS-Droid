package lejos.android.display;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;

public class DialogManager extends ManagedActivityDialog
{
   private String alertMessage = null;
   private Context ctx = null;

   public DialogManager(ManagedDialogsActivity inActivity,
                                    int dialogId,
                                    String initialMessage)
   {
      super(inActivity,dialogId);
      alertMessage = initialMessage;
      ctx = inActivity;
   }
   public Dialog create()
   {
      AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
      builder.setTitle("Alert");
      builder.setMessage(alertMessage);
      builder.setPositiveButton("Ok", this );
      AlertDialog ad = builder.create();
      return ad;
   }
   public void prepare(Dialog dialog)
   {
      AlertDialog ad = (AlertDialog)dialog;
      ad.setMessage(alertMessage);
   }
   public void setAlertMessage(String inAlertMessage)
   {
      alertMessage = inAlertMessage;
   }
   public void onClickHook(int buttonId)
   {
      //nothing to do
      //no local variables to set
   }
}