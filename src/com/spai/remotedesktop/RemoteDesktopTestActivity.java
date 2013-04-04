package com.spai.remotedesktop;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.IOUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.Keyboard.Key;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.sonyericsson.zoom.DynamicZoomControl;
import com.sonyericsson.zoom.ImageZoomView;
import com.sonyericsson.zoom.LongPressZoomListener;

public class RemoteDesktopTestActivity extends Activity {
	/** Called when the activity is first created. */
	

	private static int HEADER_SIZE = 8;
	private static int SESSION_START=1;	
	private static int DATAGRAM_MAX_SIZE = 65507;
	public static String IP_ADDRESS = null;
	public static int PORT = 6550;
	private int sendPort = 6551;
	private long lastTimeSent = 0;
	private DatagramSocket frameSocket;
	private DatagramSocket congestionSocket;
	Handler hand;
	Bitmap bp = null;
	Runnable run;
	SharedPreferences setpref;
	Timer resend;

	private ImageZoomView mZoomView;
	private DynamicZoomControl mZoomControl;
	private Bitmap mBitmap;
	private LongPressZoomListener mZoomListener;
	private boolean canReceiveFrame = true;
	private connect obj;

	
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
	boolean settingsCalled=false;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		Display display = getWindowManager().getDefaultDisplay();

		mZoomControl = new DynamicZoomControl();
		mZoomView = (ImageZoomView) findViewById(R.id.zoomview);
		container=(LinearLayout)findViewById(R.id.container);
		customview = (keyboardview) findViewById(R.id.keyboard_view);
		mBitmap = BitmapFactory.decodeResource(getResources(),
				R.drawable.connecting);
		Bundle extras = getIntent().getExtras();
		IP_ADDRESS = extras.getString("ipaddress");
		obj=new connect();
		obj.connectserver(IP_ADDRESS);
		hand=new Handler();

		mZoomListener = new LongPressZoomListener(getApplicationContext(),
				display, mZoomView,obj);
		mZoomListener.setZoomControl(mZoomControl);
		mZoomView.setZoomState(mZoomControl.getZoomState());
		mZoomView.setImage(mBitmap);
		mZoomView.setOnTouchListener(mZoomListener);
		mZoomControl.setAspectQuotient(mZoomView.getAspectQuotient());
		resetZoomState();

		resend = new Timer();
		resend.schedule(new reSend(), 50,50);
		
		
		
		setpref=PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		pref = getSharedPreferences(prefname, 0);
		editor = pref.edit();
		refreshKeyboardState();

		
		try {

			congestionSocket = new DatagramSocket();

			frameSocket = new DatagramSocket(PORT);

		}

		catch (Exception e) {

		}

		Thread frameThread = new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub

				receiveFrame();
			}
		});

		frameThread.start();
		
		

	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);

		mZoomListener.zoomView = mZoomView;

	}

	public class reSend extends TimerTask {
		public void run() {

			if ((System.currentTimeMillis() - lastTimeSent) > 1000) {

				send();
			}

		}
	}

	public void receiveFrame() {

		try {

			int currentSession = -1;
			int slicesStored = 0;
			int[] slicesCol = null;
			byte[] imageData = null;
			boolean sessionAvailable = false;

			
			byte[] buffer = new byte[DATAGRAM_MAX_SIZE];
			lastTimeSent=System.currentTimeMillis();

			while (canReceiveFrame) {
				send();
				Log.v("Remotedesktop","inside");
				lastTimeSent = System.currentTimeMillis();

				DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
				frameSocket.receive(dp);
				
				byte[] data = dp.getData();
				short session = (short) (data[1] & 0xff);
				short slices = (short) (data[2] & 0xff);
				int maxPacketSize = (int) ((data[3] & 0xff) << 8 | (data[4] & 0xff)); // mask
				short slice = (short) (data[5] & 0xff);
				int size = (int) ((data[6] & 0xff) << 8 | (data[7] & 0xff)); // mask

				if ((data[0]) == SESSION_START) {
					if (session != currentSession) {
						currentSession = session;
						slicesStored = 0;
						imageData = new byte[slices * maxPacketSize];
						slicesCol = new int[slices];
						sessionAvailable = true;

					}
				}

				if (sessionAvailable && session == currentSession) {
					if (slicesCol != null && slicesCol[slice] == 0) {
						slicesCol[slice] = 1;
						System.arraycopy(data, HEADER_SIZE, imageData, slice
								* maxPacketSize, size);
						slicesStored++;
						
					}
				}

				if (slicesStored == slices) {

					
					ByteArrayOutputStream out=new ByteArrayOutputStream();
					IOUtils.copy(new GZIPInputStream(new ByteArrayInputStream(imageData)),out);
					
					imageData=out.toByteArray();

					bp = BitmapFactory.decodeByteArray(imageData, 0,
							imageData.length);
					hand.removeCallbacks(run);
					hand.post(run);

					mZoomView.post(new Runnable() {

						@Override
						public void run() {
							// TODO Auto-generated method stub

							if (bp != null)
								mZoomView.setImage(bp);
							
							mZoomControl.getZoomState().notifyObservers();
						
						}
					});
					

				}

			}
		} catch (Exception e) {

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

	void send() {
		String s = "send";
		//Log.v("length",s.getBytes().length+"");
		try {

			DatagramPacket p = new DatagramPacket(s.getBytes(),
					s.getBytes().length, InetAddress.getByName(IP_ADDRESS),
					sendPort);
			congestionSocket.send(p);
		} catch (Exception e) {

		}
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

		/*if (extra.containsKey("not")) {
			keyboardType = R.xml.special;
		}*/

		createKeyboard(keyboardType);
	}
	
	public void saveKeyboardState() {

		altpressed = listener.altpressed;
		capspressed = listener.capspressed;
		kshiftpressed = listener.kshiftpressed;
		ctrlpressed = listener.ctrlpressed;
		/*if (notification) {
			if (capspressed || altpressed || kshiftpressed || ctrlpressed)
				//createNotification();
		}*/

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
						RemoteDesktopTestActivity.this, customkeyboard, customview,
						state, obj));

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
		}
		else if (keyCode==KeyEvent.KEYCODE_BACK&&!isKeyboardShown)
		{
			 quitDialog=new AlertDialog.Builder(this).create();
			 
			 
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
			quitDialog.setMessage("Are you sure you want to close connection ?");
			quitDialog.show();
			
		}
		else if(keyCode==KeyEvent.KEYCODE_HOME)
		{
			Log.v("home","true");
			finish();
		}
		else if(keyCode==KeyEvent.KEYCODE_VOLUME_UP)
		{
			obj.send("scroll,"+(-1-setpref.getInt("ScrollSensitivity",2)));
			return true;
		}
		else if(keyCode==KeyEvent.KEYCODE_VOLUME_DOWN)
		{
			obj.send("scroll,"+(+1+setpref.getInt("ScrollSensitivity",2)));
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

	/*public void createNotification() {

		String msg = "pressed";

		NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		Notification not = new Notification(R.drawable.touchpad, "Notification",
				System.currentTimeMillis());
		Intent notIntent = new Intent(this, AirmouseActivity.class);

		if (ctrlpressed)
			msg = "Ctrl," + msg;
		if (altpressed)
			msg = "Alt," + msg;
		if (kshiftpressed)
			msg = "Shift," + msg;
		if (capspressed)
			msg = "Caps," + msg;

		not.flags = Notification.FLAG_AUTO_CANCEL;

		notIntent.putExtra("not", "hello");
		notIntent.putExtra("ipaddress", IPaddress);

		PendingIntent intent = PendingIntent.getActivity(
				getApplicationContext(), requestCode, notIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		not.setLatestEventInfo(getApplicationContext(), "Alert!", msg, intent);
		manager.notify(notificationcode, not);

	}*/
	
	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		
		Log.v("config",isKeyboardShown+"");
		
		if(isKeyboardShown)
		{
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
	
	private void cleanUp()
	{
		Thread cleanUp=new Thread(new Runnable() {
			
			@Override
			public void run() {
			
				// TODO Auto-generated method stub
				
				try {
					mBitmap.recycle();
					canReceiveFrame = false;
					mZoomView.setOnTouchListener(null);
					mZoomControl.getZoomState().deleteObservers();
					obj.setHandShake(IP_ADDRESS);
					obj.handshakeMessage("airPcClose");
					obj.destroy();
					
					frameSocket.close();
					congestionSocket.close();
					resend.cancel();

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
		
		resend=new Timer();
		resend.schedule(new reSend(),50,50);
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		Log.v("onPause", "true");
		
		if(resend!=null)
		{
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
