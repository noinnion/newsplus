package com.noinnion.android.newsplus.extension.feedbin;

import java.io.IOException;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.noinnion.android.newsplus.extension.feedbin.R;
import com.noinnion.android.newsplus.extension.feedbin.util.AndroidUtils;
import com.noinnion.android.reader.api.ReaderException;
import com.noinnion.android.reader.api.ReaderExtension;

public class LoginActivity extends FragmentActivity implements OnClickListener {

	protected ProgressDialog	mBusy;

	private TextView			mLoginIdText;
	private TextView			mPasswordText;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final Context c = getApplicationContext();

		String action = getIntent().getAction();
		if (action != null && action.equals(ReaderExtension.ACTION_LOGOUT)) {
			logout();
		}

		if (Prefs.isLoggedIn(c)) {
			setResult(RESULT_OK);
			finish();
		}
		setContentView(R.layout.login_feedbin);
		setTitle(R.string.txt_login);

		mLoginIdText = (TextView) findViewById(R.id.edit_login_id);
		mPasswordText = (TextView) findViewById(R.id.edit_password);
		
		String user = Prefs.getUser(c);
		if (user != null) mLoginIdText.setText(user);

		findViewById(R.id.btn_login).setOnClickListener(this);
		findViewById(R.id.btn_cancel).setOnClickListener(this);
	}

	private void startHome(boolean login) {
		setResult(login ? ReaderExtension.RESULT_LOGIN : RESULT_OK);
		finish();
	}

	private void logout() {
		final Context c = getApplicationContext();
		Prefs.setLoggedIn(c, false);
		Prefs.removeLoginData(c);
		setResult(ReaderExtension.RESULT_LOGOUT);
		finish();
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.btn_login:
				String user = mLoginIdText.getText().toString();
				String password = mPasswordText.getText().toString();

				if (user.length() == 0 || password.length() == 0) {
					AndroidUtils.showToast(this, getText(R.string.msg_login_fail));
				} else {
					new SaveInputLoginTask().execute(user, password);
				}
				break;
			case R.id.btn_cancel:
				finish();
				break;
		}
	}

	private void processLogin() {	
		final Context c = getApplicationContext();
		Prefs.setLoggedIn(c, true);
		startHome(true);
	}

	private class SaveInputLoginTask extends AsyncTask<String, Void, Boolean> {

		protected void onPreExecute()
		{
			mBusy = ProgressDialog.show(LoginActivity.this, null, getText(R.string.msg_login_running), true, true);
		}

		protected Boolean doInBackground(String... params)
		{
			String user = params[0];
			String password = params[1];

			final Context c = getApplicationContext();
			Prefs.setUserPasswd(c, user, null);
			FeedBinClient client=new FeedBinClient(c);
			try  
			{
				if(client.login(user,password,true))
				{
					Prefs.setUserPasswd(c, user, password);
					return true;
				} 
				else 
				{
					AndroidUtils.showToast(LoginActivity.this, getText(R.string.msg_login_fail));
				}
			} catch (IOException e) {
				e.printStackTrace();
				AndroidUtils.showToast(LoginActivity.this, getText(R.string.err_io) + " (" + e.getLocalizedMessage() + ")");
			} catch (ReaderException e) {
				e.printStackTrace();
				AndroidUtils.showToast(LoginActivity.this, getText(R.string.msg_login_fail));
			}catch (Throwable e) {
				e.printStackTrace();
				AndroidUtils.showToast(LoginActivity.this, e.getLocalizedMessage());
			}
			return null;
		}

		protected void onPostExecute(Boolean result) {
			final Context c = getApplicationContext();
			try {
				if (mBusy != null && mBusy.isShowing()) mBusy.dismiss();
			} catch (Exception e) { }
			if (result != null) processLogin();
			else Prefs.setLoggedIn(c, false);
		}
	}

}
