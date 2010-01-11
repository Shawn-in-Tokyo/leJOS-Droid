package lejos.android;

import java.io.IOException;

import lejos.android.test.*;
import lejos.nxt.Motor;
import lejos.nxt.Sound;
import lejos.nxt.remote.NXTCommand;
import lejos.pc.comm.NXTConnector;
import android.util.Log;

public class TachoCount  {
	
	public void countThoseTachos(){
		
		Thread ctt= new Thread(){
			
			public void run(){
				
				    NXTConnector conn = test.connect(test.TACHO_COUNT, CONN_TYPE.LEGO);
					NXTCommand.getSingleton().setNXTComm(conn.getNXTComm());

					Log.i(test.TACHO_COUNT, "Tachometer A1: " + Motor.A.getTachoCount());
					//only have one moment one hand for the moment to test!
					//Log.i(test.TACHO_COUNT, "Tachometer C1: " + Motor.C.getTachoCount());
					Motor.A.rotate(500);
					//Motor.C.rotate(-5000);
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						 Log.e(test.TACHO_COUNT,"Thread.sleep error",e);					}
					Sound.playTone(1000, 1000);
					Log.i(test.TACHO_COUNT, "Tachometer A2: " + Motor.A.getTachoCount());
					//Log.i(test.TACHO_COUNT, "Tachometer C2: " + Motor.C.getTachoCount());
					
					if( conn != null ){
						try {
							conn.close();
						} catch (IOException e) {
							 Log.e(test.TACHO_COUNT,"Error closing connection",e);
						}
					}
					Log.i(test.TACHO_COUNT, "run finished");
				}
			
			
		};
	
		ctt.start();
		
	}
		


}
