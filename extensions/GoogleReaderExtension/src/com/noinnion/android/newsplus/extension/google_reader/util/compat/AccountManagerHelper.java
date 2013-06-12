package com.noinnion.android.newsplus.extension.google_reader.util.compat;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

/**
 * Uses level 14 APIs when possible to use getAuthToken
 * 
 */
public class AccountManagerHelper {
    
    public static AccountManagerFuture<Bundle> getAuthToken (AccountManager manager, Account account, String authTokenType, boolean notifyAuthFailure, AccountManagerCallback<Bundle> callback, Handler handler) {
        if (Build.VERSION.SDK_INT >= 14) {
        	return manager.getAuthToken (account, authTokenType, null, notifyAuthFailure, callback, handler);
        } else {
        	return manager.getAuthToken(account, authTokenType, notifyAuthFailure, callback, handler);
        }
    }
    
}