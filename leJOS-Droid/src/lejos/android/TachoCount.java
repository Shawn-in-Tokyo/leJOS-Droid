package lejos.android;

import java.io.IOException;

import lejos.android.LeJOSDroid.*;
import lejos.nxt.Motor;
import lejos.nxt.Sound;
import lejos.nxt.remote.NXTCommand;
import lejos.pc.comm.NXTCommAndroid;
import lejos.pc.comm.NXTCommLogListener;
import lejos.pc.comm.NXTConnector;
import android.os.Looper;
import android.util.Log;

public class TachoCount extends Thread {
	protected static final String TAG = "TachoCount";
	NXTConnector conn;

	public TachoCount() {

	}

	public void closeConnection() {
		try {
			Log.d(TAG, "TachoCount run loop finished and closing");
			conn.getNXTComm().close();
		} catch (Exception e) {
		} finally {

			conn = null;
		}

	}

	@Override
	public void run() {
		setName(TAG + " thread");
		Looper.prepare();
		conn = LeJOSDroid.connect(CONN_TYPE.LEGO_LCP);
		NXTCommand.getSingleton().setNXTComm(conn.getNXTComm());

		Motor.A.rotate(500);
		Motor.C.rotate(-500);
		LeJOSDroid.sendMessageToUIThread("T.A:" + Motor.A.getTachoCount() + " -- " + "T.C:" + Motor.C.getTachoCount());
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			Log.e(TAG, "Thread.sleep error", e);
		}
		LeJOSDroid.sendMessageToUIThread("");
		Sound.playTone(1000, 1000);

		if (conn != null) {
			try {
				conn.close();
			} catch (IOException e) {
				Log.e(TAG, "Error closing connection", e);
			}
		}
		closeConnection();
		Looper.loop();
		Looper.myLooper().quit();

		LeJOSDroid.displayToastOnUIThread("Tacho Count finished it's run");
	}

}
