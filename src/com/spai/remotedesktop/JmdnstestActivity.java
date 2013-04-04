package com.spai.remotedesktop;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class JmdnstestActivity extends Activity {
	/** Called when the activity is first created. */

	WifiManager manager;
	WifiManager.MulticastLock lock = null;
	JmDNS jmdns = null;
	String servicetext = null;
	ServiceListener listener;
	List<String> servers;
	Thread service = null;
	ListView server;
	boolean listempty = true;
	BroadcastReceiver wifiStatus;
	String NetType = "_test._tcp.local.";
	customList serverList;
	boolean isWifiOn = false;
	Timer timeoutTimer = null;
	Boolean timeout = false;
	Button connect;
	SharedPreferences prefs, setpref;
	final String prefname = "airpcprefs";
	AlertDialog dialog;
	connect obj;
	EditText ipadd;
	boolean resumeHasRun = false;
	Thread refresh = null;
	boolean addedOrRemoved = false;
	ProgressDialog progressDialog;
	Dialog passwdDialog;
	EditText passwordField;
	String ipaddress = null;
	Button ok;
	Button cancel;

	private static final String IPADDRESS_PATTERN = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
			+ "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
			+ "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
			+ "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

	Handler td;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.jmdns);

		setpref = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());

		dialog = new AlertDialog.Builder(this).create();
		dialog.setButton("OK", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub

			}
		});

		server = (ListView) findViewById(R.id.serverlist);
		manager = (WifiManager) getSystemService(WIFI_SERVICE);
		connect = (Button) findViewById(R.id.connect);
		servers = new ArrayList<String>();
		ipadd = (EditText) findViewById(R.id.ipadd);
		obj = new connect();

		serverList = new customList(this, servers);
		listStatus();
		server.setAdapter(serverList);

		td = new Handler() {

			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);

				if (msg.what == 0) {
					listStatus();
					serverList.notifyDataSetChanged();
				}

				if (msg.arg1 == 1 && timeoutTimer != null) {
					Log.v("inside", "timeouttimer");
					timeoutTimer.cancel();
					timeoutTimer = null;
					listStatus();
					serverList.notifyDataSetChanged();
				}

				else if (msg.arg1 == 2) {
					dialog.setTitle("Error");
					dialog.setMessage("Could not connect to Server");
					dialog.show();
					listStatus();
					serverList.notifyDataSetChanged();
				}

				else if (msg.arg1 == 3) {
					passwordDialog();
				}

				else if (msg.arg1 == 4) {
					dialog.setTitle("Password");
					dialog.setMessage("Wrong Password");
					dialog.show();
				} else if (msg.arg1 == 5) {
					dialog.setTitle("Error");
					dialog.setMessage("No Server Found !!");
					dialog.show();
				}
				else if(msg.arg1==6)
				{
					dialog.setTitle("Alert");
					dialog.setMessage("The PC is already used by another Mobile.");
					dialog.show();
				}

			}

		};

		wifiStatus = new BroadcastReceiver() {

			@Override
			public void onReceive(Context arg0, Intent arg1) {

				NetworkInfo netInfo = (NetworkInfo) arg1
						.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);

				if (netInfo.getType() == ConnectivityManager.TYPE_WIFI) {

					if (manager.isWifiEnabled()) {

						WifiInfo conInfo = manager.getConnectionInfo();

						if (conInfo.getIpAddress() != 0) {
							Log.v("inside wifui", true + "");
							isWifiOn = true;

							startService();

						} else {
							isWifiOn = false;
							td.sendEmptyMessage(0);
						}

					} else {
						clean();
						isWifiOn = false;

					}

				}

			}
		};

		this.registerReceiver(wifiStatus, new IntentFilter(
				ConnectivityManager.CONNECTIVITY_ACTION));

		server.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> a, View v, int i, long id) {

				Log.v("list empty", listempty + "");
				if (!listempty) {

					Dialog("Connecting To Server");
					final long ID = id;
					Thread connect = new Thread(new Runnable() {

						@Override
						public void run() {
							// TODO Auto-generated method stub
							ServiceInfo info = null;

							progressDialog.show();
							info = jmdns.getServiceInfo(NetType,
									servers.get((int) ID), true, 5000);

							if (info != null) {

								ipaddress = info.getInetAddress().toString()
										.replace("/", "");

								boolean result = false;

								// if(!ipaddress.equals(prevIpaddress))
								result = obj.setHandShake(ipaddress);

								if (result) {

									int res = obj.handshakeMessage("airPc");
									Log.v("passResult", res + "");
									if (res == 1) {
										progressDialog.dismiss();
										Message msg = new Message();
										msg.arg1 = 2;
										td.sendMessage(msg);
									} else if (res == 0) {
										progressDialog.dismiss();
										Message msg = new Message();
										msg.arg1 = 3;
										td.sendMessage(msg);
									}
									
									else if(res==4)
									{
										progressDialog.dismiss();
										Message msg=new Message();
										msg.arg1=6;
										td.sendMessage(msg);
										
									}
									
								} else {
									progressDialog.dismiss();
									Message msg = new Message();
									msg.arg1 = 2;
									td.sendMessage(msg);
								}

								

							} else {
								progressDialog.dismiss();
								Message msg = new Message();
								msg.arg1 = 2;
								td.sendMessage(msg);

							}
						}
					});
					connect.start();

				}

			}
		});

		connect.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub

				dialog.setButton("OK", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub

					}
				});

				if (manager.isWifiEnabled() || true) {
					WifiInfo info = manager.getConnectionInfo();
					int ip = info.getIpAddress();
					if (ip == 0) {
						dialog.setTitle("Error");
						dialog.setMessage("Please select a active router");
						dialog.show();
					} else {
						ipaddress = ipadd.getText().toString();
						Log.v("ipaddress", ipaddress);
						if (ipaddress == null || ipaddress.equals("")) {
							dialog.setTitle("Error");
							dialog.setMessage("Ip Address cannot be null !");
							dialog.show();

						} else {
							if (ipaddress.matches(IPADDRESS_PATTERN)) {

								Dialog("Connecting to Server");

								Thread passwordVerify = new Thread(
										new Runnable() {

											@Override
											public void run() {
												

												if (obj.setHandShake(ipaddress)) {
													
													int res = obj
															.handshakeMessage("airPc");
													Log.v("passResult", res
															+ "");
													if (res == 1) {
														progressDialog
																.dismiss();
														Message msg = new Message();
														msg.arg1 = 2;
														td.sendMessage(msg);
													} else if (res == 0) {
														progressDialog
																.dismiss();
														Message msg = new Message();
														msg.arg1 = 3;
														td.sendMessage(msg);
													}
													else if(res==4)
													{
														progressDialog.dismiss();
														Message msg=new Message();
														msg.arg1=6;
														td.sendMessage(msg);
														
													}

												} else {
													progressDialog.dismiss();
													Message msg = new Message();
													msg.arg1 = 5;
													td.sendMessage(msg);

												}
											}
										});
								
								passwordVerify.start();

							}

							else {
								dialog.setTitle("Error");
								dialog.setMessage("Invalid ip address");
								dialog.show();
							}
						}
					}
				} else {
					/*dialog.setTitle("Error");
					dialog.setMessage("WIFI Switched off . Turn on WIFI");
					dialog.show();*/
				}

			}
		});

	}

	public void passwordDialog() {
		passwdDialog = new Dialog(JmdnstestActivity.this);
		passwdDialog.setContentView(R.layout.passworddialog);
		passwdDialog.setTitle("Enter Password");
		passwdDialog.setCancelable(true);
		ok = (Button) passwdDialog.findViewById(R.id.OK);
		cancel = (Button) passwdDialog.findViewById(R.id.Cancel);
		passwordField = (EditText) passwdDialog
				.findViewById(R.id.passwordField);

		ok.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub

				try {
					String passwd = passwordField.getText().toString();
					MessageDigest md = MessageDigest.getInstance("MD5");
					md.reset();
					md.update(passwd.getBytes());
					byte[] digest = md.digest();
					BigInteger bigInt = new BigInteger(1, digest);
					final String passwdMD5 = bigInt.toString(16);

					Dialog("Verifying Password");

					Thread passVerify = new Thread(new Runnable() {

						@Override
						public void run() {
							// TODO Auto-generated method stub

							obj.setHandShake(ipaddress);
							int res = obj.handshakeMessage("airPcPassword,"
									+ passwdMD5);
							Log.v("password dialog message", res + "");

							if (res == 2) {
								progressDialog.dismiss();
								Log.v("password", "accepted");
								passwdDialog.cancel();
								
								 setDefault(); Intent inte = new Intent();
								 inte.setClass(getApplicationContext(),
								 RemoteDesktopTestActivity.class); 
								 inte.putExtra("ipaddress", ipaddress);
								 JmdnstestActivity.this.startActivity(inte);
								 
								
							} else if (res == 3) {
								progressDialog.dismiss();
								passwdDialog.cancel();
								Message msg = new Message();
								msg.arg1 = 4;
								td.sendMessage(msg);

							} else if (res == 1) {
								progressDialog.dismiss();
								passwdDialog.cancel();
								Message msg = new Message();
								msg.arg1 = 2;
								td.sendMessage(msg);
							}

							obj.destroy();
						}
					});

					passVerify.start();

				} catch (NoSuchAlgorithmException e) {

				}
			}
		});

		cancel.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				passwdDialog.cancel();
				obj.destroy();

			}
		});

		passwdDialog.show();
	}

	private void Dialog(String msg) {
		progressDialog = ProgressDialog.show(JmdnstestActivity.this,
				"Connecting", msg);
	}

	private void startService() {
		clean();
		// clearJmdnsService();
		service = new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub

				setup();

			}
		});

		service.start();
	}

	private void runTimer() {
		timeout = false;
		timeoutTimer = new Timer();
		timeoutTimer.schedule(new timeOut(), 15000);

	}

	private void restart() {
		if (manager.isWifiEnabled()) {

			startService();

			Log.v("inside ", "added or removed ");

		} else {
			td.sendEmptyMessage(0);
		}
	}

	class timeOut extends TimerTask {
		public void run() {
			if (isWifiOn && listempty && !timeout) {
				timeout = true;
				Message m = new Message();
				m.arg1 = 1;
				td.sendMessage(m);
			}

		}
	}

	private void setup() {

		if (lock == null) {
			lock = manager.createMulticastLock("jmdnstest");
			lock.setReferenceCounted(true);

		}
		lock.acquire();

		if (jmdns == null) {

			try

			{
				jmdns = JmDNS
						.create(InetAddress.getByName(getLocalIpAddress()));
				jmdns.registerServiceType(NetType);

				jmdns.addServiceListener(NetType,
						listener = new ServiceListener() {

							@Override
							public void serviceResolved(ServiceEvent arg0) {
								// TODO Auto-generated method stub
								Log.v("Service resolved", "true");

							}

							@Override
							public void serviceRemoved(ServiceEvent arg0) {
								// TODO Auto-generated method stub
								// update(arg0);
								Log.v("Service removed", "true");
								update();

							}

							@Override
							public void serviceAdded(ServiceEvent arg0) {
								// TODO Auto-generated method stub

								if (timeoutTimer != null)
									timeoutTimer.cancel();

								update();

							}
						});

			} catch (Exception e) {

			}
		}

	}

	public void update() {

		ServiceInfo[] array = jmdns.list(NetType);
		if (array.length != 0) {

			servers.clear();

			listempty = false;
			for (int i = 0; i < array.length; i++) {
				servers.add(array[i].getName());

			}

		} else {

			listempty = true;
			timeout = false;

		}

		td.sendEmptyMessage(0);

	}

	public void setDefault() {

		prefs = getSharedPreferences(prefname, 0);
		SharedPreferences.Editor editor = prefs.edit();

		editor.putBoolean("kshiftpressed", false);
		editor.putBoolean("ctrlpressed", false);
		editor.putBoolean("altpressed", false);
		editor.putBoolean("capspressed", false);
		editor.putBoolean("shiftpressed", false);
		editor.putInt("keyboardType", R.xml.keyboard);
		editor.commit();
	}

	public class customList extends ArrayAdapter<String> {
		public List<String> serverNames;
		public Activity context;

		public customList(Activity context, List<String> names) {
			super(context, R.layout.listview, names);
			serverNames = names;
			this.context = context;

		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			int imgSource = 0;
			LayoutInflater listLayout = context.getLayoutInflater();
			View listRow = listLayout.inflate(R.layout.listview, null, true);
			ImageView img = (ImageView) listRow.findViewById(R.id.img);
			TextView name = (TextView) listRow.findViewById(R.id.title);
			TextView desc = (TextView) listRow.findViewById(R.id.desc);
			ProgressBar pbar = (ProgressBar) listRow.findViewById(R.id.pbar);
			name.setText(serverNames.get(position));

			pbar.setVisibility(View.GONE);
			desc.setVisibility(View.GONE);

			if (!isWifiOn) {
				imgSource = R.drawable.nowifi;
				desc.setText("Switch On Wifi");
				desc.setVisibility(View.VISIBLE);
			}

			else if (isWifiOn && listempty && timeout) {

				imgSource = R.drawable.noserver;
				desc.setText("Run Server in PC");
				desc.setVisibility(View.VISIBLE);

			}

			else if (isWifiOn && listempty && !timeout) {
				imgSource = R.drawable.search;
				pbar.setVisibility(View.VISIBLE);

			}

			else if (isWifiOn && !listempty) {
				imgSource = R.drawable.server;
			}

			img.setImageResource(imgSource);

			return listRow;

		}
	}

	public void listStatus() {
		if (!isWifiOn) {

			servers.clear();
			servers.add("Wifi Off");

		}

		if (listempty && isWifiOn && !timeout) {

			servers.clear();
			timeout = false;
			runTimer();

			servers.add("Searching For Servers");
		}
		if (listempty && isWifiOn && timeout) {
			servers.clear();
			servers.add("No Servers Found");

		}

	}

	public void clean() {

		if (jmdns != null) {

			jmdns.removeServiceListener(NetType, listener);
			jmdns.unregisterAllServices();
			jmdns = null;
		}

		if (refresh != null) {
			// refresh.stop();
			refresh = null;
		}

		if (service != null) {
			// service.stop();
			service = null;
		}

		listempty = true;
		timeout = false;
		servers.clear();
		td.sendEmptyMessage(0);
		if (timeoutTimer != null) {
			timeoutTimer.cancel();
			timeoutTimer = null;
		}

	}

	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuItem settings = menu.add(0, 1, Menu.NONE, "Settings");
		settings.setIcon(R.drawable.settings);

		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		if (item.getItemId() == 1) {
			Intent i = new Intent();
			i.setClass(getApplicationContext(), Settings.class);
			JmdnstestActivity.this.startActivity(i);
		}

		return true;
	}

	@Override
	public void onResume() {
		super.onResume();
		if (!resumeHasRun) {
			resumeHasRun = true;
			return;
		}

		if (setpref.getBoolean("orientation", false)) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		} else {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
		}
		restart();
		Log.v("resume", "resume");
	}

	@Override
	public void onPause() {

		super.onPause();
		// clean();

		Log.v("paused", "paused");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		clean();
		// clearJmdnsService();
		if (lock != null) {
			lock.release();
			lock = null;
		}

		unregisterReceiver(wifiStatus);

	}

	public String getLocalIpAddress() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface
					.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf
						.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()) {
						return inetAddress.getHostAddress().toString();
					}
				}
			}
		} catch (SocketException ex) {

		}
		return null;
	}

}