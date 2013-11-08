package com.noinnion.android.newsplus.extension.ttrss;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;

import com.noinnion.android.newsplus.extension.ttrss.utils.AndroidUtils;
import com.noinnion.android.newsplus.extension.ttrss.utils.Utils;
import com.noinnion.android.reader.api.ReaderExtension;

public class LoginActivity extends Activity implements OnClickListener {

	public static final String	TAG	= "TtRssLoginActivity";

	protected ProgressDialog	mBusy;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setResult(RESULT_CANCELED);

		setContentView(R.layout.login_ttrss);

		final Context c = getApplicationContext();

		String action = getIntent().getAction();
		if (action != null && action.equals(ReaderExtension.ACTION_LOGOUT)) {
			logout();
		}

		if (Prefs.isLoggedIn(c)) {
			startHome(false);
		}

		findViewById(R.id.btn_cancel).setOnClickListener(this);
		findViewById(R.id.btn_login).setOnClickListener(this);

		EditText serverText = ((EditText) findViewById(R.id.server));
		String server = Prefs.getServer(c);
		if (!TextUtils.isEmpty(server)) serverText.setText(server);
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

	private void login() {
		String server = ((EditText) findViewById(R.id.server)).getText().toString();
		String user = ((EditText) findViewById(R.id.username)).getText().toString();
		String pass = ((EditText) findViewById(R.id.password)).getText().toString();

		String httpUser = ((EditText) findViewById(R.id.http_username)).getText().toString();
		String httpPass = ((EditText) findViewById(R.id.http_password)).getText().toString();

		if (user != null && user.length() > 0 && pass != null && pass.length() > 0 && server != null && server.length() > 0 && server.startsWith("http")) {
			if (!server.endsWith("/")) server += "/";
			login(user, pass, server, httpUser, httpPass);
		} else AndroidUtils.showToast(getApplicationContext(), getText(R.string.ttrss_err_missing));
	}

	private void login(String username, String password, String server, String httpUser, String httpPass) {
		new LoginTask(username, password, server, httpUser, httpPass).execute();
	}

	class LoginData {

		String	server;
		String	username;
		String	password;
		String	httpUsername;
		String	httpPassword;
		String	sessionId;
		int		apiLevel	= 0;

	}

	private class LoginTask extends AsyncTask<Void, Void, LoginData> {

		private String	server;
		private String	username;
		private String	password;
		private String	httpUsername;
		private String	httpPassword;
		private String	error;

		public LoginTask(String name, String pass, String server, String httpUser, String httpPass) {
			this.username = name;
			this.password = pass;
			this.server = server;
			this.httpUsername = httpUser;
			this.httpPassword = httpPass;
		}

		@Override
		protected void onPreExecute() {
			mBusy = ProgressDialog.show(LoginActivity.this, null, getText(R.string.msg_login_running), true, true);
		}

		@Override
		protected LoginData doInBackground(Void... args) {
			try {
				HttpPost post = new HttpPost(server + TtRssExtension.URL_API_);

				if (!TextUtils.isEmpty(httpUsername) && !TextUtils.isEmpty(httpPassword)) {
					UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(httpUsername, httpPassword);
					BasicScheme scheme = new BasicScheme();
					Header authorizationHeader = scheme.authenticate(credentials, post);
					post.addHeader(authorizationHeader);
				}

				JSONObject jo = new JSONObject();
				jo.put("op", "login");
				jo.put("user", username);
				jo.put("password", password);

				StringEntity postEntity = new StringEntity(jo.toString(), HTTP.UTF_8);
				postEntity.setContentType("application/json");
				post.setEntity(postEntity);

				DefaultHttpClient client = TtRssExtension.createHttpClient();
				HttpResponse response = client.execute(post);

				HttpEntity responseEntity = response.getEntity();
				String content = Utils.convertStreamToString(responseEntity.getContent());

				JSONObject resJsonObject = new JSONObject(content);
				if (resJsonObject != null) {
					JSONObject contentObject = resJsonObject.getJSONObject("content");
					if (contentObject != null) {
						// handle error
						if (contentObject.has("error")) {
							error = contentObject.getString("error");
							if (TextUtils.isEmpty(error)) return null;
						}

						if (contentObject.has("session_id")) {
							String sessionId = contentObject.getString("session_id");

							LoginData loginData = new LoginData();
							loginData.server = server;
							loginData.sessionId = sessionId;

							if (contentObject.has("api_level")) {
								loginData.apiLevel = contentObject.getInt("api_level");
							}

							loginData.username = username;
							loginData.password = password;

							loginData.httpUsername = httpUsername;
							loginData.httpPassword = httpPassword;

							return loginData;
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			return null;
		}

		@Override
		protected void onPostExecute(LoginData result) {
			try {
				if (mBusy != null && mBusy.isShowing()) mBusy.dismiss();
			} catch (Exception e) {}

			processLogin(result, error);
		}
	}

	private void processLogin(LoginData data, String error) {
		final Context c = getApplicationContext();
		if (!TextUtils.isEmpty(error)) {
			AndroidUtils.showToast(c, getErrorMessage(error));
			return;
		}

		if (data == null || data.sessionId == null) {
			AndroidUtils.showToast(c, R.string.ttrss_err_fail);
			return;
		}

		Prefs.setServer(c, data.server);
		Prefs.setSessionId(c, data.sessionId);
		if (data.apiLevel > 0) Prefs.setApiLevel(c, data.apiLevel);

		Prefs.setUsername(c, data.username);
		Prefs.setPassword(c, data.password);

		Prefs.setHttpUsername(c, data.httpUsername);
		Prefs.setHttpPassword(c, data.httpPassword);

		Prefs.removeLoginData(c);

		Prefs.setLoggedIn(c, true);
		startHome(true);
	}

	private int getErrorMessage(String error) {
		if (error == null) return R.string.ttrss_error_unknown;
		else if (error.equals("HTTP_UNAUTHORIZED")) return R.string.ttrss_error_http_unauthorized;
		else if (error.equals("HTTP_FORBIDDEN")) return R.string.ttrss_error_http_forbidden;
		else if (error.equals("HTTP_NOT_FOUND")) return R.string.ttrss_error_http_not_found;
		else if (error.equals("HTTP_SERVER_ERROR")) return R.string.ttrss_error_http_server_error;
		else if (error.equals("HTTP_OTHER_ERROR")) return R.string.ttrss_error_http_other_error;
		else if (error.equals("SSL_REJECTED")) return R.string.ttrss_error_ssl_rejected;
		else if (error.equals("SSL_HOSTNAME_REJECTED")) return R.string.ttrss_error_ssl_hostname_rejected;
		else if (error.equals("PARSE_ERROR")) return R.string.ttrss_error_parse_error;
		else if (error.equals("IO_ERROR")) return R.string.ttrss_error_io_error;
		else if (error.equals("OTHER_ERROR")) return R.string.ttrss_error_other_error;
		else if (error.equals("API_DISABLED")) return R.string.ttrss_error_api_disabled;
		else if (error.equals("API_UNKNOWN")) return R.string.ttrss_error_api_unknown;
		else if (error.equals("API_UNKNOWN_METHOD")) return R.string.ttrss_error_api_unknown_method;
		else if (error.equals("LOGIN_FAILED")) return R.string.ttrss_error_login_failed;
		else if (error.equals("INVALID_URL")) return R.string.ttrss_error_invalid_api_url;
		else if (error.equals("API_INCORRECT_USAGE")) return R.string.ttrss_error_api_incorrect_usage;
		else if (error.equals("NETWORK_UNAVAILABLE")) return R.string.ttrss_error_network_unavailable;
		else {
			Log.e(TAG, "getErrorMessage: unknown error code=" + error);
			return R.string.ttrss_error_unknown;
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.btn_login:
				login();
				break;
			case R.id.btn_cancel:
				finish();
				break;
		}
	}

}
