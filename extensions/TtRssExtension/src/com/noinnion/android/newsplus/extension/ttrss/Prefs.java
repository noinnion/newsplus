package com.noinnion.android.newsplus.extension.ttrss;

import android.content.Context;
import android.content.SharedPreferences;

import com.noinnion.android.reader.api.ExtensionPrefs;

public class Prefs extends ExtensionPrefs {

	// tiny tiny rss
	public static final String	KEY_SERVER						= "server";
	public static final String	KEY_USERNAME					= "username";
	public static final String	KEY_PASSWORD					= "password";
	public static final String	KEY_HTTP_USERNAME				= "http_username";
	public static final String	KEY_HTTP_PASSWORD				= "http_password";

	public static final String	KEY_SESSION_ID					= "session_id";
	public static final String	KEY_API_LEVEL					= "api_level";
	public static final String	KEY_SINCE_ID					= "since_id";

	public static final String	KEY_LOGGED_IN					= "logged_in";

	public static boolean isLoggedIn(Context c) {
		return getBoolean(c, KEY_LOGGED_IN, false);
	}

	public static void setLoggedIn(Context c, boolean value) {
		putBoolean(c, KEY_LOGGED_IN, value);
	}

	public static void removeLoginData(Context c) {
		SharedPreferences sp = getPrefs(c);
		SharedPreferences.Editor editor = sp.edit();
		editor.remove(KEY_SINCE_ID);
		editor.commit();
	}

	public static String getServer(Context c) {
		return getString(c, KEY_SERVER);
	}

	public static void setServer(Context c, String server) {
		putString(c, KEY_SERVER, server);
	}

	public static String getUsername(Context c) {
		return getString(c, KEY_USERNAME);
	}

	public static void setUsername(Context c, String value) {
		putString(c, KEY_USERNAME, value);
	}

	public static String getPassword(Context c) {
		return getString(c, KEY_PASSWORD);
	}

	public static void setPassword(Context c, String value) {
		putString(c, KEY_PASSWORD, value);
	}

	public static String getHttpUsername(Context c) {
		return getString(c, KEY_HTTP_USERNAME);
	}

	public static void setHttpUsername(Context c, String value) {
		putString(c, KEY_HTTP_USERNAME, value);
	}

	public static String getHttpPassword(Context c) {
		return getString(c, KEY_HTTP_PASSWORD);
	}

	public static void setHttpPassword(Context c, String value) {
		putString(c, KEY_HTTP_PASSWORD, value);
	}

	public static String getSessionId(Context c) {
		return getString(c, KEY_SESSION_ID);
	}

	public static void setSessionId(Context c, String sessionId) {
		putString(c, KEY_SESSION_ID, sessionId);
	}

	public static boolean hasSessionId(Context c) {
		return getSessionId(c) != null;
	}


	public static int getApiLevel(Context c) {
		return getInt(c, KEY_API_LEVEL, 0);
	}

	public static void setApiLevel(Context c, int api) {
		putInt(c, KEY_API_LEVEL, api);
	}

	public static void removeSessionId(Context c) {
		removeKey(c, KEY_SESSION_ID);
	}

	public static int getSinceId(Context c) {
		return getInt(c, KEY_SINCE_ID, 0);
	}

	public static void setSinceId(Context c, int sessionId) {
		putInt(c, KEY_SINCE_ID, sessionId);
	}


}
