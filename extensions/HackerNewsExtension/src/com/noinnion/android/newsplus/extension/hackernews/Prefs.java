package com.noinnion.android.newsplus.extension.hackernews;

import android.content.Context;
import android.content.SharedPreferences;

import com.noinnion.android.reader.api.ExtensionPrefs;

public class Prefs extends ExtensionPrefs {

	public static final String USER="username";
	public static final String PASSWORD="password";
	public static final String AUTH="auth";
	public static final String AUTH_TIME="authtime";	
	
	public static final String	LOGGED_IN				= "logged_in";
	
	public static final String APP_KEY="";
	public static final String APP_SECRET="";
	
	public static final String USER_KEY="userkey";
	public static final String USER_SECRET="usersecret";

	public static boolean isLoggedIn(Context c) {
		return getBoolean(c, LOGGED_IN, false);
	}

	public static void setLoggedIn(Context c, boolean value) {
		putBoolean(c, LOGGED_IN, value);
	}	
	
	public static void removeLoginData(Context c) {
		SharedPreferences sp = getPrefs(c);
		SharedPreferences.Editor editor = sp.edit();
		editor.remove(AUTH);
		editor.remove(AUTH_TIME);
		editor.remove(USER);
		editor.remove(PASSWORD);
		editor.remove(USER_KEY);
		editor.remove(USER_SECRET);
		editor.commit();
	}	
	public static void setUserAuth(Context c,String userkey,String usersecret)
	{
		SharedPreferences sp = getPrefs(c);
		SharedPreferences.Editor editor = sp.edit();
		editor.putString(USER_KEY, userkey);
		editor.putString(USER_SECRET, usersecret);
	}
	public static String getUserKey(Context c)
	{
		return getString(c, USER_KEY);
	}
	public static String getUserSecret(Context c)
	{
		return getString(c, USER_SECRET);
	}
	public static void setUser(Context c,String user)
	{
		putString(c, USER, user);
	}
	public static String getUser(Context c)
	{
		return getString(c, USER);
	}
	
	public static String getAuthURL()
	{
		return "https://www.instapaper.com/api/authenticate";
	}
	public static String getAddURL()
	{
		return "https://www.instapaper.com/api/add";
	}
	public static String getListURL()
	{
		return "https://www.instapaper.com/api/1/bookmarks/list";
	}
	public static String getPassword(Context c)
	{
		return getString(c,PASSWORD);
	}

	public static void setUserPasswd(Context c, String user, String password) {
		SharedPreferences sp = getPrefs(c);
		SharedPreferences.Editor editor = sp.edit();
		editor.putString(USER, user);
		editor.putString(PASSWORD, password);
		editor.commit();
	}

	public static void setAuth(Context c, String authToken) 
	{
		SharedPreferences sp = getPrefs(c);
		SharedPreferences.Editor editor = sp.edit();
		editor.putString(AUTH, authToken);
		editor.putLong(AUTH_TIME, System.currentTimeMillis());
		editor.commit();
	}

	public static String getAuth(Context c) {
		return getString(c, AUTH);
	}
	
	
	public static long getAuthTime(Context c) {
		return getLong(c, AUTH_TIME, 0);
	}
	
}
