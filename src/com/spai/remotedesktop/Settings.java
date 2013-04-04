package com.spai.remotedesktop;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.KeyEvent;

public class Settings extends PreferenceActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preference);
	}
	
	public boolean onKeyDown(int keyCode,KeyEvent event)
	{
		if(keyCode==KeyEvent.KEYCODE_BACK);
		finish();
		
		return super.onKeyDown(keyCode, event);
	}
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		
	}

}
