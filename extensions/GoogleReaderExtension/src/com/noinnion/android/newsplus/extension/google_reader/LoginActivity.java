package com.noinnion.android.newsplus.extension.google_reader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.noinnion.android.newsplus.extension.google_reader.OAuthLoginFragment.OAuthDialogListener;
import com.noinnion.android.newsplus.extension.google_reader.util.AndroidUtils;
import com.noinnion.android.newsplus.extension.google_reader.util.HttpUtils;
import com.noinnion.android.newsplus.extension.google_reader.util.UrlUtils;
import com.noinnion.android.newsplus.extension.google_reader.util.Utils;
import com.noinnion.android.newsplus.extension.google_reader.util.compat.AccountManagerHelper;
import com.noinnion.android.reader.api.ReaderException;
import com.noinnion.android.reader.api.ReaderException.ReaderLoginException;
import com.noinnion.android.reader.api.ReaderExtension;

public class LoginActivity extends FragmentActivity implements OnClickListener, OAuthDialogListener {

	public static final int		REQUEST_OAUTH	= 1;

	public static final String	EXTRA_URL								= "url";
	public static final String	EXTRA_TITLE								= "title";


	private final Handler		mHandler		= new Handler();
	protected ProgressDialog	mBusy;

	private String				mGoogleId		= null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final Context c = getApplicationContext();

		String action = getIntent().getAction();
		if (action != null && action.equals(ReaderExtension.ACTION_LOGOUT)) {
			logout();
		}

		int loginMethod = Prefs.getLoginMethod(c);		
		if (loginMethod != Prefs.PREF_LOGIN_NONE) {				
			startHome(false);
		}

		setContentView(R.layout.login_google_reader);
		setTitle(R.string.txt_login);

		initViews();
	}

	private void logout() {
		final Context c = getApplicationContext();
		Prefs.setLoginMethod(c, Prefs.PREF_LOGIN_NONE);
		Prefs.setLastSyncTime(c, 0);
		setResult(ReaderExtension.RESULT_LOGOUT);
		finish();
	}


	private TextView	mLoginIdText;
	private TextView	mPasswordText;

	private void initViews() {
		getAccounts();

		findViewById(R.id.another_container_title).setOnClickListener(this);
		findViewById(R.id.btn_another_method).setOnClickListener(this);

		mLoginIdText = (TextView) findViewById(R.id.edit_login_id);
		mPasswordText = (TextView) findViewById(R.id.edit_password);

		String googleId = Prefs.getGoogleId(getApplicationContext());
		if (googleId != null) mLoginIdText.setText(googleId);

		findViewById(R.id.btn_login).setOnClickListener(this);
		findViewById(R.id.btn_cancel).setOnClickListener(this);
		findViewById(R.id.btn_oauth).setOnClickListener(this);
		findViewById(R.id.btn_getting_started).setOnClickListener(this);
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.btn_getting_started:
				try {
					Intent viewIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://goo.gl/ZE4H6")); // getting started
					startActivity(viewIntent);
				} catch (Exception e) {
					AndroidUtils.showToast(this, e.getLocalizedMessage());
				}
				break;
			case R.id.btn_oauth:
				oauthLogin();
				break;
			case R.id.btn_login:
				String googleId = mLoginIdText.getText().toString();
				String password = mPasswordText.getText().toString();
				if (googleId.length() == 0 || password.length() == 0) {
					AndroidUtils.showToast(this, getText(R.string.msg_login_fail));
				} else {
					new SaveInputLoginTask().execute(googleId, password);
				}
				break;
			case R.id.btn_cancel:
			case R.id.another_container_title:
			case R.id.btn_another_method:
				View anotherAccountButton = findViewById(R.id.btn_another_method);
				View anotherAccountInfo = findViewById(R.id.another_account_info);
				View v = findViewById(R.id.another_container);
				if (v.getVisibility() != View.VISIBLE) {
					anotherAccountButton.setVisibility(View.GONE);
					anotherAccountInfo.setVisibility(View.GONE);
					v.setVisibility(View.VISIBLE);
				} else {
					anotherAccountButton.setVisibility(View.VISIBLE);
					anotherAccountInfo.setVisibility(View.VISIBLE);
					v.setVisibility(View.GONE);
				}
				break;				
		}
	}

	private void getAccounts() {
		try {
			final AccountManager manager = AccountManager.get(this);
			String authToken = Prefs.getGoogleAuth(getApplicationContext());
			Account[] accounts = manager.getAccountsByType(AuthHelper.GOOGLE_ACCOUNT_TYPE);
			manager.invalidateAuthToken(AuthHelper.GOOGLE_ACCOUNT_TYPE, authToken);

			LayoutInflater mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			LinearLayout l = (LinearLayout) findViewById(R.id.account_container);

			int size = accounts.length;
			if (size == 0) throw new Throwable();

			for (int i = 0; i < size; i++) {
				Account account = accounts[i];

				View row = mInflater.inflate(R.layout.login_account_row, null);
				TextView tv = (TextView) row.findViewById(R.id.name);
				tv.setText(account.name);

				row.setTag(account);

				row.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						Account a = (Account) v.getTag();
						authenticateAccount(manager, a);
						// testing
						mGoogleId = a.name;
					}
				});

				l.addView(row);
			}
		} catch (Throwable t) {
			TextView tv = (TextView) findViewById(R.id.login_account_title);
			tv.setText(R.string.login_no_accounts);
			findViewById(R.id.btn_another_method).setVisibility(View.GONE);
			findViewById(R.id.another_container).setVisibility(View.VISIBLE);
		}
	}

	public static final int	REQUEST_AUTHENTICATE	= 0;

	private void authenticateAccount(final AccountManager manager, final Account account) {
		mBusy = ProgressDialog.show(LoginActivity.this, null, getText(R.string.msg_login_running), true, true);

		new Thread() {

			@Override
			public void run() {
				try {
					String authToken = Prefs.getGoogleAuth(getApplicationContext());
					manager.invalidateAuthToken(AuthHelper.GOOGLE_ACCOUNT_TYPE, authToken);

					AccountManagerFuture<Bundle> accountManagerFuture = AccountManagerHelper.getAuthToken(manager, account, AuthHelper.AUTH_TOKEN_TYPE, true, null, null);
					final Bundle bundle = accountManagerFuture.getResult();

					try {
						if (bundle.containsKey(AccountManager.KEY_INTENT)) {
							Intent intent = bundle.getParcelable(AccountManager.KEY_INTENT);
							int flags = intent.getFlags();
							flags &= ~Intent.FLAG_ACTIVITY_NEW_TASK;
							intent.setFlags(flags);
							startActivityForResult(intent, REQUEST_AUTHENTICATE);
						} else if (bundle.containsKey(AccountManager.KEY_AUTHTOKEN) && bundle.containsKey(AccountManager.KEY_ACCOUNT_NAME)) {
							saveAccountManagerLogin(bundle.getString(AccountManager.KEY_ACCOUNT_NAME), bundle.getString(AccountManager.KEY_AUTHTOKEN));
						}
					} catch (Exception e) {
						AndroidUtils.showToast(LoginActivity.this, e.getLocalizedMessage());
					}

				} catch (Exception e) {
					AndroidUtils.showToast(LoginActivity.this, getText(R.string.err_io));
				}

				mHandler.post(new Runnable() {
					public void run() {
						try {
							if (mBusy != null && mBusy.isShowing()) mBusy.dismiss();
						} catch (Exception e) { }
					}
				});

			}
		}.start();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
			case REQUEST_AUTHENTICATE:
				if (resultCode == RESULT_OK) {
					if (mGoogleId != null) {
						AccountManager manager = AccountManager.get(this);
						Account account = new Account(mGoogleId, AuthHelper.GOOGLE_ACCOUNT_TYPE);
						authenticateAccount(manager, account);
					}
				}
				break;
		}
	}

	private void startHome(boolean login) {
		setResult(login ? ReaderExtension.RESULT_LOGIN : RESULT_OK);
		finish();
	}

	private void processLogin() {	
		final Context c = getApplicationContext();
		Prefs.removeUserId(c);
		Prefs.setLastSyncTime(c, 0);
		startHome(true);
	}

	private class SaveInputLoginTask extends AsyncTask<String, Void, Boolean> {

		protected void onPreExecute() {
			mBusy = ProgressDialog.show(LoginActivity.this, null, getText(R.string.msg_login_running), true, true);
		}

		protected Boolean doInBackground(String... params) {
			String googleId = params[0];
			String password = params[1];

			final Context c = getApplicationContext();
			GoogleReaderClient client = new GoogleReaderClient();
			try {
				Prefs.setLoginMethod(c, Prefs.PREF_LOGIN_CLIENT);
				Prefs.setGoogleAuth(c, null);
				if (client.login(googleId, password, true)) {
					Prefs.setGoogleIdPasswd(c, googleId, password);
					return true;
				} else {
					AndroidUtils.showToast(LoginActivity.this, getText(R.string.msg_login_fail));
				}
			} catch (IOException e) {
				AndroidUtils.showToast(LoginActivity.this, getText(R.string.err_io) + " (" + e.getLocalizedMessage() + ")");
			} catch (ReaderException e) {
				AndroidUtils.showToast(LoginActivity.this, getText(R.string.msg_login_fail));
			} catch (Throwable e) {
				AndroidUtils.showToast(LoginActivity.this, e.getLocalizedMessage());
			}

			return null;
		}

		protected void onPostExecute(Boolean result) {
			try {
				if (mBusy != null && mBusy.isShowing()) mBusy.dismiss();
			} catch (Exception e) { }
			if (result != null) processLogin();
			else Prefs.setLoginMethod(getApplicationContext(), Prefs.PREF_LOGIN_NONE);
		}
	}

	private String getOAuth2AuthUrl() {
		StringBuilder builder = new StringBuilder();
		builder.append(GoogleReaderAPI.OAUTH2_AUTH_URL)
		.append("?scope=").append(UrlUtils.encode(GoogleReaderAPI.OAUTH2_SCOPE))
		.append("&redirect_uri=").append(GoogleReaderAPI.OAUTH2_REDIRECT_URI)
		.append("&response_type=code&client_id=").append(GoogleReaderAPI.OAUTH2_CLIENT_ID);
		return builder.toString();
	}	

	private void oauthLogin() {
		FragmentManager fm = getSupportFragmentManager();
		OAuthLoginFragment oauthLogin = new OAuthLoginFragment();

		Bundle bundle = new Bundle();
		bundle.putString(EXTRA_URL, getOAuth2AuthUrl());
		bundle.putString(EXTRA_TITLE, getString(R.string.login_google_authentication));
		bundle.putInt(OAuthLoginFragment.EXTRA_LOGIN_TYPE, OAuthLoginFragment.OAUTH_LOGIN_GOOGLE_READER);

		oauthLogin.setArguments(bundle);
		oauthLogin.show(fm, "fragment_oauth_login");
	}

	@Override
	public void onFinishOAuthLogin(String callback) {
		Uri callbackUrl = Uri.parse(callback);
		if (callbackUrl != null && callbackUrl.toString().startsWith(GoogleReaderAPI.OAUTH2_REDIRECT_URI)) {
			String authCode = callbackUrl.getQueryParameter("code");
			new OAuthLoginTask().execute(authCode);
		}
	}

	private class OAuthLoginTask extends AsyncTask<String, Void, Boolean> {

		protected void onPreExecute() {
			mBusy = ProgressDialog.show(LoginActivity.this, null, getText(R.string.msg_login_running), true, true);
		}

		protected Boolean doInBackground(String... params) {
			String authCode = params[0];

			try {
				if (authCode != null) {
					DefaultHttpClient client = HttpUtils.createHttpClient();

					List<NameValuePair> postParams = new ArrayList<NameValuePair>(5);
					postParams.add(new BasicNameValuePair("code", authCode));
					postParams.add(new BasicNameValuePair("client_id", GoogleReaderAPI.OAUTH2_CLIENT_ID));
					postParams.add(new BasicNameValuePair("client_secret", GoogleReaderAPI.OAUTH2_CLIENT_SECRET));
					postParams.add(new BasicNameValuePair("redirect_uri", GoogleReaderAPI.OAUTH2_REDIRECT_URI));
					postParams.add(new BasicNameValuePair("grant_type", "authorization_code"));

					HttpPost post = new HttpPost(GoogleReaderAPI.OAUTH2_TOKEN_URL);
					post.setEntity(new UrlEncodedFormEntity(postParams, HTTP.UTF_8));
					post.setHeader("Content-Type", "application/x-www-form-urlencoded");

					HttpResponse res = client.execute(post);
					int resStatus = res.getStatusLine().getStatusCode();

					if (resStatus == HttpStatus.SC_FORBIDDEN) { throw new ReaderLoginException("Login failure"); }
					else if (resStatus == HttpStatus.SC_UNAUTHORIZED) { throw new ReaderLoginException("Authentication fails (" + resStatus + ")"); }
					else if (resStatus != HttpStatus.SC_OK) { throw new ReaderException("Invalid http status " + resStatus); }

					final HttpEntity entity = res.getEntity();
					if (entity == null) {
						throw new ReaderException("null response entity");
					}

					String content = Utils.convertStreamToString(entity.getContent());
					JSONObject jsonObject = new JSONObject(content);
					String accessToken = jsonObject.getString("access_token");
					String refreshToken = jsonObject.getString("refresh_token");

					if (accessToken != null && refreshToken != null) {
						final Context c = getApplicationContext();
						Prefs.setOAuth2Tokens(c, accessToken, refreshToken, Prefs.PREF_LOGIN_OAUTH);
						return true;
					}
				}
			} catch (Exception e) {
				AndroidUtils.showToast(LoginActivity.this, getText(R.string.err_io) + " (" + e.getLocalizedMessage() + ")");
			}
			return null;
		}

		protected void onPostExecute(Boolean result) {
			try {
				if (mBusy != null && mBusy.isShowing()) mBusy.dismiss();
			} catch (Exception e) { }
			if (result != null) processLogin(); 
			else Prefs.setLoginMethod(getApplicationContext(), Prefs.PREF_LOGIN_NONE);
		}
	}

	private void saveAccountManagerLogin(final String accountName, final String authToken) {
		try {
			// save login data
			Prefs.setLoginMethod(this, Prefs.PREF_LOGIN_ACCOUNT);
			Prefs.setGoogleIdPasswd(this, accountName, null);
			Prefs.setGoogleAuth(this, authToken);

			mHandler.post(new Runnable() {
				public void run() {
					try {
						if (mBusy != null && mBusy.isShowing()) mBusy.dismiss();
					} catch (Exception e) {}
					processLogin();
				}
			});
		} catch (Exception e) {
			AndroidUtils.showToast(this, getText(R.string.err_io) + " (" + e.getLocalizedMessage() + ")");
		}
	}

}
