/*
 * Copyright (c) 2010, Sony Ericsson Mobile Communication AB. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, 
 * are permitted provided that the following conditions are met:
 *
 *    * Redistributions of source code must retain the above copyright notice, this 
 *      list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above copyright notice,
 *      this list of conditions and the following disclaimer in the documentation
 *      and/or other materials provided with the distribution.
 *    * Neither the name of the Sony Ericsson Mobile Communication AB nor the names
 *      of its contributors may be used to endorse or promote products derived from
 *      this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.sonyericsson.zoom;


import java.net.DatagramSocket;


import com.spai.remotedesktop.connect;

import android.content.Context;
import android.os.Vibrator;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;

/**
 * Listener for controlling zoom state through touch events
 */
public class LongPressZoomListener implements View.OnTouchListener {

	/**
	 * Enum defining listener modes. Before the view is touched the listener is
	 * in the UNDEFINED mode. Once touch starts it can enter either one of the
	 * other two modes: If the user scrolls over the view the listener will
	 * enter PAN mode, if the user lets his finger rest and makes a longpress
	 * the listener will enter ZOOM mode.
	 */
	private enum Mode {
		UNDEFINED, PAN, ZOOM
	}

	/** Time of tactile feedback vibration when entering zoom mode */
	private static final long VIBRATE_TIME = 50;

	/** Current listener mode */
	private Mode mMode = Mode.UNDEFINED;

	/** Zoom control to manipulate */
	private DynamicZoomControl mZoomControl;

	public ImageZoomView zoomView;

	/** X-coordinate of previously handled touch event */
	private float mX;

	/** Y-coordinate of previously handled touch event */
	private float mY;

	/** X-coordinate of latest down event */
	private float mDownX;

	/** Y-coordinate of latest down event */
	private float mDownY;

	/** Velocity tracker for touch events */
	private VelocityTracker mVelocityTracker;

	/** Distance touch can wander before we think it's scrolling */
	private final int mScaledTouchSlop;

	/** Duration in ms before a press turns into a long press */
	private final int mLongPressTimeout;

	/** Vibrator for tactile feedback */
	private final Vibrator mVibrator;

	/** Maximum velocity for fling */
	private final int mScaledMaximumFlingVelocity;

	DatagramSocket socket;

	float xRatio = 0;
	float yRatio = 0;

	Display disp;

	connect conObj;
	GestureDetector gd;

	/**
	 * Creates a new instance
	 * 
	 * @param context
	 *            Application context
	 */
	public LongPressZoomListener(Context context, Display display,
			ImageZoomView view,connect obj) {
		mLongPressTimeout = ViewConfiguration.getLongPressTimeout();
		mScaledTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
		mScaledMaximumFlingVelocity = ViewConfiguration.get(context)
				.getScaledMaximumFlingVelocity();
		mVibrator = (Vibrator) context.getSystemService("vibrator");

		zoomView = view;
		xRatio = 1920 / display.getWidth();
		yRatio = 1080 / (display.getHeight() - 70);

		disp = display;
		conObj=obj;
		try {
			socket = new DatagramSocket();
		} catch (Exception e) {

		}
		gd = new GestureDetector(new gestureListener());

	}

	/**
	 * Sets the zoom control to manipulate
	 * 
	 * @param control
	 *            Zoom control
	 */
	public void setZoomControl(DynamicZoomControl control) {
		mZoomControl = control;

	}

	/**
	 * Runnable that enters zoom mode
	 */
	private final Runnable mLongPressRunnable = new Runnable() {
		public void run() {
			mMode = Mode.ZOOM;
			mVibrator.vibrate(VIBRATE_TIME);
		}
	};

	// implements View.OnTouchListener
	public boolean onTouch(View v, MotionEvent event) {
		final int action = event.getAction();
		final float x = event.getX();
		final float y = event.getY();

		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain();
		}
		mVelocityTracker.addMovement(event);

		switch (action) {
		case MotionEvent.ACTION_DOWN:
			
			
				mZoomControl.stopFling();
				Log.v("press", mZoomControl.getZoomState().getPanX() + " "
						+ mZoomControl.getZoomState().getPanY() + " "
						+ mZoomControl.getZoomState().getZoom());
				v.postDelayed(mLongPressRunnable, mLongPressTimeout);
	
				mDownX = x;
				mDownY = y;
				mX = x;
				mY = y;
			
			break;

		case MotionEvent.ACTION_MOVE:
			if(event.getPointerCount()==1)
		{
			final float dx = (x - mX) / v.getWidth();
			final float dy = (y - mY) / v.getHeight();

			if (mMode == Mode.ZOOM) {
				mZoomControl.zoom((float) Math.pow(20, -dy),
						mDownX / v.getWidth(), mDownY / v.getHeight());
			} else if (mMode == Mode.PAN) {
				mZoomControl.pan(-dx, -dy);
			} else {
				final float scrollX = mDownX - x;
				final float scrollY = mDownY - y;

				final float dist = (float) Math.sqrt(scrollX * scrollX
						+ scrollY * scrollY);

				if (dist >= mScaledTouchSlop) {
					v.removeCallbacks(mLongPressRunnable);
					mMode = Mode.PAN;
				}
			}

			mX = x;
			mY = y;
			break;
		}
			
		case MotionEvent.ACTION_POINTER_2_DOWN:
			conObj.send("clk,right,single");
			
			break;
			
		case MotionEvent.ACTION_UP:
			if (mMode == Mode.PAN) {
				mVelocityTracker.computeCurrentVelocity(1000,
						mScaledMaximumFlingVelocity);
				mZoomControl.startFling(
						-mVelocityTracker.getXVelocity() / v.getWidth(),
						-mVelocityTracker.getYVelocity() / v.getHeight());
			} else {
				mZoomControl.startFling(0, 0);
			}
			mVelocityTracker.recycle();
			mVelocityTracker = null;
			v.removeCallbacks(mLongPressRunnable);
			mMode = Mode.UNDEFINED;
			break;

		default:
			mVelocityTracker.recycle();
			mVelocityTracker = null;
			v.removeCallbacks(mLongPressRunnable);
			mMode = Mode.UNDEFINED;
			break;

		}

		if (gd.onTouchEvent(event))
			return false;

		return true;
	}

	public class gestureListener extends
			GestureDetector.SimpleOnGestureListener {

		float x = 0;
		float y = 0;

		float length = 0;
		float breadth = 0;
		
		float lastTouchY=0;
		
		String mesg=null;

		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {

			
			if(e.getPointerCount()==2)
			{
				Log.v("mouse right click ","true");
			}

				getMouse(e);
				

				mesg = "moveClk,single," + ((int) (x)) + "," + ((int) (y));

				conObj.send(mesg);
			return true;
		}

		@Override
		public boolean onDoubleTap(MotionEvent e) {
			
				mMode=Mode.UNDEFINED;
			
			
			getMouse(e);
			
			mesg="moveClk,double,"+((int) (x)) + "," + ((int) (y));
			conObj.send(mesg);
			
			return true;
			
		}
		

		private void getMouse(MotionEvent e) {
			breadth = zoomView.mRectSrc.width();
			length = zoomView.mRectSrc.height();

			xRatio = breadth / zoomView.viewWidth;
			yRatio = length / zoomView.viewHeight;

			x = e.getX() * xRatio + zoomView.mRectSrc.left;
			y = yRatio * e.getY() + zoomView.mRectSrc.top;
		}

		
	}

}
