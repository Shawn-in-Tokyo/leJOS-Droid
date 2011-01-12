package lejos.android;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import lejos.android.LeJOSDroid.CONN_TYPE;
import lejos.pc.comm.NXTConnector;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class BTSend extends Thread {
	static final String TAG = "BTSend";
	private NXTConnector conn;
	Handler mUIMessageHandler;
	DataOutputStream dos;
	DataInputStream dis;

	public BTSend(Handler mUIMessageHandler) {

		super();
		this.mUIMessageHandler = mUIMessageHandler;
	}

	public void closeConnection() {
		try {
			Log.d(TAG, "BTSend run loop finished and closing");

			dis.close();
			dos.close();
			conn.getNXTComm().close();
		} catch (Exception e) {
		} finally {
			dis = null;
			dos = null;
			conn = null;
		}
	}

	@Override
	public void run() {
		Log.d(TAG, "BTSend run");
		Looper.prepare();
		
		conn = LeJOSDroid.connect(CONN_TYPE.LEJOS_PACKET);

		dos = conn.getDataOut();
		dis = conn.getDataIn();
		int x;
		for (int i = 0; i < 100; i++) {

			try {
				dos.writeInt((i * 30000));
				dos.flush();
				LeJOSDroid.sendMessageToUIThread("sent:" + i * 30000);
				yield();
				x = dis.readInt();
				if (x > 0) {

					LeJOSDroid.displayToastOnUIThread("got: " + x);
				}
				Log.d(TAG, "sent:" + i * 30000 + " got:" + x);
				LeJOSDroid.sendMessageToUIThread("sent:" + i * 30000 + " got:" + x);
				yield();
			} catch (IOException e) {
				Log.e(TAG, "Error ... ", e);

			}

		}

		closeConnection();
		Looper.loop();
		Looper.myLooper().quit();
		LeJOSDroid.sendMessageToUIThread("");//clear 
		LeJOSDroid.displayToastOnUIThread("BTSend finished it's run");
	}

}
