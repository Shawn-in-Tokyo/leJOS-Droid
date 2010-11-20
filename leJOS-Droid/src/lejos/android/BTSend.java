package lejos.android;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import lejos.android.LeJOSDroid.CONN_TYPE;
import lejos.pc.comm.NXTCommAndroid;
import lejos.pc.comm.NXTConnector;
import android.os.Looper;
import android.util.Log;

public class BTSend extends Thread {
    static final String TAG = "BTSend";
    private NXTConnector conn;
    private LeJOSDroid mActivity;

    public BTSend(LeJOSDroid mActivity) {
	super();
	this.mActivity = mActivity;
    }

    @Override
    public void run() {

	Looper.prepare();
	conn = mActivity.connect(CONN_TYPE.LEJOS_PACKET);
	NXTCommAndroid nca = (NXTCommAndroid) conn.getNXTComm();
	DataOutputStream dos = conn.getDataOut();
	DataInputStream dis = conn.getDataIn();
	int x;
	for (int i = 0; i < 100; i++) {

	    try {
		dos.writeInt((i * 30000));
		dos.flush();
		nca.sendMessageToUIThread("sent:" + i * 30000);
		yield();
		x = dis.readInt();
		nca.sendMessageToUIThread("sent:" + i * 30000 + " got:" + x);
		yield();
	    } catch (IOException e) {
		Log.e(TAG, "Error ... ", e);

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
	nca.sendMessageToUIThread("");
	nca.displayToastOnUIThread("BTSend finished it's run");
    }

}
