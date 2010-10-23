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

public class TachoCount extends Thread {
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
		conn = mActivity.connect(LeJOSDroid.TACHO_COUNT, CONN_TYPE.LEGO_LCP);
		NXTCommand.getSingleton().setNXTComm(conn.getNXTComm());

		sentMessageToUIThread("Tachometer A:" + Motor.A.getTachoCount(), LeJOSDroid.DISPLAY_MESSAGE);

		Motor.A.rotate(500);
		Motor.C.rotate(-500);
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			Log.e(LeJOSDroid.TACHO_COUNT, "Thread.sleep error", e);
		}
		Sound.playTone(1000, 1000);

		sentMessageToUIThread("Tachometer A:" + Motor.A.getTachoCount(), LeJOSDroid.DISPLAY_MESSAGE);

		if (conn != null) {
			try {
				conn.close();
			} catch (IOException e) {
				//Log.e(LeJOSDroid.TACHO_COUNT,
				//"Error closing connection", e);
			}
		}

		sentMessageToUIThread("TACHO_COUNT run finished" + Motor.A.getTachoCount(), LeJOSDroid.DISPLAY_TOAST);

		Looper.loop();
		Looper.myLooper().quit();
	}

	private void sentMessageToUIThread(String message, int messageType) {
		Log.e(LeJOSDroid.TACHO_COUNT, "sentMessageToUIThread "+message);
		Bundle b = new Bundle();
		b.putString(LeJOSDroid.MESSAGE, message);
		Message message_holder = new Message();
		message_holder.what = messageType;
		message_holder.setData(b);
		mRedrawHandler.sendMessage(message_holder);
	}

}
