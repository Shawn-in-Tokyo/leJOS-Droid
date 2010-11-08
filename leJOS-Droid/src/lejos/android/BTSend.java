package lejos.android;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import lejos.android.LeJOSDroid.CONN_TYPE;
import lejos.android.LeJOSDroid.UIMessageHandler;
import lejos.pc.comm.NXTCommAndroid;
import lejos.pc.comm.NXTConnector;
import android.os.Looper;
import android.util.Log;

public class BTSend extends Thread {
    static final String BT_SEND = "BTSend";
    NXTConnector conn;
    private LeJOSDroid mActivity;
    UIMessageHandler mRedrawHandler;

    public BTSend(UIMessageHandler mRedrawHandler, LeJOSDroid mActivity) {
	super();
	this.mRedrawHandler = mRedrawHandler;
	this.mActivity = mActivity;
    }

    @Override
    public void run() {

	Looper.prepare();
	// we are going to talk to the LeJOS firmware so use LEJOS
	conn = mActivity.connect(BT_SEND, CONN_TYPE.LEJOS_PACKET);
	NXTCommAndroid nca = (NXTCommAndroid) conn.getNXTComm();
	nca.setUIHander(mRedrawHandler);
	Log.e(BT_SEND, " after connect:");

	DataOutputStream dos = conn.getDataOut();
	DataInputStream dis = conn.getDataIn();
	int x;
	for (int i = 0; i < 100; i++) {

	    try {
		dos.writeInt((i * 30000));
		dos.flush();
		// Log.e(BT_SEND, "sent "+(i * 30000));
		nca.sentMessageToUIThread("sent:" + i * 30000);
		yield();
		x = dis.readInt();
		// Log.e(BT_SEND, "Received " + x);
		nca.sentMessageToUIThread("sent:" + i * 30000 + " got:" + x);
		yield();
	    } catch (IOException e) {
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

}
