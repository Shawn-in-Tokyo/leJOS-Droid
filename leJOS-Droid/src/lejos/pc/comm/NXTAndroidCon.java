package lejos.pc.comm;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
 

public class NXTAndroidCon extends Thread {
	public NXTAndroidCon(String mMACaddress, Handler uiHandler, BluetoothAdapter bluetoothAdapter) {
		super();
		this.mMACaddress = mMACaddress;
		this.uiHandler = uiHandler;
		this.btAdapter=bluetoothAdapter;
	}

	BluetoothAdapter btAdapter;
    private String mMACaddress;
    private static final UUID SERIAL_PORT_SERVICE_CLASS_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static final int STATE_CONNECTERROR = 1002;
    public static final int STATE_CONNECTED = 1001;
	private static final String TAG = "NXTAndroidCon";
    private BluetoothSocket nxtBTsocket = null;
    private OutputStream nxtOs = null;
    private Handler uiHandler;

	private InputStream nxtIs = null;
    private boolean connected = false;
	/**
     * Create bluetooth-connection with SerialPortServiceClass_UUID
     *
     * @see <a href=
     *      "http://lejos.sourceforge.net/forum/viewtopic.php?t=1991&highlight=android"
     *      />
     */
    private void createNXTconnection() {
        try {
            BluetoothSocket nxtBTsocketTEMPORARY;
            BluetoothDevice nxtDevice = null;
            nxtDevice = btAdapter.getRemoteDevice(mMACaddress);

            if (nxtDevice == null) {
               // sendToast(myMINDdroid.getResources().getString(R.string.no_paired_nxt));
                sendState(STATE_CONNECTERROR);
                return;
            }

            nxtBTsocketTEMPORARY = nxtDevice.createRfcommSocketToServiceRecord(SERIAL_PORT_SERVICE_CLASS_UUID);
            nxtBTsocketTEMPORARY.connect();
            nxtBTsocket = nxtBTsocketTEMPORARY;
    
            nxtIs =  nxtBTsocket.getInputStream();
            nxtOs =  nxtBTsocket.getOutputStream();
            Log.d(TAG," nxtIs==null "+(nxtIs==null));
            connected = true;

        } catch (IOException e) {
            //Log.d("BTCommunicator", "error createNXTConnection()", e);
//            if (myMINDdroid.pairing) {
//                sendToast(myMINDdroid.getResources().getString(R.string.pairing_message));
//                sendState(STATE_CONNECTERROR);
//
//            } else {
               sendState(STATE_CONNECTERROR);
//            }

            return;
        }

        sendState(STATE_CONNECTED);
    }
    
    @Override
    public void run() {
    	createNXTconnection();
 
    }
    
    public OutputStream getNxtOs() {
		return nxtOs;
	}

	public InputStream getNxtIs() {
		return nxtIs;
	}
	
	  private void sendState(int message) {
	        Bundle myBundle = new Bundle();
	        myBundle.putInt("message", message);
	        sendBundle(myBundle);
	    }

	    private void sendBundle(Bundle myBundle) {
	        Message myMessage = uiHandler.obtainMessage();
	        myMessage.setData(myBundle);
	        uiHandler.sendMessage(myMessage);
	    }

}
