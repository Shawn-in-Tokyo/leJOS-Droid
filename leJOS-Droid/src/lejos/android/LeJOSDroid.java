package lejos.android;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import lejos.pc.comm.NXTCommAndroid;
import lejos.pc.comm.NXTComm;
import lejos.pc.comm.NXTCommLogListener;
import lejos.pc.comm.NXTConnector;
import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class LeJOSDroid extends Activity {
    public static enum CONN_TYPE {
	LEJOS_PACKET, LEGO_LCP
    }
    class UIMessageHandler extends Handler {
	@Override
	public void handleMessage(Message msg) {

	    switch (msg.what) {
	    case NXTCommAndroid.MESSAGE:
		_message.setText((String) msg.getData().get(
			NXTCommAndroid.MESSAGE_CONTENT));

		break;

	    case LeJOSDroid.MESSAGE:
		_message.setText((String) msg.getData().get(
			LeJOSDroid.TACHO_COUNT));
		break;
	    }

	    _message.setVisibility(View.VISIBLE);
	    _message.requestLayout();

	}

	public void sleep(long delayMillis) {
	    this.removeMessages(0);
	    sendMessageDelayed(obtainMessage(0), delayMillis);
	}
    }

    private Toast reusableToast;

    private static final String TOAST_TEXT = "toastText";

    private static final int REQUEST_CONNECT_DEVICE = 1000;
    public static final String TACHO_COUNT = "TachoCount";

    static final String YOUR_TURN = "Your Turn";
    static final String NXJ_CACHE = "nxt.cache";
    static final String CONNECTING = "Connecting...";
    private final static String TAG = "TestLeJOSDroid";
    NXTConnector conn;
    TextView _message;
    public TachoCount tc;
    static final String START_MESSAGE = "Please make sure you NXT is on and both it and your Android handset have bluetooth enabled";
    private static final String GO_AHEAD = "Choose one!";
    protected static final int MESSAGE = 0;
    protected static final int DISPLAY_TOAST = 10;

    // receive messages from the BTCommunicator
    final Handler myHandler = new Handler() {
	@Override
	public void handleMessage(Message myMessage) {
	    switch (myMessage.getData().getInt("message")) {
	    case DISPLAY_TOAST:
		showToast(myMessage.getData().getString(TOAST_TEXT));
		break;

	    }
	}

	private void showToast(String string) {

	}
    };

    public UIMessageHandler mRedrawHandler = new UIMessageHandler();

    public NXTConnector connect(final String source,
	    final CONN_TYPE connection_type) {
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

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {

	super.onCreate(savedInstanceState);
	setContentView(R.layout.main);
	_message = (TextView) findViewById(R.id.messageText);
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
		    _message.setVisibility(View.INVISIBLE);
		    tc.start();
		} catch (Exception e) {
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
		_message.setText("nxj.cache (record of connection addresses) written to: "
			+ mCacheFile.getName() + GO_AHEAD);
	    } else {
		_message.setText("nxj.cache file not written as"
			+ (!root.canWrite() ? mCacheFile.getName()
				+ " can't be written to sdcard."
				: " cache already exists.") + GO_AHEAD);

	    }
	} catch (IOException e) {
	    Log.e(YOUR_TURN, "Could not write nxj.cache " + e.getMessage(), e);
	}
	_message.setVisibility(View.VISIBLE);
	_message.requestLayout();
    }

    private void showToast(String textToShow) {
	reusableToast.setText(textToShow);
	reusableToast.show();
    }

}