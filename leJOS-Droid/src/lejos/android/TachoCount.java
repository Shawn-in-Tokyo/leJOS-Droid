package lejos.android;

import java.io.IOException;

import lejos.android.test.*;
import lejos.nxt.Motor;
import lejos.nxt.Sound;
import lejos.nxt.remote.NXTCommand;
import lejos.pc.comm.NXTConnector;
import android.os.AsyncTask;
import android.util.Log;

public class TachoCount extends AsyncTask  {

 

	@Override
	protected Object doInBackground(Object... params) {
		NXTConnector conn;
	    conn = test.connect(test.TACHO_COUNT, CONN_TYPE.LEGO);
		NXTCommand.getSingleton().setNXTComm(conn.getNXTComm());

		Log.i(test.TACHO_COUNT, "Tachometer A: " + Motor.A.getTachoCount());
		Log.i(test.TACHO_COUNT, "Tachometer C: " + Motor.C.getTachoCount());
		Motor.A.rotate(5000);
		Motor.C.rotate(-5000);
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Sound.playTone(1000, 1000);
		Log.i(test.TACHO_COUNT, "Tachometer A: " + Motor.A.getTachoCount());
		Log.i(test.TACHO_COUNT, "Tachometer C: " + Motor.C.getTachoCount());
		
		if( conn != null ){
			try {
				conn.close();
			} catch (IOException e) {
				 Log.e(test.TACHO_COUNT,"Error closing connection",e);
			}
		}

		
		return null;  //alter to send feedback to ui via onPostExecute
	}
	
	protected void onPostExecute(Object result) {//executes back on UI thread
	       
    }

}
