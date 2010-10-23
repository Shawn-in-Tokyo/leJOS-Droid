package lejos.android;

import java.io.IOException;

import lejos.android.TestLeJOSDroid.*;
import lejos.android.display.DialogManager;
import lejos.nxt.Motor;
import lejos.nxt.Sound;
import lejos.nxt.remote.NXTCommand;
import lejos.pc.comm.NXTConnector;
import android.os.Bundle;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class TachoCount {
	protected static final String TAG = "TachoCount";
	DialogManager _dialog_manager;  
	RefreshHandler mRedrawHandler;
	private TestLeJOSDroid mActivity;
	NXTConnector conn;

	public void setConn(NXTConnector conn) {
		this.conn = conn;
	}

 

	public TachoCount(DialogManager dialogManager, RefreshHandler mRedrawHandler, TestLeJOSDroid mActivity) {
		 this._dialog_manager=dialogManager;
		 this.mRedrawHandler=mRedrawHandler;
		 this.mActivity=mActivity;
		 
	}
 







	public void countThoseTachos() {
		Log.d(TAG,"countThoseTachos"  );
		Thread ctt = new Thread() {

			

			@Override
			public void run() {
				Looper.prepare();
				 // Log.d(TAG,"os=null "+(conn.getOutputStream()==null) );
				  conn = mActivity.connect(TestLeJOSDroid.TACHO_COUNT,
						CONN_TYPE.LEGO);
				  Log.d(TestLeJOSDroid.TACHO_COUNT, "conn==null"+(conn==null));
				  Log.d(TAG,"os=null "+(conn.getOutputStream()==null) );
				NXTCommand.getSingleton().setNXTComm(conn.getNXTComm());
				Log.d(TestLeJOSDroid.TACHO_COUNT, "conn.getNXTComm()==null"+(conn.getNXTComm()==null));
				try {
					Log.i(TestLeJOSDroid.TACHO_COUNT, "Tachometer A: "
							+ Motor.A.getTachoCount());
				} catch (Exception e1) {
					Log.e(TestLeJOSDroid.TACHO_COUNT, "Tachometer A: ",e1);
					 
				}

				
				
				try {
					sentMessageToUIThread(TestLeJOSDroid.MESSAGE,TestLeJOSDroid.TACHO_COUNT, "Tachometer A:"+ Motor.A.getTachoCount());
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					Log.e(TestLeJOSDroid.TACHO_COUNT, "sentMessageToUIThread   ",e1);
				}
				// only have one moment one hand for the moment to test!
				// Log.i(test.TACHO_COUNT, "Tachometer C1: " +
				// Motor.C.getTachoCount());
				
				Motor.A.rotate(500);
				// Motor.C.rotate(-5000);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					Log.e(TestLeJOSDroid.TACHO_COUNT, "Thread.sleep error", e);
				}
				Sound.playTone(1000, 1000);
				
				 
				Log.i(TestLeJOSDroid.TACHO_COUNT, "Tachometer A: "
						+ Motor.A.getTachoCount());
				// Log.i(test.TACHO_COUNT, "Tachometer C2: " +
				// Motor.C.getTachoCount());
			 
				//b.putString(TestLeJOSDroid.TACHO_COUNT, "(after movement) Tachometer A:"+ Motor.A.getTachoCount());
				//message.setData(b);
				//mRedrawHandler.sendMessage(message);
				if (conn != null) {
					try {
						conn.close();
					} catch (IOException e) {
						Log.e(TestLeJOSDroid.TACHO_COUNT, "Error closing connection", e);
					}
				}
				Log.i(TestLeJOSDroid.TACHO_COUNT, "run finished");
			
Looper.loop();
Looper.myLooper().quit();
}

			private void sentMessageToUIThread(int message_type, String message_type_as_string, String message) {
				Bundle b = new Bundle();
				b.putString(message_type_as_string, message);
				Message message_holder = new Message();
				message_holder.setData(b);
				message_holder.what=message_type;
				mRedrawHandler.sendMessage(message_holder);
				
				//"Tachometer A:"+ Motor.A.getTachoCount()
			   
			}

		};
		
		ctt.start();

	}

}
