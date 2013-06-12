package com.noinnion.android.newsplus.extension.google_reader;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;

public class OAuthLoginFragment extends DialogFragment {

	public static final String		EXTRA_LOGIN_TYPE				= "loginType";

	// oauth
	public static final int			OAUTH_LOGIN_UNKNOWN				= 0;
	public static final int			OAUTH_LOGIN_GOOGLE_READER		= 1;
	public static final int			OAUTH_LOGIN_FEEDLY				= 2;

	private WebView	bodyView;
	private View	progressBar;

	private String	mUrl;
	private String	mTitle;
	private int		mType = OAUTH_LOGIN_UNKNOWN;
	
	private String	mRedirectUri;
	
	
    public interface OAuthDialogListener {
        void onFinishOAuthLogin(String callback);
    }
	
	public OAuthLoginFragment() {
	}	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setStyle(STYLE_NO_TITLE, 0);
		
		Bundle bundle = getArguments();
		mTitle = bundle.getString(LoginActivity.EXTRA_TITLE);			
		if (TextUtils.isEmpty(mTitle)) mTitle = getString(R.string.login_authentication);
			
		mUrl = bundle.getString(LoginActivity.EXTRA_URL);			
		if (mUrl == null) dismiss();
		
		mType = bundle.getInt(EXTRA_LOGIN_TYPE, OAUTH_LOGIN_UNKNOWN);			
		if (mType == OAUTH_LOGIN_UNKNOWN) dismiss();
		else {
			switch (mType) {
				case OAUTH_LOGIN_GOOGLE_READER:
					mRedirectUri = GoogleReaderAPI.OAUTH2_REDIRECT_URI;
					break;
			}
		}
	}	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.oauth_activity, container);
		
		((TextView) view.findViewById(R.id.title)).setText(mTitle);
		((ImageView) view.findViewById(R.id.icon)).setImageResource(R.drawable.icon);

		progressBar = view.findViewById(R.id.progress_bar);
		
//		CookieSyncManager.createInstance(getActivity()); 
//	    CookieManager cookieManager = CookieManager.getInstance();
//	    cookieManager.removeAllCookie();		
		
		bodyView = (WebView) view.findViewById(R.id.body);		
		
//		setResult(RESULT_CANCELED);
		
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		bodyView.setWebViewClient(new BodyWebViewClient());
		
		WebSettings settings = bodyView.getSettings();
		settings.setJavaScriptEnabled(true);
//		settings.setUseWideViewPort(true);
		settings.setSaveFormData(false);
		settings.setSavePassword(false);
		settings.setDomStorageEnabled(true);
		
		bodyView.loadUrl(mUrl);
	}	
	
	private void onCallBack(String url) {
		LoginActivity activity = (LoginActivity) getActivity();
        activity.onFinishOAuthLogin(url);
        this.dismiss();
	}
	
	private class BodyWebViewClient extends WebViewClient {

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, final String url) {
			if (url.startsWith(mRedirectUri)) {
				onCallBack(url);
			} 
			if (url.startsWith("mailto:") || url.startsWith("about:blank")) {
				
			}
			else view.loadUrl(url);
			return true;
		}

		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			progressBar.setVisibility(View.VISIBLE);
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			progressBar.setVisibility(View.GONE);
		}
	}

	@Override
	public void onResume() {
		try {
			Class.forName("android.webkit.WebView").getMethod("onResume", (Class[]) null).invoke(bodyView, (Object[]) null);
			bodyView.resumeTimers();
		} catch (Exception e) {}
		super.onPause();
	}
	
	@Override
	public void onPause() {
		try {
			Class.forName("android.webkit.WebView").getMethod("onPause", (Class[]) null).invoke(bodyView, (Object[]) null);
			bodyView.pauseTimers();
		} catch (Exception e) {}
		super.onPause();
	}
	
	@Override
	public void onDestroy() {
		bodyView.clearCache(true);
		bodyView.destroy();
		super.onDestroy();
	}

}
