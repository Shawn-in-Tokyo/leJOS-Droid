package lejos.android;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import lejos.pc.comm.NXTComm;
import lejos.pc.comm.NXTCommLogListener;
import lejos.pc.comm.NXTConnector;
import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class LeJOSDroid extends Activity {
	public static enum CONN_TYPE {
		LEGO_LCP, LEJOS_PACKET
	}


	static final String CONNECTING = "Connecting...";
	protected static final int DISPLAY_TOAST = 10;
	protected static final int DISPLAY_MESSAGE = 20;
	private static final String GO_AHEAD = "Choose one!";
	protected static final String MESSAGE = "message";
	static final String NXJ_CACHE = "nxt.cache";
	static final String START_MESSAGE = "Please make sure your NXT is on and both it and your Android handset have bluetooth enabled";
	public static final String TACHO_COUNT = "TachoCount";
	private final static String TAG = "TestLeJOSDroid";
 
	static final String YOUR_TURN = "Your Turn";
	private NXTConnector conn;
	private TextView mMessageView;
	public RefreshHandler mRedrawHandler = new RefreshHandler();

	private Toast reusableToast;

	private TachoCount tc;

	class RefreshHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			Log.d(TAG , " NXJ handleMessage:");
			switch (msg.what) {
				case DISPLAY_TOAST:
					showToast(msg.getData().getString(MESSAGE));
					break;
				case DISPLAY_MESSAGE:
					Log.d(TAG , " DISPLAY_MESSAGE:");
					mMessageView.setText((String) msg.getData().getString(MESSAGE));
					mMessageView.setVisibility(View.VISIBLE);
					mMessageView.requestLayout();
					break;
				
			}
		}

	}
	
	public NXTConnector connect(final String source, final CONN_TYPE connection_type) {
		Log.d(source, " about to add LEJOS listener ");

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
			case LEGO_LCP:
				// Log.i(source, " about to attempt LEGO connection ");
				conn.connectTo("btspp://NXT", NXTComm.LCP);
				break;
			case LEJOS_PACKET:
				// Log.i(source, " about to attempt LEJOS connection ");
				conn.connectTo("btspp://");
				break;
		}

		return conn;

	}

	protected void newApp() throws Exception {
		Log.i(YOUR_TURN, "Now get to work and write a great app!");

	}

	// @Override
	// public void onActivityResult(int requestCode, int resultCode, Intent
	// data) {
	// switch (requestCode) {
	// case REQUEST_CONNECT_DEVICE:
	// default:
	// break;
	// }
	// }

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mMessageView = (TextView) findViewById(R.id.messageText);
		seupNXJCache();

		setupTachoCount(this);
		setupBTSend(this);
		setupNewTemplate();
		reusableToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	private void setupBTSend(final LeJOSDroid leJOSDroid) {
		Button button;
		button = (Button) findViewById(R.id.button2);
		button.setOnClickListener(new View.OnClickListener() {

			public void onClick(View arg0) {
				try {
					BTSend btSend = new BTSend(mRedrawHandler, leJOSDroid);
					btSend.start();
				} catch (Exception e) {
					showToast("BTSend failed to start.  Is your NXT paired?  Is bluetooth on?");
					Log.e(BTSend.BT_SEND, "failed to run:" + e.getMessage(), e);
				}

			}
		});
	}

	private void setupNewTemplate() {
		Button button;
		button = (Button) findViewById(R.id.button3);
		button.setOnClickListener(new View.OnClickListener() {

			public void onClick(View arg0) {
				try {
					newApp();
				} catch (Exception e) {
					showToast("Failed to start.  Is your NXT paired?  Is bluetooth on?");
					Log.e(YOUR_TURN, e.getMessage());
				}
			}
		});
	}

	private void setupTachoCount(final LeJOSDroid mActivity) {
		Button button = (Button) findViewById(R.id.button1);

		button.setOnClickListener(new View.OnClickListener() {

			public void onClick(View arg0) {
				try {
					tc = new TachoCount(mRedrawHandler, mActivity);
					mMessageView.setVisibility(View.INVISIBLE);
					tc.start();
				} catch (Exception e) {
					showToast("TachoCount failed to start.  Is your NXT paired?  Is bluetooth on?");
					Log.e(TACHO_COUNT, "failed to run:" + e.getMessage(), e);
				}
			}

		});
	}

	private void seupNXJCache() {

		File root = Environment.getExternalStorageDirectory();

		try {
			String androidCacheFile = "nxj.cache";
			File mLeJOS_dir = new File(root + "/LeJOS");
			if (!mLeJOS_dir.exists()) {
				mLeJOS_dir.mkdir();
				// Log.d(NXJ_CACHE, "creating /LeJOS dir");
			}
			File mCacheFile = new File(root + "/LeJOS/", androidCacheFile);

			if (root.canWrite() && !mCacheFile.exists()) {
				FileWriter gpxwriter = new FileWriter(mCacheFile);
				BufferedWriter out = new BufferedWriter(gpxwriter);
				out.write("");
				out.flush();
				out.close();
				mMessageView.setText("nxj.cache (record of connection addresses) written to: " + mCacheFile.getName() + GO_AHEAD);
			} else {
				mMessageView.setText("nxj.cache file not written as"
						+ (!root.canWrite() ? mCacheFile.getName() + " can't be written to sdcard." : " cache already exists.") + GO_AHEAD);

			}
		} catch (IOException e) {
			Log.e(YOUR_TURN, "Could not write nxj.cache " + e.getMessage(), e);
		}
		mMessageView.setVisibility(View.VISIBLE);
		mMessageView.requestLayout();
	}

	private void showToast(String textToShow) {
		reusableToast.setText(textToShow);
		reusableToast.show();
	}

}