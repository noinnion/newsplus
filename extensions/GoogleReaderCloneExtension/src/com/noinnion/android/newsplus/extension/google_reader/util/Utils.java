package com.noinnion.android.newsplus.extension.google_reader.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.net.URLEncoder;

import org.apache.http.Header;
import org.apache.http.HttpVersion;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;

import android.net.SSLCertificateSocketFactory;
import android.os.Environment;
import android.text.TextUtils;


public class Utils {

    private static final int SOCKET_OPERATION_TIMEOUT = 20000; // 20s

	public static DefaultHttpClient createHttpClient() {
		return createHttpClient(null, false);
	}

	public static DefaultHttpClient createHttpClient(String userAgent, boolean redirecting) {
		HttpParams params = new BasicHttpParams();

		// Turn off stale checking.  Our connections break all the time anyway,
        // and it's not worth it to pay the penalty of checking every time.
        HttpConnectionParams.setStaleCheckingEnabled(params, false);

        // Set the timeout in milliseconds until a connection is established. The default value is zero, that means the timeout is not used.
        HttpConnectionParams.setConnectionTimeout(params, SOCKET_OPERATION_TIMEOUT);
        // Set the default socket timeout (SO_TIMEOUT) in milliseconds which is the timeout for waiting for data.
        HttpConnectionParams.setSoTimeout(params, SOCKET_OPERATION_TIMEOUT);

        HttpConnectionParams.setSocketBufferSize(params, 8192);

        // Don't handle redirects -- return them to the caller.  Our code
        // often wants to re-POST after a redirect, which we must do ourselves.
        HttpClientParams.setRedirecting(params, redirecting);

        HttpProtocolParams.setUserAgent(params, userAgent);
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

        // Set the specified user agent and register standard protocols.
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        if (AndroidUtils.isFroyo()) schemeRegistry.register(new Scheme("https", SSLCertificateSocketFactory.getHttpSocketFactory(SOCKET_OPERATION_TIMEOUT, null), 443));
        else schemeRegistry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));

        final ClientConnectionManager manager = new ThreadSafeClientConnManager(params, schemeRegistry);

		return new DefaultHttpClient(manager, params);
	}

	public static String stripTags(String s, boolean removeWhiteSpaces) {
		try {
			if (s == null) return "";
			s = s.replaceAll("(?i)<(style|script)>.*?</(style|script)>", "").replaceAll("<.*?>", "");
			if (removeWhiteSpaces) {
				s = s.replaceAll("(\n|\r|\t| |\\s{2,})", " ");
//				s = s.replaceAll(" ", " ").replaceAll("\\s{2,}", " ");
			}
			return s.trim();
		} catch (Throwable t) {
			// NOTE: OutOfMemoryError
			return "";
		}
	}

	public static String encode(String url) {
		try {
			return URLEncoder.encode(url, "UTF-8");
		} catch (UnsupportedEncodingException e) {}
		return url;
	}

	public static String decode(String url) {
		try {
			return URLDecoder.decode(url, "UTF-8");
		} catch (Exception e) {}
		return url;
	}

	public static String dec2Hex(String dec) {
		if (dec == null) return null;
		long n = new BigInteger(dec).longValue();
		String hex = Long.toHexString(n);
		// leading zeros for 16 digit
		while (hex.length() < 16) {
			hex = "0" + hex;
		}
		return hex;
	}

	public static long asLong(Object value) {
		return asLong(value, 0);
	}

	public static long asLong(Object value, long defaultValue) {
		if (value == null) {
			return defaultValue;
		}
		try {
			return Long.parseLong(String.valueOf(value));
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	public static String getHeaderValue(Header header) {
		if (header != null) return header.getValue();
		return null;
	}

	// writeFileToSD("/", "filename.txt", "content");
	public static boolean writeContentToSD(String path, String filename, String content) throws IOException {
		if (TextUtils.isEmpty(content)) return false;

		File dir = new File(Environment.getExternalStorageDirectory(), path);
		if (!dir.exists()) dir.mkdirs();

		File file = new File(dir, filename);
		FileWriter out = new FileWriter(file);
		try {
			out.write(content);
		} finally {
			out.flush();
			out.close();
		}

		return true;
	}

}
