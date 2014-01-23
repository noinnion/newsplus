package com.noinnion.android.newsplus.extension.readability.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
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

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.SSLCertificateSocketFactory;

public final class HttpUtils {

    private static final int SOCKET_OPERATION_TIMEOUT = 20000; // 20s

	public static DefaultHttpClient createHttpClient() {
		return createHttpClient(null, false);
	}

	@SuppressLint("NewApi")
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


	public static String getHeaderValue(Header header) {
		if (header != null) return header.getValue();
		return null;
	}

	public static String getCharSet(HttpEntity entity) {
		if (entity == null) return null;

		Header header = entity.getContentType();
		if (header == null) return null;

		HeaderElement[] elements = header.getElements();
	 	if (elements.length > 0) {
	 		HeaderElement element = elements[0];
	 		NameValuePair param = element.getParameterByName("charset");
	 		if (param != null) {
	 			return param.getValue();
	 		}
	 	}

	 	return null;
	}

	public static final String	URL_GOOGLE_FAVICON	= "http://www.google.com/s2/u/0/favicons?domain=";

	public static byte[] getFavicon(String link) throws IOException {
		URL url = new URL(link);
		StringBuilder buff = new StringBuilder(128);
		buff.append(URL_GOOGLE_FAVICON).append(url.getHost());
		Bitmap icon = null;

//		HttpURLConnection conn = (HttpURLConnection) url.openConnection(); // Cast shouldn't fail
//		InputStream in = conn.getInputStream();
		InputStream in = doGetInputStream(new String(buff));

		try {
			icon = BitmapFactory.decodeStream(in);
		} finally {
			in.close();
		}
		int size = icon.getWidth() * icon.getHeight() * 2;
		ByteArrayOutputStream out = new ByteArrayOutputStream(size);
		icon.compress(Bitmap.CompressFormat.PNG, 100, out);
		out.flush();
		out.close();
		return out.toByteArray();
	}

	public static InputStream doGetInputStream(String url) throws IOException {
		// Log.d(TAG, "[DEBUG] GET: " + url);
		HttpGet get = new HttpGet(url);

		// gzip
		get.setHeader("User-agent", "gzip");

		HttpResponse res = createHttpClient().execute(get);
		int resStatus = res.getStatusLine().getStatusCode();

		if (resStatus == HttpStatus.SC_FORBIDDEN) {
			throw new IOException("403 Forbidden");
		} else if (resStatus == HttpStatus.SC_UNAUTHORIZED) {
			throw new IOException("401 Unauthorized");
		} else if (resStatus != HttpStatus.SC_OK) {
			throw new IOException("Invalid http status " + resStatus + ": " + url);
		}

		final HttpEntity entity = res.getEntity();
		if (entity == null) {
			throw new IOException("null response entity");
		}

		InputStream in = entity.getContent();
		return in;
	}

}
