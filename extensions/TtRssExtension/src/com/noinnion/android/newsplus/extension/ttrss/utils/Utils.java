package com.noinnion.android.newsplus.extension.ttrss.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.http.protocol.HTTP;

import android.os.Environment;
import android.text.TextUtils;

public class Utils {

	public static int asInt(Object value) {
		return asInt(value, 0);
	}

	public static int asInt(Object value, int defaultValue) {
		if (value == null) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(String.valueOf(value));
		} catch (NumberFormatException e) {
			return defaultValue;
		}
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

	public static String asString(Object value) {
		return asString(value, null);
	}

	public static String asString(Object value, String defaultValue) {
		if (value == null) {
			return defaultValue;
		}
		try {
			return String.valueOf(value).trim();
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	public static long[] convertLongs(List<Long> longs) {
		long[] ret = new long[longs.size()];
		Iterator<Long> iterator = longs.iterator();
		for (int i = 0; i < ret.length; i++) {
			ret[i] = iterator.next().longValue();
		}
		return ret;
	}

	public static long[] convertLongs(Set<Long> longs) {
		long[] ret = new long[longs.size()];
		Iterator<Long> iterator = longs.iterator();
		for (int i = 0; i < ret.length; i++) {
			ret[i] = iterator.next().longValue();
		}
		return ret;
	}
	

	public static String escapeQuery(String str) {
		return str.replaceAll("'", "");
	}

	public static String convertStreamToString(InputStream is) throws IOException {
		String encoding = HTTP.UTF_8;

		StringBuilder sb = new StringBuilder(Math.max(16, is.available()));
		char[] tmp = new char[4096];

		try {
			InputStreamReader reader = new InputStreamReader(is, encoding);
			for (int cnt; (cnt = reader.read(tmp)) > 0;)
				sb.append(tmp, 0, cnt);
		} finally {
			is.close();
		}
		return sb.toString().trim();
	}

	// detect charset for stream
//	public static String convertStreamToString(InputStream is, String charset) throws IOException {
//		ByteArrayOutputStream baos = new ByteArrayOutputStream();
//		// Fake code simulating the copy
//		// You can generally do better with nio if you need...
//		// And please, unlike me, do something about the Exceptions :D
//		byte[] buf = new byte[1024];
//		int len;
//		while ((len = is.read(buf)) > 0) {
//			baos.write(buf, 0, len);
//		}
//		baos.flush();
//
//		if (charset == null) {
//			InputStream is1 = new ByteArrayInputStream(baos.toByteArray());
//
//			// (1)
//			UniversalDetector detector = new UniversalDetector(null);
//
//			// (2)
//			try {
//				while ((len = is1.read(buf)) > 0 && !detector.isDone()) {
//					detector.handleData(buf, 0, len);
//				}
//			} finally {
//				is1.close();
//			}
//
//			// (3)
//			detector.dataEnd();
//
//			// (4)
//			charset = detector.getDetectedCharset();
//		}
//
//		StringBuilder sb = new StringBuilder(Math.max(16, is.available()));
//		char[] tmp = new char[4096];
//
//		InputStream is2 = new ByteArrayInputStream(baos.toByteArray());
//		try {
//			if (charset == null) charset = HTTP.UTF_8;
//			InputStreamReader reader = new InputStreamReader(is2, charset);
//			for (int cnt; (cnt = reader.read(tmp)) > 0;)
//				sb.append(tmp, 0, cnt);
//		} finally {
//			is2.close();
//		}
//		return sb.toString().trim();
//	}

	public static String getStackTrace(Throwable throwable) {
		Writer writer = new StringWriter();
		PrintWriter printWriter = new PrintWriter(writer);
		throwable.printStackTrace(printWriter);
		return writer.toString();
	}

	public static String md5(String s) {
		try {
			// Create MD5 Hash
			MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
			digest.update(s.getBytes());
			byte messageDigest[] = digest.digest();

			// Create Hex String
			StringBuffer hexString = new StringBuffer();
			for (int i = 0; i < messageDigest.length; i++)
				hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
			return hexString.toString();

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}
	
//	protected static void trustAllHosts(boolean trustAnyCert, boolean trustAnyHost) {
//	    try {
//	    	if (trustAnyCert) {
//	    	    X509TrustManager easyTrustManager = new X509TrustManager() {
//
//	    	        public void checkClientTrusted(
//	    	        		X509Certificate[] chain,
//	    	                String authType) throws CertificateException {
//	    	            // Oh, I am easy!
//	    	        }
//
//	    	        public void checkServerTrusted(
//	    	        		X509Certificate[] chain,
//	    	                String authType) throws CertificateException {
//	    	            // Oh, I am easy!
//	    	        }
//
//	    	        public X509Certificate[] getAcceptedIssuers() {
//	    	            return null;
//	    	        }
//
//	    	    };
//
//	    	    // Create a trust manager that does not validate certificate chains
//	    	    TrustManager[] trustAllCerts = new TrustManager[] {easyTrustManager};
//
//	    	    // Install the all-trusting trust manager
//
//	    		SSLContext sc = SSLContext.getInstance("TLS");
//
//	    		sc.init(null, trustAllCerts, new java.security.SecureRandom());
//
//	    		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
//	    	}
//	    	
//	    	if (trustAnyHost) {
//	    		HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {				
//	    			@Override
//	    			public boolean verify(String hostname, SSLSession session) {
//	    				return true;
//	    			}
//	    		});
//	    	}
//
//	    } catch (Exception e) {
//	            e.printStackTrace();
//	    }
//	}

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
