package lejos.android.display;

import android.app.Dialog;

public interface IDialogProtocol {

	public Dialog create();
	public void prepare(Dialog dialog);
	public void show();
	public void onClickHook(int buttonId);
	public int getDialogId();
}
