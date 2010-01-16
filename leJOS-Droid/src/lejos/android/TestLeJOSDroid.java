package lejos.android;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import lejos.android.display.DialogManager;
import lejos.android.display.ManagedDialogsActivity;
import lejos.pc.comm.NXTComm;
import lejos.pc.comm.NXTCommLogListener;
import lejos.pc.comm.NXTConnector;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class TestLeJOSDroid extends ManagedDialogsActivity {

	public static enum CONN_TYPE {
		LEJOS, LEGO
	}

	public static final String TACHO_COUNT = "TachoCount";
	static final String BT_SEND = "BTSend";
	static final String YOUR_TURN = "Your Turn";
	static final String NXJ_CACHE = "nxt.cache";
	static final String CONNECTING = "Connecting...";
	
	
	private final DialogManager _dialog_manager =new DialogManager(this,0, START_MESSAGE);
	TextView _message;
	
	static final String START_MESSAGE = "Please make sure you NXT is on and both it and your Android handset have bluetooth enabled";
	private static final String GO_AHEAD = "Choose one!";
	protected static final int MESSAGE = 0;
	
	/** Called when the activity is first created. */
	@Override
	
	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		registerDialog(_dialog_manager);
		_dialog_manager.show();
		
		setContentView(R.layout.main);
		_message = (TextView)findViewById(R.id.messageText);
		//updateUI();
		seupNXJCache();
		setupTachoCount();
		setupBTSend();
		setupNewTemplate();

	}

	private void setupNewTemplate() {
		Button button;
		button = (Button) findViewById(R.id.button3);
		button.setOnClickListener(new View.OnClickListener() {

			public void onClick(View arg0) {
				try {
					newApp();
				} catch (Exception e) {
					Log.e(YOUR_TURN, e.getMessage());
				}
			}
		});
	}

	private void setupBTSend() {
		Button button;
		button = (Button) findViewById(R.id.button2);
		button.setOnClickListener(new View.OnClickListener() {

			public void onClick(View arg0) {
				try {

					btSend();
				} catch (Exception e) {
					Log.e(BT_SEND, "failed to run:" + e.getMessage());
				}

			}
		});
	}

	private void setupTachoCount() {
		Button button = (Button) findViewById(R.id.button1);
		button.setOnClickListener(new View.OnClickListener() {

	    TachoCount tc = new TachoCount(_dialog_manager, mRedrawHandler);	
			
			public void onClick(View arg0) {
				try {
					_message.setVisibility(TextView.INVISIBLE);
					tc.countThoseTachos();
				} catch (Exception e) {
					Log.e(TACHO_COUNT, "failed to run:" + e.getMessage());
				}

			}

		});
	}

	public static NXTConnector connect(final String source,
			final CONN_TYPE connection_type) {

		NXTConnector conn;
		conn = new NXTConnector();

		conn.addLogListener(new NXTCommLogListener() {

			public void logEvent(String arg0) {
				Log.e(source + " NXJ log:", arg0);
			}

			public void logEvent(Throwable arg0) {
				Log.e(source + " NXJ log:", arg0.getMessage(), arg0);

			}

		});

		conn.setDebug(true);

		switch (connection_type) {
		case LEGO:
			Log.e(source, " about to attempt LEGO connection ");
			conn.connectTo("btspp://", NXTComm.LCP);

			// conn.connectTo("btspp://NXT", NXTComm.LCP) ;
			break;
		case LEJOS:
			Log.e(source, " about to attempt LEJOS connection ");
			conn.connectTo("btspp://");
			break;

		}

		return conn;

	}

	protected void btSend() throws Exception {
		Thread t = new Thread() {

			@Override
			public void run() {

				// we are going to talk to the LeJOS firmware so use LEJOS
				NXTConnector conn = connect(BT_SEND, CONN_TYPE.LEJOS);
				Log.e(BT_SEND, " after connect:");
				DataOutputStream dos = conn.getDataOut();
				DataInputStream dis = conn.getDataIn();

				for (int i = 0; i < 100; i++) {
					try {
						Log.e(BT_SEND, "Sending " + (i * 30000));
						dos.writeInt((i * 30000));
						dos.flush();

					} catch (IOException ioe) {
						Log.e(BT_SEND, "IO Exception writing bytes:");
						Log.e(BT_SEND, ioe.getMessage());
						break;
					}

					try {
						Log.e(BT_SEND, "Received " + dis.readInt());
					} catch (IOException ioe) {
						Log.e(BT_SEND, "IO Exception reading bytes:");
						Log.e(BT_SEND, ioe.getMessage());
						break;
					}
				}

				try {
					try {
						dis.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					try {
						dos.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					try {
						conn.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} finally {
					dis = null;
					dos = null;
					conn = null;
				}
			}

		};
		t.start();
	}

	protected void newApp() throws Exception {
		Environment.getExternalStorageDirectory();
		Log.i(YOUR_TURN, "Now get to work and write a great app!");

	}

	private void seupNXJCache() {
		
		

		
		File root = Environment.getExternalStorageDirectory();
		Log.i(YOUR_TURN, "Can we write to sdcard?:" + root.canWrite());
		//_message.requestLayout();
		
		try {
			String androidCacheFile = "nxj.cache";
			File _LeJOS_dir = new File(root + "/LeJOS");
			if ( !_LeJOS_dir.exists()) {
				_LeJOS_dir.mkdir();
				Log.e(NXJ_CACHE, "creading /LeJOS dir");
			}
			File _cache_file = new File(root + "/LeJOS/", androidCacheFile);
			
			if (root.canWrite() && !_cache_file.exists()) {
				FileWriter gpxwriter = new FileWriter(_cache_file);
				BufferedWriter out = new BufferedWriter(gpxwriter);
				out.write("Hello world!");
				out.flush();
				out.close();
				Log.e(YOUR_TURN, "File seems written to:"
						+ _cache_file.getName());
				_message.setText("nxj.cache (record of connection addresses) written to: "+_cache_file.getName()+GO_AHEAD);
			}else {
				 
				_message.setText("nxj.cache file not written as"+ (!root.canWrite()? _cache_file.getName()+" can't be written to sdcard.":" cache already exists.")+GO_AHEAD);
				 				
				
			}
		} catch (IOException e) {
			Log.e(YOUR_TURN, "Could not write nxj.cache " + e.getMessage(), e);
		}
		
		_message.setVisibility(View.VISIBLE);
		_message.requestLayout();
	}

	
	 public RefreshHandler mRedrawHandler = new RefreshHandler();

	   class RefreshHandler extends Handler {
	      @Override
	      public void handleMessage(Message msg) {
	    	  //refractor to recieve messages from different senders -- not always TestLeJOSDroid.TACHO_COUNT
	    	_message.setText((String)msg.getData().get(TestLeJOSDroid.TACHO_COUNT));
	    	//are the following necessary?
	    	_message.setVisibility(View.VISIBLE);
	  		_message.requestLayout();
	    
	      }

	      public void sleep(long delayMillis) {
	         this.removeMessages(0);
	         sendMessageDelayed(obtainMessage(0), delayMillis);
	      }
	   };


}