package com.noinnion.android.newsplus.extension.cnn;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

import com.noinnion.android.reader.api.ReaderExtension;

public class LoginActivity extends Activity implements OnClickListener {
	
	protected ProgressDialog	mBusy;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setResult(RESULT_CANCELED);
		
		final Context c = getApplicationContext();
		
		String action = getIntent().getAction();
		if (action != null && action.equals(ReaderExtension.ACTION_LOGOUT)) {
			logout();
		}
		
		if (Prefs.isLoggedIn(c)) {
			setResult(RESULT_OK);
			finish();
		}		
		
		setContentView(R.layout.login_cnn);
		
		findViewById(R.id.ok_button).setOnClickListener(this);
	}

	private void logout() {
		final Context c = getApplicationContext();
		Prefs.setLoggedIn(c, false);
		setResult(ReaderExtension.RESULT_LOGOUT);
		finish();
	}
	
	private void login() {
		final Context c = getApplicationContext();
		Prefs.setLoggedIn(c, true);
		setResult(ReaderExtension.RESULT_LOGIN);
		finish();
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.ok_button:
				login();
				break;
		}
	}
	
	
}
