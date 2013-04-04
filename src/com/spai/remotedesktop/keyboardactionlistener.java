package com.spai.remotedesktop;

import android.app.Activity;
import android.content.SharedPreferences;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView.OnKeyboardActionListener;
import android.util.Log;
import android.view.KeyEvent;

public class keyboardactionlistener implements OnKeyboardActionListener {

	private Activity mTargetActivity;
	SharedPreferences pref;

	keyboardview customview;
	Keyboard customkeyboard;
	String ctrl = "nor";
	boolean shiftPressed;
	boolean kshiftpressed;
	boolean ctrlpressed;
	boolean altpressed;
	boolean capspressed;
	connect obj;
	keycode object;

	final String prefname = "wifiprefs";

	public keyboardactionlistener(Activity targetActivity, Keyboard Keyb,
			keyboardview view, boolean[] state, connect ob) {
		mTargetActivity = targetActivity;
		customview = view;
		customkeyboard = Keyb;
		shiftPressed = state[0];
		kshiftpressed = state[1];
		ctrlpressed = state[2];
		altpressed = state[3];
		capspressed = state[4];
		obj = ob;

		object = new keycode();

	}

	@Override
	public void swipeUp() {
		// TODO Auto-generated method stub

	}

	@Override
	public void swipeRight() {
		// TODO Auto-generated method stub

	}

	@Override
	public void swipeLeft() {
		// TODO Auto-generated method stub

	}

	@Override
	public void swipeDown() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onText(CharSequence text) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onRelease(int primaryCode) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPress(int primaryCode) {
		// TODO Auto-generated method stub

	}

	public void onKey(int primaryCode, int[] keyCodes) {

		String s = null;

		if (primaryCode > 0) {

			if (primaryCode == 59 && !shiftPressed) {

				customview.invalidateAllKeys();
				customkeyboard.setShifted(true);
				shiftPressed = true;
				pressKey(primaryCode);
				ctrl = "shift";

			} else if (primaryCode == 59 && shiftPressed) {

				Log.v("shift ", "dismissed");
				shiftPressed = false;
				customkeyboard.setShifted(false);
				customview.invalidateAllKeys();
				releaseKey(primaryCode);
				ctrl = "nor";

			} else {
				pressKey(primaryCode);
				s = "key," + ctrl + "," + primaryCode;
			}
		}

		else if (primaryCode >= -58 && primaryCode <= -37) {
			int alt;
			alt = object.key(primaryCode);
			pressKey(59);
			s = "key,shift," + alt;
			pressKey(alt);
			releaseKey(59);
		}

		else if (primaryCode >= -36 && primaryCode <= -10) {
			if (primaryCode == -20 && kshiftpressed == false) {

				kshiftpressed = true;
				s = "special,shift,press";

			} else if (primaryCode == -20 && kshiftpressed == true) {
				kshiftpressed = false;
				s = "special,shift,release";

			}

			else if (primaryCode == -21 && ctrlpressed == false) {

				ctrlpressed = true;
				s = "special,ctrl,press";

			} else if (primaryCode == -21 && ctrlpressed == true) {
				ctrlpressed = false;

				s = "special,ctrl,release";

			} else if (primaryCode == -30 && capspressed == false) {
				capspressed = true;
				s = "special,caps,press";

			} else if (primaryCode == -30 && capspressed == true) {
				capspressed = false;

				s = "special,caps,release";

			} else if (primaryCode == -31 && altpressed == false) {
				altpressed = true;
				s = "special,alt,press";

			} else if (primaryCode == -31 && altpressed == true) {
				altpressed = false;

				s = "special,alt,release";

			} else {
				s = "special,nor," + primaryCode;
			}

			pressKey(primaryCode);
		} else
			pressKey(primaryCode);

		if (s != null)
			obj.send(s);

	}

	public void pressKey(int code) {
		long eventTime = System.currentTimeMillis();

		KeyEvent event = new KeyEvent(eventTime, eventTime,
				KeyEvent.ACTION_DOWN, code, 0, 0, 0, 0,
				KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE);
		mTargetActivity.dispatchKeyEvent(event);

	}

	public void releaseKey(int code) {
		long eventTime = System.currentTimeMillis();

		KeyEvent event = new KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP,
				code, 0, 0, 0, 0, KeyEvent.FLAG_SOFT_KEYBOARD
						| KeyEvent.FLAG_KEEP_TOUCH_MODE);
		mTargetActivity.dispatchKeyEvent(event);
	}

}
