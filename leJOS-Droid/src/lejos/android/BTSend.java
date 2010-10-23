package lejos.android;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import lejos.android.LeJOSDroid.CONN_TYPE;
import lejos.android.LeJOSDroid.RefreshHandler;
import lejos.pc.comm.NXTConnector;
import android.os.Bundle;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class BTSend extends Thread {
	static final String BT_SEND = "BTSend";
	NXTConnector conn;
	private LeJOSDroid mActivity;
	RefreshHandler mRedrawHandler;

	public BTSend(RefreshHandler mRedrawHandler, LeJOSDroid mActivity) {
		super();
		this.mRedrawHandler = mRedrawHandler;
		this.mActivity = mActivity;
	}

	@Override
	public void run() {

		Looper.prepare();
		// we are going to talk to the LeJOS firmware so use LEJOS
		conn = mActivity.connect(BT_SEND, CONN_TYPE.LEJOS_PACKET);
		Log.e(BT_SEND, " after connect:");

		DataOutputStream dos = conn.getDataOut();
		DataInputStream dis = conn.getDataIn();
		for (int i = 0; i < 100; i++) {
			try {
				// Log.d(BT_SEND, "Sending " + (i * 30000));
				dos.writeInt((i * 30000));
				dos.flush();
				sentMessageToUIThread("Read: " + dis.readInt(), LeJOSDroid.DISPLAY_MESSAGE);
			} catch (IOException e) {
				sentMessageToUIThread("IO Error", LeJOSDroid.DISPLAY_TOAST);
				Log.e(BT_SEND, "Error ... ", e);
			}
		}

		try {

			dis.close();
			dos.close();
			conn.close();
		} catch (Exception e) {
		} finally {
			dis = null;
			dos = null;
			conn = null;
		}
		Looper.loop();
		Looper.myLooper().quit();
	}

	private void sentMessageToUIThread(String message, int messageType) {
		Bundle b = new Bundle();
		b.putString(LeJOSDroid.MESSAGE, message);
		Message message_holder = new Message();
		message_holder.what = messageType;
		message_holder.setData(b);
		mRedrawHandler.sendMessage(message_holder);
	}

}
