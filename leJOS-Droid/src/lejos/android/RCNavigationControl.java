package lejos.android;

import android.app.TabActivity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TabHost;
import android.widget.TextView;

public class RCNavigationControl extends TabActivity {
	protected static final String TAG = "RCNavigationControl";
	private boolean connected;
	private Handler mUIMessageHandler;
	private RCNavComms communicator;
	TextView mMessage;
	EditText mName;
	EditText mAddress;
	private ConnectThread mConnectThread;
	public static final String ROBOT_POS = "rp";

	class UIMessageHandler extends Handler {
		float[] pos;

		@Override
		public void handleMessage(Message msg) {
			//Log.d(TAG, "handleMessage");
			switch (msg.what) {

				case LeJOSDroid.MESSAGE:
					//Log.d(TAG, (String) msg.getData().get(LeJOSDroid.MESSAGE_CONTENT));
					mMessage.setText((String) msg.getData().get(LeJOSDroid.MESSAGE_CONTENT));
					break;

				default:
					pos = msg.getData().getFloatArray(ROBOT_POS);
					showtRobotPosition(pos[0], pos[1], pos[2]);
			}

		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate 0");
		TabHost tabHost = getTabHost();

		LayoutInflater.from(this).inflate(R.layout.rc_nav_control, tabHost.getTabContentView(), true);

		tabHost.addTab(tabHost.newTabSpec("tab1").setIndicator(getResources().getText(R.string.connect_tab)).setContent(R.id.view1));
		tabHost.addTab(tabHost.newTabSpec("tab3").setIndicator(getResources().getText(R.string.command_tab)).setContent(R.id.view2));
		tabHost.addTab(tabHost.newTabSpec("tab3").setIndicator(getResources().getText(R.string.xyz_tab)).setContent(R.id.view3));

		Log.d(TAG, "onCreate 1");
		mMessage = (TextView) findViewById(R.id.message_status);
		Log.d(TAG, "onCreate 2");

		mName = (EditText) findViewById(R.id.name_edit);
		Log.d(TAG, "onCreate 3");

		mAddress = (EditText) findViewById(R.id.address_edit);

	}

	private void doConnect() {

		Log.d(TAG, "doConnect");
		mConnectThread = new ConnectThread();
		mConnectThread.start();
	}

	private void goButtonMouseClicked() {//GEN-FIRST:event_goButtonMouseClicked
		if (!connected)
			return;

		float x;
		float y;
		try {
			EditText XField = (EditText) findViewById(R.id.goto_x_edit);
			EditText YField = (EditText) findViewById(R.id.goto_y_edit);
			mMessage.setText("GoTo " + XField.getText() + " " + YField.getText());

			x = Float.parseFloat(XField.getText().toString());
			y = Float.parseFloat(YField.getText().toString());
			System.out.println("Sent " + Command.GOTO + " x " + x + " y " + y);
			communicator.send(Command.GOTO, x, y);
			mMessage.setText("waiting for data");
		} catch (NumberFormatException e) {
			mMessage.setText("Invalid x, y values");
		}
	}//GEN-LAST:event_goButtonMouseClicked

	private void travelButtonMouseClicked() {//GEN-FIRST:event_travelButtonMouseClicked
		if (!connected)
			return;
		EditText distanceField = (EditText) findViewById(R.id.travel_edit);
		mMessage.setText("Travel " + distanceField.getText());
		float distance;
		try {
			distance = Float.parseFloat(distanceField.getText().toString());
			System.out.println("Sent " + Command.TRAVEL + " " + distance);
			communicator.send(Command.TRAVEL, distance);
			mMessage.setText("waiting for data");
		} catch (NumberFormatException e) {
			mMessage.setText("Invalid distance value");
		}
	}//GEN-LAST:event_travelButtonMouseClicked

	private void rotateButtonMouseClicked() {//GEN-FIRST:event_rotateButtonMouseClicked
		if (!connected)
			return;
		EditText angleField = (EditText) findViewById(R.id.rotate_edit);
		mMessage.setText("Rotate " + angleField.getText());
		float angle;
		try {
			angle = Float.parseFloat(angleField.getText().toString());
			System.out.println("Sent " + Command.ROTATE + " " + angle);
			communicator.send(Command.ROTATE, angle);
			mMessage.setText("waiting for data");
		} catch (NumberFormatException e) {
			mMessage.setText("Invalid angle value");
		}
	}

	public void handleGoTo(View v) {
		goButtonMouseClicked();
	}

	public void handleConnect(View v) {
		Log.d(TAG, "handleConnect called ");
		doConnect();
	}

	public void handleRotate(View v) {
		rotateButtonMouseClicked();
	}

	public void handleTravel(View v) {
		travelButtonMouseClicked();
	}

	@Override
	protected void onPause() {

		super.onPause();
		communicator.end();
		try {
			communicator.getConnector().close();
		} catch (Exception e) {
			Log.e(TAG, "onPause() error closing NXTComm ", e);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		mUIMessageHandler = new UIMessageHandler();
		communicator = new RCNavComms(mUIMessageHandler);
	}

	public void showtRobotPosition(float x, float y, float heading) {
		Log.d(TAG, "showtRobotPosition");
		final TextView xField = (TextView) findViewById(R.id.x);
		final TextView yField = (TextView) findViewById(R.id.y);
		final TextView headingField = (TextView) findViewById(R.id.heading);
		xField.setText("" + x);
		yField.setText("" + y);
		headingField.setText("" + heading);
		mMessage.setText("waiting for command");

	}

	private class ConnectThread extends Thread {

		@Override
		public void run() {
			Looper.prepare();
			setName("RCNavigationControl ConnectThread");
			Editable name = mName.getText();
			Editable address = mAddress.getText();
			sendMessageToUIThread("Connecting ... ");
			if (!communicator.connect(name.toString(), address.toString())) {
				sendMessageToUIThread("Connection Failed");
				connected = false;
			} else {
				sendMessageToUIThread("Connected to " + communicator.getConnector().getNXTInfo().name);

				connected = true;
			}
			Looper.loop();
		}

		public void sendMessageToUIThread(String message) {
			//Log.d(TAG,"sendMessageToUIThread: "+message);
			Bundle b = new Bundle();
			b.putString(LeJOSDroid.MESSAGE_CONTENT, message);
			Message message_holder = new Message();
			message_holder.setData(b);
			message_holder.what = LeJOSDroid.MESSAGE;
			mUIMessageHandler.sendMessage(message_holder);
		}

	}

}
