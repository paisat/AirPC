package com.spai.remotedesktop;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.Keyboard.Key;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import com.sonyericsson.zoom.DynamicZoomControl;
import com.sonyericsson.zoom.ImageZoomView;
import com.sonyericsson.zoom.LongPressZoomListener;

public class RemoteDesktopTestActivity extends Activity {
	/** Called when the activity is first created. */

	public static String IP_ADDRESS = null;
	Runnable run;
	SharedPreferences setpref;
	Timer resend;
	private ImageZoomView mZoomView;
	private DynamicZoomControl mZoomControl;
	private Bitmap mBitmap;
	private LongPressZoomListener mZoomListener;
	private boolean canReceiveFrame = true;
	private connect obj;
	private FrameReceiver receiver;
	private congestionControl control;

	Keyboard customkeyboard;
	keyboardview customview;
	boolean shiftPressed;
	boolean kshiftpressed;
	boolean ctrlpressed;
	boolean altpressed;
	boolean capspressed;
	boolean twofingerscroll = false;
	boolean notification;
	int scrollspeed;
	int requestCode = 1;
	int notificationcode = 2;
	Timer clearTimer;
	final String prefname = "airpcprefs";
	SharedPreferences pref;
	SharedPreferences.Editor editor;
	LinearLayout inputLayout;
	keyboardactionlistener listener;
	int keyboardType;
	LinearLayout container;
	boolean isKeyboardShown = false;
	AlertDialog quitDialog;
	boolean settingsCalled = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		Display display = getWindowManager().getDefaultDisplay();

		mZoomControl = new DynamicZoomControl();
		mZoomView = (ImageZoomView) findViewById(R.id.zoomview);
		container = (LinearLayout) findViewById(R.id.container);
		customview = (keyboardview) findViewById(R.id.keyboard_view);
		mBitmap = BitmapFactory.decodeResource(getResources(),
				R.drawable.connecting);
		Bundle extras = getIntent().getExtras();
		IP_ADDRESS = extras.getString("ipaddress");
		obj = new connect();
		obj.connectserver(IP_ADDRESS);

		mZoomListener = new LongPressZoomListener(getApplicationContext(),
				display, mZoomView, obj);
		mZoomListener.setZoomControl(mZoomControl);
		mZoomView.setZoomState(mZoomControl.getZoomState());
		mZoomView.setImage(mBitmap);
		mZoomView.setOnTouchListener(mZoomListener);
		mZoomControl.setAspectQuotient(mZoomView.getAspectQuotient());
		resetZoomState();

		setpref = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		pref = getSharedPreferences(prefname, 0);
		editor = pref.edit();
		refreshKeyboardState();

		receiver = new FrameReceiver();

		receiver.execute(IP_ADDRESS, mZoomView, mZoomControl);
		resend = new Timer();
		resend.schedule(new reSend(), 50, 50);

	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);

		mZoomListener.zoomView = mZoomView;

	}

	class congestionControl extends AsyncTask<Void, Void, Void> {

		protected Void doInBackground(Void... voids) {

			receiver.send();

			return null;
		}

	}

	public class reSend extends TimerTask {
		public void run() {

			if (control == null
					|| (System.currentTimeMillis() - receiver.lastTimeSent) > 1000
					&& !(control.getStatus() == AsyncTask.Status.RUNNING)) {

				control = new congestionControl();
				control.execute();

			}
		}
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuItem keyboard = menu.add(0, 1, Menu.NONE, "Keyboard");
		keyboard.setIcon(R.drawable.keyboard);
		MenuItem settings = menu.add(0, 2, Menu.NONE, "Settings");
		settings.setIcon(R.drawable.settings);

		return true;
	}

	public void refreshKeyboardState() {

		capspressed = pref.getBoolean("capspressed", false);
		kshiftpressed = pref.getBoolean("kshiftpressed", false);
		altpressed = pref.getBoolean("altpressed", false);
		ctrlpressed = pref.getBoolean("ctrlpressed", false);
		shiftPressed = pref.getBoolean("shiftpressed", false);
		Log.v("shift presses", kshiftpressed + "");
		keyboardType = pref.getInt("keyboardType", R.xml.keyboard);
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		if (item.getItemId() == 1) {

			showKeyboard();

		} else if (item.getItemId() == 2) {

			Intent i = new Intent();
			i.setClass(getApplicationContext(), Settings.class);
			RemoteDesktopTestActivity.this.startActivity(i);
		}

		return true;
	}

	public void showKeyboard() {

		refreshKeyboardState();
		Bundle extra = getIntent().getExtras();

		/*
		 * if (extra.containsKey("not")) { keyboardType = R.xml.special; }
		 */

		createKeyboard(keyboardType);
	}

	public void saveKeyboardState() {

		altpressed = listener.altpressed;
		capspressed = listener.capspressed;
		kshiftpressed = listener.kshiftpressed;
		ctrlpressed = listener.ctrlpressed;
		/*
		 * if (notification) { if (capspressed || altpressed || kshiftpressed ||
		 * ctrlpressed) //createNotification(); }
		 */

	}

	public void createKeyboard(int id) {

		isKeyboardShown = true;

		keyboardType = id;

		customkeyboard = new Keyboard(this, id);

		boolean state[] = new boolean[5];
		state[0] = shiftPressed;
		state[1] = kshiftpressed;
		state[2] = ctrlpressed;
		state[3] = altpressed;
		state[4] = capspressed;

		customview.setKeyboard(customkeyboard);

		customview.setVisibility(View.VISIBLE);

		customview.setPreviewEnabled(true);

		if (id == R.xml.special) {

			setStickyKeys();
		}

		Log.v("inside create", true + "");

		customview
				.setOnKeyboardActionListener(listener = new keyboardactionlistener(
						RemoteDesktopTestActivity.this, customkeyboard,
						customview, state, obj));

		if (id == R.xml.keyboard) {
			if (shiftPressed) {

				customkeyboard.setShifted(true);
				customview.invalidateAllKeys();
				listener.pressKey(59);
			}
		}

		container.post(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub

				Display display = getWindowManager().getDefaultDisplay();

				int myheight = display.getHeight();
				Rect rectgle = new Rect();
				Window window = getWindow();
				window.getDecorView().getWindowVisibleDisplayFrame(rectgle);
				int StatusBarHeight = rectgle.top;
				myheight = myheight - StatusBarHeight;
				LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
						LinearLayout.LayoutParams.FILL_PARENT, myheight
								- customkeyboard.getHeight());

				mZoomView.setLayoutParams(param);
			}
		});

	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {

		if (keyCode != KeyEvent.KEYCODE_BACK
				&& keyCode != KeyEvent.KEYCODE_MENU
				&& (keyCode == -20 || keyCode == -21 || keyCode == -30 || keyCode == -31)) {

			saveKeyboardState();

		}

		if (keyCode == -2) {

			createKeyboard(R.xml.numbers);

			return true;
		} else if (keyCode == -3) {

			createKeyboard(R.xml.special);

			return true;
		} else if (keyCode == -4) {

			createKeyboard(R.xml.keyboard);
			return true;

		}

		else if (keyCode == -5) {

			createKeyboard(R.xml.symbols);
			return true;
		}

		else if (keyCode == KeyEvent.KEYCODE_BACK
				&& customview.getVisibility() == View.VISIBLE) {

			customview.setVisibility(View.GONE);

			LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.FILL_PARENT,
					LinearLayout.LayoutParams.FILL_PARENT);
			mZoomView.setLayoutParams(param);
			saveKeyState();

			isKeyboardShown = false;

			return true;
		} else if (keyCode == KeyEvent.KEYCODE_BACK && !isKeyboardShown) {
			quitDialog = new AlertDialog.Builder(this).create();

			quitDialog.setButton("Yes", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub

					finish();

				}
			});

			quitDialog.setButton2("No", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub

					quitDialog.cancel();

				}
			});

			quitDialog.setTitle("Close Connection");
			quitDialog
					.setMessage("Are you sure you want to close connection ?");
			quitDialog.show();

		} else if (keyCode == KeyEvent.KEYCODE_HOME) {
			Log.v("home", "true");
			finish();
		} else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
			obj.send("scroll," + (-1 - setpref.getInt("ScrollSensitivity", 2)));
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
			obj.send("scroll," + (+1 + setpref.getInt("ScrollSensitivity", 2)));
			return true;
		}

		return super.onKeyDown(keyCode, event);

	}

	public void saveKeyState() {
		editor.putBoolean("altpressed", altpressed);
		editor.putBoolean("capspressed", capspressed);
		editor.putBoolean("ctrlpressed", ctrlpressed);
		editor.putBoolean("kshiftpressed", kshiftpressed);
		editor.putInt("keyboardType", keyboardType);

		editor.commit();

	}

	public void setStickyKeys() {
		List<Key> lst = new ArrayList<Keyboard.Key>();
		lst = customkeyboard.getKeys();
		Keyboard.Key key;

		if (kshiftpressed) {
			key = lst.get(10);
			key.onReleased(false);
		}

		if (ctrlpressed) {
			key = lst.get(11);
			key.onReleased(false);

		}

		if (capspressed) {
			key = lst.get(17);
			key.onReleased(false);
		}
		if (altpressed) {
			key = lst.get(18);
			key.onReleased(false);

		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		Log.v("config", isKeyboardShown + "");

		if (isKeyboardShown) {
			customview.setVisibility(View.GONE);
			showKeyboard();
		}

	}

	private void resetZoomState() {
		mZoomControl.getZoomState().setPanX(0.5f);
		mZoomControl.getZoomState().setPanY(0.5f);
		mZoomControl.getZoomState().setZoom(1f);
		mZoomControl.getZoomState().notifyObservers();
	}

	private void cleanUp() {
		Thread cleanUp = new Thread(new Runnable() {

			@Override
			public void run() {

				// TODO Auto-generated method stub

				try {
					mBitmap.recycle();
					receiver.canReceiveFrame = false;
					mZoomView.setOnTouchListener(null);
					mZoomControl.getZoomState().deleteObservers();
					obj.setHandShake(IP_ADDRESS);
					obj.handshakeMessage("airPcClose");
					obj.destroy();

					resend.cancel();
					// receiver.quit();
					// frameSocket.close();
					// congestionSocket.close();

				} catch (Exception e) {
				}

			}
		});

		cleanUp.start();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();

		resend = new Timer();
		resend.schedule(new reSend(), 50, 50);
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		Log.v("onPause", "true");

		if (resend != null) {
			resend.cancel();

		}

	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();

		finish();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		cleanUp();

	}

	// -------------------------------

}
