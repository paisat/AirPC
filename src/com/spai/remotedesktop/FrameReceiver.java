package com.spai.remotedesktop;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.zip.GZIPInputStream;
import org.apache.commons.io.IOUtils;
import com.sonyericsson.zoom.DynamicZoomControl;
import com.sonyericsson.zoom.ImageZoomView;

import android.R.bool;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

public class FrameReceiver extends AsyncTask<Object, Bitmap, Void> {

	private DatagramSocket frameSocket;
	private DatagramSocket congestionSocket;
	private int framePort = 6550;
	private int sendPort = 6551;
	public long lastTimeSent = 0;
	private ImageZoomView zoomView;
	private DynamicZoomControl mZoomControl;
	private String IP_ADDRESS = null;
	private static int HEADER_SIZE = 8;
	private static int SESSION_START = 1;
	private static int DATAGRAM_MAX_SIZE = 65507;
	volatile public Boolean canReceiveFrame = true;

	public synchronized void send() {

		String s = "send";

		try {

			DatagramPacket p = new DatagramPacket(s.getBytes(),
					s.getBytes().length, InetAddress.getByName(IP_ADDRESS),
					sendPort);
			congestionSocket.send(p);
		} catch (Exception e) {

			Log.e("FrameReceiver:send", "send", e);

		}

	}

	public void quit() {
		canReceiveFrame = false;

	}

	@Override
	protected void onPreExecute() {

		try {

			Log.v("FrameReceiver:onPreExecute", "inside");

			frameSocket = new DatagramSocket(framePort);
			congestionSocket = new DatagramSocket();
		} catch (SocketException e) {

			Log.v("FrameReceiver:onPreExecute", e.getMessage());

		}

	}

	@Override
	protected Void doInBackground(Object... objects) {

		try {

			IP_ADDRESS = (String) objects[0];
			zoomView = (ImageZoomView) objects[1];
			mZoomControl = (DynamicZoomControl) objects[2];

			Log.v("FrameReceiver:doInBackground", "inside");

			int currentSession = -1;
			int slicesStored = 0;
			int[] slicesCol = null;
			byte[] imageData = null;
			boolean sessionAvailable = false;

			byte[] buffer = new byte[DATAGRAM_MAX_SIZE];
			lastTimeSent = System.currentTimeMillis();

			while (canReceiveFrame) {

				send();
				Log.v("Remotedesktop", "inside");
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

					// ByteArrayOutputStream out = new ByteArrayOutputStream();
					// IOUtils.copy(new GZIPInputStream(new
					// ByteArrayInputStream(
					// imageData)), out);

					GZIPInputStream in = new GZIPInputStream(
							new ByteArrayInputStream(imageData));

					imageData = IOUtils.toByteArray(in);

					in.close();

					Bitmap bp = BitmapFactory.decodeByteArray(imageData, 0,
							imageData.length);

					publishProgress(bp);

				}

			}

		} catch (Exception e) {

			Log.v("Frame Receiver", e.getMessage());
			Log.e("error", "frameReceiver", e);

		}

		return null;
	}

	@Override
	protected void onCancelled() {

		Log.v("Frame Receiver  ", "inside onCancelled");

	}

	@Override
	protected void onProgressUpdate(Bitmap... frame) {
		super.onProgressUpdate();

		Log.v("Frame Receiver", "inside progress update");

		Bitmap display = frame[0];

		zoomView.setImage(display);

		mZoomControl.getZoomState().notifyObservers();

	}

	@Override
	protected void onPostExecute(Void result) {

		frameSocket.close();
		congestionSocket.close();

		Log.v("Frame Receiver", "inside post execute update");

	}

}
