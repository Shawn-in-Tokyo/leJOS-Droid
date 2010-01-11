package lejos.android;

import java.io.IOException;

import lejos.android.TestLeJOSDroid.*;
import lejos.android.display.DialogManager;
import lejos.nxt.Motor;
import lejos.nxt.Sound;
import lejos.nxt.remote.NXTCommand;
import lejos.pc.comm.NXTConnector;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

public class TachoCount {
	DialogManager _dialog_manager;  
	RefreshHandler mRedrawHandler;
	
	public TachoCount(DialogManager _dialog_manager,RefreshHandler mRedrawHandler) {
		 this._dialog_manager=_dialog_manager;
		 this.mRedrawHandler=mRedrawHandler;
	}

	public void countThoseTachos() {

		Thread ctt = new Thread() {

			@Override
			public void run() {

				NXTConnector conn = TestLeJOSDroid.connect(TestLeJOSDroid.TACHO_COUNT,
						CONN_TYPE.LEGO);
				NXTCommand.getSingleton().setNXTComm(conn.getNXTComm());
				
				Log.i(TestLeJOSDroid.TACHO_COUNT, "Tachometer A: "
						+ Motor.A.getTachoCount());

				Message message = new Message();
				Bundle b = new Bundle();
				b.putString(TestLeJOSDroid.TACHO_COUNT, "Tachometer A:"+ Motor.A.getTachoCount());
				message.setData(b);
				message.what=TestLeJOSDroid.MESSAGE;
				mRedrawHandler.sendMessage(message);
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
			 
				b.putString(TestLeJOSDroid.TACHO_COUNT, "(after movement) Tachometer A:"+ Motor.A.getTachoCount());
				message.setData(b);
				mRedrawHandler.sendMessage(message);
				if (conn != null) {
					try {
						conn.close();
					} catch (IOException e) {
						Log.e(TestLeJOSDroid.TACHO_COUNT, "Error closing connection", e);
					}
				}
				Log.i(TestLeJOSDroid.TACHO_COUNT, "run finished");
			}

		};
		
		ctt.start();

	}

}
