package com.noinnion.android.newsplus.extension.inoreader;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.noinnion.android.newsplus.extension.inoreader.util.AndroidUtils;
import com.noinnion.android.reader.api.util.Utils;

public class WelcomeActivity extends SherlockActivity implements OnClickListener {

	public static final String	TAG						= "WelcomeActivity";

	public static final String	NEWSPLUS_PACKAGE		= "com.noinnion.android.newsplus";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.welcome);
		initButton();
		
		String version = AndroidUtils.getVersionName(getApplicationContext());
		getSupportActionBar().setSubtitle(version);		
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.btn_ok:
				if (TextUtils.isEmpty(mAppPackage)) {
					Utils.startMarketApp(this, NEWSPLUS_PACKAGE);
				} else {
					Utils.startAppPackage(this, mAppPackage);
					finish();
				}
				break;
			case R.id.changelog:
			case R.id.rate:
				Utils.startMarketApp(this, getPackageName());
				break;
		}
	}

	private String	mAppPackage	= null;

	public void initButton() {
		boolean installed = Utils.appInstalledOrNot(this, NEWSPLUS_PACKAGE);
		if (installed) mAppPackage = NEWSPLUS_PACKAGE;

		Button button = (Button) findViewById(R.id.btn_ok);
		button.setText(installed ? R.string.txt_start_app : R.string.txt_download_app);
		button.setEnabled(true);
		button.setOnClickListener(this);
	
		findViewById(R.id.changelog).setOnClickListener(this);
		findViewById(R.id.rate).setOnClickListener(this);	
		
		CheckBox hideIcon = (CheckBox)findViewById(R.id.hide_icon);

		PackageManager p = getPackageManager();
		Intent intent = new Intent(WelcomeActivity.this, WelcomeActivity.class);
		ComponentName component = intent.getComponent();
		int settings = p.getComponentEnabledSetting(component);
		hideIcon.setChecked(settings == PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
		
		hideIcon.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				PackageManager p = getPackageManager();
				Intent intent = new Intent(WelcomeActivity.this, WelcomeActivity.class);
				ComponentName component = intent.getComponent();
				if (isChecked) {
					p.setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
				} else {
					p.setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, PackageManager.DONT_KILL_APP);					
				}
			}
		});	
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.welcome, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_feedback:
				Intent intent = new Intent(this, SendLogActivity.class);
				intent.putExtra(SendLogActivity.EXTRA_SEND_LOG, true);
				startActivity(intent);
				return true;
		}
		return false;
	}
	
	
}
