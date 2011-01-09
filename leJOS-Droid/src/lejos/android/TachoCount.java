package lejos.android;

import java.io.IOException;

import lejos.android.LeJOSDroid.*;
import lejos.nxt.Motor;
import lejos.nxt.Sound;
import lejos.nxt.remote.NXTCommand;
import lejos.pc.comm.NXTCommAndroid;
import lejos.pc.comm.NXTConnector;
import android.os.Looper;
import android.util.Log;

public class TachoCount extends Thread {
    protected static final String TAG = "TachoCount";
    private LeJOSDroid mActivity;
    NXTConnector conn;

    public TachoCount(LeJOSDroid mActivity) {
	this.mActivity = mActivity;
    }

    @Override
    public void run() {
	Looper.prepare();
	conn = mActivity.connect(CONN_TYPE.LEGO_LCP);
	NXTCommAndroid nca = (NXTCommAndroid) conn.getNXTComm();
	NXTCommand.getSingleton().setNXTComm(conn.getNXTComm());
	
	Motor.A.rotate(500);
	Motor.C.rotate(-500);
	nca.sendMessageToUIThread("T.A:" + Motor.A.getTachoCount()+" -- "+"T.C:" + Motor.C.getTachoCount() );
	try {
	    Thread.sleep(1000);
	} catch (InterruptedException e) {
	    Log.e(TAG, "Thread.sleep error", e);
	}
	nca.sendMessageToUIThread("");
	Sound.playTone(1000, 1000);

	if (conn != null) {
	    try {
		conn.close();
	    } catch (IOException e) {
		Log.e(TAG, "Error closing connection", e);
	    }
	}

	Looper.loop();
	Looper.myLooper().quit();
	
	nca.displayToastOnUIThread("Tacho Count finished it's run");
    }

}
