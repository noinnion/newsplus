package com.noinnion.android.newsplus.extension.hackernews;

import java.io.IOException;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.noinnion.android.newsplus.extension.hackernews.util.AndroidUtils;
import com.noinnion.android.newsplus.extension.hackernews.R;
import com.noinnion.android.reader.api.ReaderException;
import com.noinnion.android.reader.api.ReaderExtension;

public class LoginActivity extends FragmentActivity implements OnClickListener 
{

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final Context c = getApplicationContext();

		String action = getIntent().getAction();
		if (action != null && action.equals(ReaderExtension.ACTION_LOGOUT)) {
			logout();
		}
		else
		{
			setResult(RESULT_OK);
			finish();
		}
	}

	private void logout() {
		final Context c = getApplicationContext();
		Prefs.setLoggedIn(c, false);
		Prefs.removeLoginData(c);
		setResult(ReaderExtension.RESULT_LOGOUT);
		finish();
	}

	@Override
	public void onClick(View v) 
	{
	}
}
