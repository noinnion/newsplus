package com.noinnion.android.newsplus.extension.inoreader;

import android.content.Context;
import android.content.SharedPreferences;

import com.noinnion.android.reader.api.ExtensionPrefs;

public class Prefs extends ExtensionPrefs {

//	public static final int		PREF_LOGIN_NONE				= 0;
//	public static final int		PREF_LOGIN_CLIENT			= 1;
//	public static final int		PREF_LOGIN_OAUTH			= 2;
//	public static final int		PREF_LOGIN_ACCOUNT			= 3;

//	public static final String	KEY_LOGIN_METHOD			= "login_method";
	public static final String	KEY_USER_ID					= "user_id";

	public static final String	KEY_OAUTH2_TOKEN_REFRESH	= "oauth2_token_refresh";
	public static final String	KEY_GOOGLE_ID				= "google_login_id";
	public static final String	KEY_GOOGLE_PASSWD			= "google_passwd";

	public static final String	KEY_GOOGLE_AUTH				= "google_auth";
	public static final String	KEY_GOOGLE_AUTH_TIME		= "google_auth_time";
	
	public static final String	KEY_LOGGED_IN				= "logged_in";
	
	public static boolean isLoggedIn(Context c) {
		return getBoolean(c, KEY_LOGGED_IN, false);
	}

	public static void setLoggedIn(Context c, boolean value) {
		putBoolean(c, KEY_LOGGED_IN, value);
	}
	
	public static String getUserId(Context c) {
		return getString(c, KEY_USER_ID);
	}

	public static void setUserId(Context c, String userId) {
		putString(c, KEY_USER_ID, userId);
	}

	public static void removeLoginData(Context c) {
		SharedPreferences sp = getPrefs(c);
		SharedPreferences.Editor editor = sp.edit();
		editor.remove(KEY_USER_ID);
		editor.remove(KEY_GOOGLE_AUTH);
		editor.remove(KEY_GOOGLE_AUTH_TIME);
		editor.commit();
	}

	public static String getGoogleId(Context c) {
		return getString(c, KEY_GOOGLE_ID);
	}

	public static String getGooglePasswd(Context c) {
		return getString(c, KEY_GOOGLE_PASSWD);
	}

	public static void setGoogleIdPasswd(Context c, String googleId, String googlePasswd) {
		SharedPreferences sp = getPrefs(c);
		SharedPreferences.Editor editor = sp.edit();
		editor.putString(KEY_GOOGLE_ID, googleId);
		editor.putString(KEY_GOOGLE_PASSWD, googlePasswd);
		editor.commit();
	}

	public static void setGoogleAuth(Context c, String authToken) {
		SharedPreferences sp = getPrefs(c);
		SharedPreferences.Editor editor = sp.edit();
		editor.putString(KEY_GOOGLE_AUTH, authToken);
		editor.putLong(KEY_GOOGLE_AUTH_TIME, System.currentTimeMillis());
		editor.commit();
	}

	public static String getGoogleAuth(Context c) {
		return getString(c, KEY_GOOGLE_AUTH);
	}

	public static long getGoogleAuthTime(Context c) {
		return getLong(c, KEY_GOOGLE_AUTH_TIME, 0);
	}

}
