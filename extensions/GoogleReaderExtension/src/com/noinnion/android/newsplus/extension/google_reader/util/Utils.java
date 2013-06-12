package com.noinnion.android.newsplus.extension.google_reader.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.http.protocol.HTTP;

import android.content.Context;

import com.noinnion.android.newsplus.extension.google_reader.R;

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
	
	
	// time in secs
	public static String formatTimeAgo(Context c, long time) {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
		long currentTime = System.currentTimeMillis();
		long diff = currentTime - time;
		GregorianCalendar now = new GregorianCalendar();
		GregorianCalendar today = new GregorianCalendar(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DATE));
		long diffToday = currentTime - today.getTimeInMillis();

		if (diff < diffToday) return new StringBuilder(c.getText(R.string.time_today)).append(", ").append(simpleDateFormat.format(new Date(time))).toString();
		else if (diff < diffToday + 86400000) return new StringBuilder(c.getText(R.string.time_yesterday)).append(", ").append(simpleDateFormat.format(new Date(time))).toString();

		return DateFormat.getDateInstance().format(new Date(time));
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
}
