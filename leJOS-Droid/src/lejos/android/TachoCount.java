package lejos.android;

import java.io.IOException;

import lejos.android.test.*;
import lejos.nxt.Motor;
import lejos.nxt.Sound;
import lejos.nxt.remote.NXTCommand;
import lejos.pc.comm.NXTComm;
import lejos.pc.comm.NXTCommLogListener;
import lejos.pc.comm.NXTConnector;
import android.os.AsyncTask;
import android.util.Log;

public class TachoCount extends AsyncTask {

	@Override
	protected Object doInBackground(Object... params) {

		String source = test.TACHO_COUNT;
		NXTConnector conn = new NXTConnector();

		conn.addLogListener(new NXTCommLogListener() {

			public void logEvent(String arg0) {
				Log.e(test.TACHO_COUNT + " NXJ log:", arg0);

			}

			public void logEvent(Throwable arg0) {
				Log.e(test.TACHO_COUNT + " NXJ log:", arg0.getMessage(), arg0);

			}

		});

		conn.setDebug(true);

		Log.e(test.TACHO_COUNT, " about to attempt LEGO connection ");
		conn.connectTo("btspp://", NXTComm.LCP);

		NXTCommand.getSingleton().setNXTComm(conn.getNXTComm());

		Log.i(test.TACHO_COUNT, "1 Tachometer A: " + Motor.A.getTachoCount());
	    Log.i(test.TACHO_COUNT, "1 Tachometer C: " + Motor.C.getTachoCount());
		Motor.A.rotate(5000);
	 	Motor.C.rotate(-5000);
		
		Log.i(test.TACHO_COUNT, "before sleep ");
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			 Log.e(test.TACHO_COUNT, e.getMessage(), e);
		}
		
		Sound.playTone(1000, 1000);
		Log.i(test.TACHO_COUNT, "2 Tachometer A: " + Motor.A.getTachoCount());
		Log.i(test.TACHO_COUNT, "2 Tachometer C: " + Motor.C.getTachoCount());

		if (conn != null) {
			try {
				conn.close();
			} catch (IOException e) {
				Log.e(test.TACHO_COUNT, "Error closing connection", e);
			}
		}
		Log.i(test.TACHO_COUNT, "Conn closed: ");

		return "arg check"; // alter to send feedback to ui 
	}

	protected void onPostExecute(Object result) {// executes back on UI thread
		Log.i(test.TACHO_COUNT, " onPostExecute ");
	}

}
