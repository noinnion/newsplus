package com.noinnion.android.newsplus.extension.google_reader;

import java.io.IOException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

/**
 * @author Evan
 *         AuthHelper retrieves authentication information about the user logged on the Android device
 */
public class AuthHelper {

	public static final String	GOOGLE_ACCOUNT_TYPE				= "com.google";	
	public static final String	AUTH_TOKEN_TYPE					= "reader";
	
	
	/**
	 * Retrieves all of the Google accounts on the device
	 * 
	 * @param context
	 * @return
	 */
	public static Account[] getAccounts(Context context) {
		return AccountManager.get(context).getAccountsByType(GOOGLE_ACCOUNT_TYPE);
	}

	public static String getRefreshedAuthToken(Context context, String name, Activity activity, String token) {
		String retVal = "";
		Account account = new Account(name, GOOGLE_ACCOUNT_TYPE);
		AccountManager.get(context).invalidateAuthToken(GOOGLE_ACCOUNT_TYPE, token);

		AccountManagerFuture<Bundle> accFut = AccountManager.get(context).getAuthToken(account, AUTH_TOKEN_TYPE, null, activity, null, null);
		try {
			Bundle authTokenBundle = accFut.getResult();
			retVal = authTokenBundle.get(AccountManager.KEY_AUTHTOKEN).toString();
		} catch (OperationCanceledException e) {
			e.printStackTrace();
		} catch (AuthenticatorException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return retVal;
	}

	public static String getAuthToken(Context context, String name, Activity activity) {
		String retVal = "";
		Account account = new Account(name, GOOGLE_ACCOUNT_TYPE);
		AccountManagerFuture<Bundle> accFut = AccountManager.get(context).getAuthToken(account, AUTH_TOKEN_TYPE, null, activity, null, null);
		try {
			Bundle authTokenBundle = accFut.getResult();
			retVal = authTokenBundle.get(AccountManager.KEY_AUTHTOKEN).toString();
		} catch (OperationCanceledException e) {
			e.printStackTrace();
		} catch (AuthenticatorException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return retVal;
	}

}
