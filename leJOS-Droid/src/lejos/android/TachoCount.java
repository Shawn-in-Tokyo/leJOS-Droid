package lejos.android;

import java.io.IOException;

import lejos.android.LeJOSDroid.*;
import lejos.nxt.Motor;
import lejos.nxt.Sound;
import lejos.nxt.remote.NXTCommand;
import lejos.pc.comm.NXTConnector;
import android.os.Bundle;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class TachoCount extends Thread{
    protected static final String TAG = "TachoCount";

    RefreshHandler mRedrawHandler;
    private LeJOSDroid mActivity;
    NXTConnector conn;

    public TachoCount(RefreshHandler mRedrawHandler, LeJOSDroid mActivity) {
	this.mRedrawHandler = mRedrawHandler;
	this.mActivity = mActivity;
    }
    
  
	    public void run() {
		Looper.prepare();
		conn = mActivity.connect(LeJOSDroid.TACHO_COUNT,
			CONN_TYPE.LEGO_LCP);
		Log.d(LeJOSDroid.TACHO_COUNT, "conn==null" + (conn == null));
		Log.d(TAG, "os=null " + (conn.getOutputStream() == null));
		NXTCommand.getSingleton().setNXTComm(conn.getNXTComm());
		Log.d(LeJOSDroid.TACHO_COUNT, "conn.getNXTComm()==null"
			+ (conn.getNXTComm() == null));
		try {
		    Log.i(LeJOSDroid.TACHO_COUNT, "Tachometer A: "
			    + Motor.A.getTachoCount());
		} catch (Exception e1) {
		    Log.e(LeJOSDroid.TACHO_COUNT, "Tachometer A: ", e1);

		}

		try {
		    sentMessageToUIThread(LeJOSDroid.MESSAGE,
			    LeJOSDroid.TACHO_COUNT, "Tachometer A:"
				    + Motor.A.getTachoCount());
		} catch (Exception e1) {
		    // TODO Auto-generated catch block
		    Log.e(LeJOSDroid.TACHO_COUNT,
			    "sentMessageToUIThread   ", e1);
		}

		Motor.A.rotate(500);
		Motor.C.rotate(-500);
		try {
		    Thread.sleep(1000);
		} catch (InterruptedException e) {
		    Log.e(LeJOSDroid.TACHO_COUNT, "Thread.sleep error", e);
		}
		Sound.playTone(1000, 1000);

		Log.i(LeJOSDroid.TACHO_COUNT,
			"Tachometer A: " + Motor.A.getTachoCount());
		// Log.i(test.TACHO_COUNT, "Tachometer C2: " +
		// Motor.C.getTachoCount());

		// b.putString(TestLeJOSDroid.TACHO_COUNT,
		// "(after movement) Tachometer A:"+ Motor.A.getTachoCount());
		// message.setData(b);
		// mRedrawHandler.sendMessage(message);
		if (conn != null) {
		    try {
			conn.close();
		    } catch (IOException e) {
			Log.e(LeJOSDroid.TACHO_COUNT,
				"Error closing connection", e);
		    }
		}
		Log.i(LeJOSDroid.TACHO_COUNT, "run finished");

		Looper.loop();
		Looper.myLooper().quit();
	    }

	    private void sentMessageToUIThread(int message_type,
		    String message_type_as_string, String message) {
		Bundle b = new Bundle();
		b.putString(message_type_as_string, message);
		Message message_holder = new Message();
		message_holder.setData(b);
		message_holder.what = message_type;
		mRedrawHandler.sendMessage(message_holder);
	    }


}
