package com.noinnion.android.newsplus.extension.google_reader.util;

import android.app.Application;
import android.util.Log;

/**
 * Static helper utilities for logging  (<b>Beta +</b>).<br>
 * <br>
 * Use {@link #setGlobalTag(String)}, {@link #setLogLevel(int)}, {@link #setLogTraceLevel(int)} or
 * {@link #setLogTraceFormat(boolean, boolean, boolean)} for initialization (a good place to call
 * this methods is in a custom {@link Application#onCreate()}.).<br>
 * <br>
 * Log priority:<br>
 * {@link #LVL_VERBOSE} &lt; {@link #LVL_DEBUG} &lt; {@link #LVL_INFO} &lt; {@link #LVL_WARNING}
 * &lt; {@link #LVL_ERROR} &lt; {@link #LVL_DISABLED}<br>
 * <br>
 * <b>Use logging</b>:
 * 
 * <pre>
 * if (L.isD()) {
 * 	L.d(&quot;That's bad: &quot; + objectWithHeavyStringConstruction.toString());
 * }
 * 
 * // or simply
 * L.d(&quot;That's bad&quot;);
 * </pre>
 * 
 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
 */
public class L {
	
	// PUBLIC CONSTANTS ===========================================================================
	
	public static final int LVL_VERBOSE = 0;
	public static final int LVL_DEBUG = 1;
	public static final int LVL_INFO = 2;
	public static final int LVL_WARNING = 3;
	public static final int LVL_ERROR = 4;
	public static final int LVL_DISABLED = 6;
	
	// PRIVATE ====================================================================================
	
	/** Used logging tag when log methods without tag are used given. */
	private static String mGlobalLogTag = "Android Toolbox";
	
	/** Log level from which log trace will be shown. */
	private static int mLogTraceLevel = LVL_DEBUG;
	
	/** Should method name displayed in log trace string? */
	private static boolean mShowMethodInLogTrace = false;
	
	/** Should line number displayed in log trace string? */
	private static boolean mShowLineInLogTrace = true;
	
	/** Should inner class name should be stripped from class in log trace string? */
	private static boolean mStripInnerClassInLogTrace = true;
	
	/** Log level for log messages. */
	private static int mLogLevel = LVL_DEBUG;
	
	// PUBLIC =====================================================================================
	
	/**
	 * Is logging at {@link #LVL_VERBOSE} level enabled?
	 * 
	 * @return {@code true} if logging enabled
	 */
	public static boolean isV() {
		return mLogLevel <= LVL_VERBOSE;
	}
	
	/**
	 * Log message at {@link #LVL_VERBOSE} level.
	 * 
	 * @param msg
	 *            message
	 */
	public static void v(String msg) {
		if (isV())
			Log.v(mGlobalLogTag, getLogTrace(LVL_VERBOSE) + String.valueOf(msg));
	}
	
	/**
	 * Log message at {@link #LVL_VERBOSE} level.
	 * 
	 * @param msg
	 *            message
	 * @param tr
	 *            throwable which causes the log
	 */
	public static void v(String msg, Throwable tr) {
		if (isV())
			Log.v(mGlobalLogTag, getLogTrace(LVL_VERBOSE) + String.valueOf(msg), tr);
	}
	
	/**
	 * Log message at {@link #LVL_VERBOSE} level.
	 * 
	 * @param tag
	 *            tag to use for log message
	 * @param msg
	 *            message
	 */
	public static void v(String tag, String msg) {
		if (isV())
			Log.v(tag, getLogTrace(LVL_VERBOSE) + String.valueOf(msg));
	}
	
	/**
	 * Log message at {@link #LVL_VERBOSE} level.
	 * 
	 * @param tag
	 *            tag to use for log message
	 * @param msg
	 *            message
	 * @param tr
	 *            throwable which causes the log
	 */
	public static void v(String tag, String msg, Throwable tr) {
		if (isV())
			Log.v(tag, getLogTrace(LVL_VERBOSE) + String.valueOf(msg), tr);
	}
	
	
	/**
	 * Is logging at {@link #LVL_DEBUG} level enabled?
	 * 
	 * @return {@code true} if logging enabled
	 */
	public static boolean isD() {
		return mLogLevel <= LVL_DEBUG;
	}
	
	/**
	 * Log message at {@link #LVL_DEBUG} level.
	 * 
	 * @param msg
	 *            message
	 */
	public static void d(String msg) {
		if (isD())
			Log.d(mGlobalLogTag, getLogTrace(LVL_DEBUG) + String.valueOf(msg));
	}
	
	/**
	 * Log message at {@link #LVL_DEBUG} level.
	 * 
	 * @param msg
	 *            message
	 * @param tr
	 *            throwable which causes the log
	 */
	public static void d(String msg, Throwable tr) {
		if (isD())
			Log.d(mGlobalLogTag, getLogTrace(LVL_DEBUG) + String.valueOf(msg), tr);
	}
	
	/**
	 * Log message at {@link #LVL_DEBUG} level.
	 * 
	 * @param tag
	 *            tag to use for log message
	 * @param msg
	 *            message
	 */
	public static void d(String tag, String msg) {
		if (isD())
			Log.d(tag, getLogTrace(LVL_DEBUG) + String.valueOf(msg));
	}
	
	/**
	 * Log message at {@link #LVL_DEBUG} level.
	 * 
	 * @param tag
	 *            tag to use for log message
	 * @param msg
	 *            message
	 * @param tr
	 *            throwable which causes the log
	 */
	public static void d(String tag, String msg, Throwable tr) {
		if (isD())
			Log.d(tag, getLogTrace(LVL_DEBUG) + String.valueOf(msg), tr);
	}
	
	
	/**
	 * Is logging at {@link #LVL_INFO} level enabled?
	 * 
	 * @return {@code true} if logging enabled
	 */
	public static boolean isI() {
		return mLogLevel <= LVL_INFO;
	}
	
	/**
	 * Log message at {@link #LVL_INFO} level.
	 * 
	 * @param msg
	 *            message
	 */
	public static void i(String msg) {
		if (isI())
			Log.i(mGlobalLogTag, getLogTrace(LVL_INFO) + String.valueOf(msg));
	}
	
	/**
	 * Log message at {@link #LVL_INFO} level.
	 * 
	 * @param msg
	 *            message
	 * @param tr
	 *            throwable which causes the log
	 */
	public static void i(String msg, Throwable tr) {
		if (isI())
			Log.i(mGlobalLogTag, getLogTrace(LVL_INFO) + String.valueOf(msg), tr);
	}
	
	/**
	 * Log message at {@link #LVL_INFO} level.
	 * 
	 * @param tag
	 *            tag to use for log message
	 * @param msg
	 *            message
	 */
	public static void i(String tag, String msg) {
		if (isI())
			Log.i(tag, getLogTrace(LVL_INFO) + String.valueOf(msg));
	}
	
	/**
	 * Log message at {@link #LVL_INFO} level.
	 * 
	 * @param tag
	 *            tag to use for log message
	 * @param msg
	 *            message
	 * @param tr
	 *            throwable which causes the log
	 */
	public static void i(String tag, String msg, Throwable tr) {
		if (isI())
			Log.i(tag, getLogTrace(LVL_INFO) + String.valueOf(msg), tr);
	}
	
	
	/**
	 * Is logging at {@link #LVL_WARNING} level enabled?
	 * 
	 * @return {@code true} if logging enabled
	 */
	public static boolean isW() {
		return mLogLevel <= LVL_WARNING;
	}
	
	/**
	 * Log message at {@link #LVL_WARNING} level.
	 * 
	 * @param msg
	 *            message
	 */
	public static void w(String msg) {
		if (isW())
			Log.w(mGlobalLogTag, getLogTrace(LVL_WARNING) + String.valueOf(msg));
	}
	
	/**
	 * Log message at {@link #LVL_WARNING} level.
	 * 
	 * @param msg
	 *            message
	 * @param tr
	 *            throwable which causes the log
	 */
	public static void w(String msg, Throwable tr) {
		if (isW())
			Log.w(mGlobalLogTag, getLogTrace(LVL_WARNING) + String.valueOf(msg), tr);
	}
	
	/**
	 * Log message at {@link #LVL_WARNING} level.
	 * 
	 * @param tag
	 *            tag to use for log message
	 * @param msg
	 *            message
	 */
	public static void w(String tag, String msg) {
		if (isW())
			Log.w(tag, getLogTrace(LVL_WARNING) + String.valueOf(msg));
	}
	
	/**
	 * Log message at {@link #LVL_WARNING} level.
	 * 
	 * @param tag
	 *            tag to use for log message
	 * @param msg
	 *            message
	 * @param tr
	 *            throwable which causes the log
	 */
	public static void w(String tag, String msg, Throwable tr) {
		if (isW())
			Log.w(tag, getLogTrace(LVL_WARNING) + String.valueOf(msg), tr);
	}
	
	
	/**
	 * Is logging at {@link #LVL_ERROR} level enabled?
	 * 
	 * @return {@code true} if logging enabled
	 */
	public static boolean isE() {
		return mLogLevel <= LVL_ERROR;
	}
	
	/**
	 * Log message at {@link #LVL_ERROR} level.
	 * 
	 * @param msg
	 *            message
	 */
	public static void e(String msg) {
		if (isE())
			Log.e(mGlobalLogTag, getLogTrace(LVL_ERROR) + String.valueOf(msg));
	}
	
	/**
	 * Log message at {@link #LVL_ERROR} level.
	 * 
	 * @param msg
	 *            message
	 * @param tr
	 *            throwable which causes the log
	 */
	public static void e(String msg, Throwable tr) {
		if (isE())
			Log.e(mGlobalLogTag, getLogTrace(LVL_ERROR) + String.valueOf(msg), tr);
	}
	
	/**
	 * Log message at {@link #LVL_ERROR} level.
	 * 
	 * @param tag
	 *            tag to use for log message
	 * @param msg
	 *            message
	 */
	public static void e(String tag, String msg) {
		if (isE())
			Log.e(tag, getLogTrace(LVL_ERROR) + String.valueOf(msg));
	}
	
	/**
	 * Log message at {@link #LVL_ERROR} level.
	 * 
	 * @param tag
	 *            tag to use for log message
	 * @param msg
	 *            message
	 * @param tr
	 *            throwable which causes the log
	 */
	public static void e(String tag, String msg, Throwable tr) {
		if (isE())
			Log.e(tag, getLogTrace(LVL_ERROR) + String.valueOf(msg), tr);
	}
	
	
	/**
	 * Set global log tag.<br>
	 * <br>
	 * This tag will be used for logging if no tag was given by log method.<br>
	 * Default is {@code "Android Toolbox"}.
	 * 
	 * @param tag
	 *            log tag
	 */
	public static void setGlobalTag(String tag) {
		mGlobalLogTag = String.valueOf(tag);
	}
	
	/**
	 * Set log level from which trace will be logged with message.<br>
	 * <br>
	 * Every log request to a log level less that this one won't print a log trace.<br>
	 * Default is {@link #LVL_DEBUG}.
	 * 
	 * @param level
	 *            {@link #LVL_VERBOSE}, {@link #LVL_DEBUG}, {@link #LVL_INFO}, {@link #LVL_WARNING},
	 *            {@link #LVL_ERROR} or {@link #LVL_DISABLED}
	 * 
	 * @see #setLogTraceFormat(boolean, boolean, boolean)
	 */
	public static void setLogTraceLevel(int level) {
		mLogTraceLevel = level;
	}
	
	/**
	 * Set format of log trace.<br>
	 * <br>
	 * Log trace is the information in front of every log message.<br>
	 * Default is strip inner class, no method and show line {@code [Class:Line]}.<br>
	 * <br>
	 * Possible full format: {@code [Class$Inner.Method:Line]}<br>
	 * <br>
	 * Use {@link #setLogTraceLevel(int)} to disable completely.
	 * 
	 * @param stripInnerClass
	 *            should inner class be stripped from class name
	 * @param showMethod
	 *            should method name be displayed
	 * @param showLine
	 *            should line number be displayed
	 */
	public static void setLogTraceFormat(boolean stripInnerClass, boolean showMethod,
			boolean showLine) {
		mStripInnerClassInLogTrace = stripInnerClass;
		mShowMethodInLogTrace = showMethod;
		mShowLineInLogTrace = showLine;
	}
	
	/**
	 * Set log level.<br>
	 * <br>
	 * Every log request to a log level less that this one will be ignored.<br>
	 * Default is {@link #LVL_DEBUG}.
	 * 
	 * @param level
	 *            {@link #LVL_VERBOSE}, {@link #LVL_DEBUG}, {@link #LVL_INFO}, {@link #LVL_WARNING},
	 *            {@link #LVL_ERROR} or {@link #LVL_DISABLED}
	 */
	public static void setLogLevel(int level) {
		mLogLevel = level;
	}
	
	// PRIVATE ====================================================================================
	
	/**
	 * Get trace of log.
	 * 
	 * @see #setLogTraceFormat(boolean, boolean)
	 * 
	 * @param level
	 *            log level of log method which requested trace
	 * 
	 * @return {@code [Class.Method:Line]}
	 */
	private static String getLogTrace(int level) {
		if (level < mLogTraceLevel) {
			return "";
		}
		
		StackTraceElement stack[] = Thread.currentThread().getStackTrace();
		StackTraceElement element = stack[4];
		
		String className = element.getClassName();
		className = className.substring(className.lastIndexOf(".") + 1, className.length());
		StringBuilder logTrace = new StringBuilder();
		
		if (mStripInnerClassInLogTrace) {
			int toInner = className.indexOf('$');
			
			if (toInner != -1) {
				className = className.substring(0, toInner);
			}
		}
		
		logTrace.append("[");
		logTrace.append(className);
		
		if (mShowMethodInLogTrace) {
			logTrace.append(".");
			logTrace.append(element.getMethodName());
		}
		
		if (mShowLineInLogTrace) {
			logTrace.append(":");
			logTrace.append(element.getLineNumber());
		}
		
		logTrace.append("] ");
		
		return logTrace.toString();
	}
	
	
	/**
	 * No constructor for static class.
	 */
	private L() {
		
	}
}
