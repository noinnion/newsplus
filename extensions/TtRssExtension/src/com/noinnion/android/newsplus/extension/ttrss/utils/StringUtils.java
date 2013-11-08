package com.noinnion.android.newsplus.extension.ttrss.utils;

import java.lang.reflect.Array;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Static helper utilities for string operations (<b>Beta</b>).
 * 
 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
 */
public final class StringUtils {

	// PUBLIC =====================================================================================

	/**
	 * Split string by delimiter.<br>
	 * <br>
	 * Like {@link #split(String, String, boolean, int)} with parameters {@code false} and {@code 0}
	 * 
	 * @param delimiter
	 *            delimiter between splits
	 * @param string
	 *            string to split
	 * 
	 * @return split strings
	 */
	public static String[] split(String delimiter, String string) {
		return split(delimiter, string, true, 0);
	}

	/**
	 * Split string by delimiter.<br>
	 * <br>
	 * Like {@link #split(String, String, boolean, int)} with parameter {@code 0}
	 * 
	 * @param delimiter
	 *            delimiter between splits
	 * @param string
	 *            string to split
	 * @param removeEmpty
	 *            {@code true} when empty strings should't be returned
	 * 
	 * @return split strings
	 */
	public static String[] split(String delimiter, String string, boolean removeEmpty) {
		return split(delimiter, string, removeEmpty, 0);
	}

	/**
	 * Split string by delimiter.
	 * 
	 * @param delimiter
	 *            delimiter between splits
	 * @param string
	 *            string to split
	 * @param removeEmpty
	 *            {@code true} when empty strings should't be returned
	 * @param minSplits
	 *            the least amount of splits which should be returned (added splits will be empty
	 *            strings)
	 * 
	 * @return split strings
	 */
	public static String[] split(String delimiter, String string, boolean removeEmpty, int minSplits) {
		List<String> list = new LinkedList<String>();

		int delimiterLength = delimiter.length();
		int stringLength = string.length();

		int currentMatch = 0;
		int nextMatch = string.indexOf(delimiter);

		if (nextMatch == -1) {
			if (!removeEmpty || stringLength != 0) {
				list.add(string);
			}
		} else {
			while (true) {
				if (!removeEmpty || currentMatch != nextMatch) {
					list.add(string.substring(currentMatch, nextMatch));
				}

				currentMatch = nextMatch + delimiterLength;

				if (nextMatch == stringLength || currentMatch == stringLength) {
					break;
				}

				nextMatch = string.indexOf(delimiter, currentMatch);

				if (nextMatch == -1) {
					nextMatch = stringLength;
				}
			}
		}

		for (int i = list.size(); i < minSplits; i++) {
			list.add("");
		}

		return list.toArray(new String[0]);
	}

	/**
	 * Join array to a string.
	 * 
	 * Example:<br>
	 * 
	 * <pre>
	 * join(",", new int [] {1, 2, 3)) = "1,2,3"
	 * </pre>
	 * 
	 * You can pass anything as {@code array} as long its {@code toString()} method makes sense.
	 * 
	 * @param separator
	 *            separator to put between joined string elements
	 * @param array
	 *            object which is an array with elements on which {@code toString()} will be called
	 *            to join them to a string
	 * 
	 * @return joined string
	 * 
	 * @throws IllegalArgumentException
	 *             {@code array} is not an array
	 */
	public static String join(Object array, String separator) {
		if (!array.getClass().isArray()) {
			throw new IllegalArgumentException("Given object is not an array!");
		}

		StringBuilder s = new StringBuilder();
		int length = Array.getLength(array) - 1;

		for (int i = 0; i <= length; i++) {
			s.append(String.valueOf(Array.get(array, i)));

			if (i != length) {
				s.append(separator);
			}
		}

		return s.toString();
	}

	public static String implode(final Collection<?> values, final String delimiter) {
		return implode(values.toArray(), delimiter);
	}

	public static String implode(final Object[] values, final String delimiter) {
		if (values == null) return "";
		
		final StringBuilder out = new StringBuilder();
		
		for (int i = 0, l = values.length; i < l; i++) {
			if (i > 0) out.append(delimiter);
			out.append(values[i]);
		}
		return out.toString();
	}
	
	public static String implode(final Object[] values, final String delimiter, int start, int end) {
		if (values == null) return "";

		final StringBuilder out = new StringBuilder();

		int l = end > values.length - 1 ? values.length : end + 1;
		for (int i = start; i < l; i++) {
			if (i > start) out.append(delimiter);
			out.append(values[i]);
		}
		return out.toString();
	}

	public static String implode(final long[] values, final String delimiter, int start, int end) {
		if (values == null) return "";

		final StringBuilder out = new StringBuilder();

		int l = end > values.length - 1 ? values.length : end + 1;
		for (int i = start; i < l; i++) {
			if (i > start) out.append(delimiter);
			out.append(values[i]);
		}
		return out.toString();
	}	
	
	/**
	 * Prepend value(s) to an array.<br>
	 * <br>
	 * Example:<br>
	 * 
	 * <pre>
	 * prepend(new int [] {1, 2, 3), 4) = String [] {"41", "42", "43"}}
	 * prepend(new int [] {1, 2, 3), new String [] {"a", "b", "c"}) = String [] {"a1", "b2", "c3"}}
	 * </pre>
	 * 
	 * You can pass anything as {@code array} and {@code prefix} as long its {@code toString()} method makes sense.
	 * 
	 * @param array
	 *            object which is an array with elements on which {@code toString()} will be called
	 *            to join them to a string
	 * @param prefix
	 *            object or array on which {@code toString()} will be called
	 * 
	 * @return string array with prepended prefix
	 * 
	 * @throws NullPointerException
	 * @throws IllegalArgumentException
	 *             {@code array} is not an array
	 * @throws ArrayIndexOutOfBoundsException
	 *             if {@code prefix} array length is less the length of {@code array}
	 */
	public static String[] prepend(Object array, Object prefix) {
		if (!array.getClass().isArray()) {
			throw new IllegalArgumentException("Given object is not an array!");
		}

		boolean isArray = prefix.getClass().isArray();
		String[] result = new String[Array.getLength(array)];

		for (int i = 0; i < result.length; i++) {
			result[i] = String.valueOf(isArray ? Array.get(prefix, i) : prefix) + String.valueOf(String.valueOf(Array.get(array, i)));
		}

		return result;
	}

	/**
	 * Append value(s) to an array.<br>
	 * <br>
	 * Example:<br>
	 * 
	 * <pre>
	 * append(new int [] {1, 2, 3), 4) = String [] {"14", "24", "34"}}
	 * append(new int [] {1, 2, 3), new String [] {"a", "b", "c"}) = String [] {"1a", "2b", "3c"}}
	 * </pre>
	 * 
	 * You can pass anything as {@code array} and {@code postfix} as long its {@code toString()} method makes sense.
	 * 
	 * @param array
	 *            object which is an array with elements on which {@code toString()} will be called
	 *            to join them to a string
	 * @param postfix
	 *            object or array on which {@code toString()} will be called
	 * 
	 * @return string array with appended postfix
	 * 
	 * @throws NullPointerException
	 * @throws IllegalArgumentException
	 *             {@code array} is not an array
	 * @throws ArrayIndexOutOfBoundsException
	 *             if {@code postfix} array length is less the length of {@code array}
	 */
	public static String[] append(Object array, Object postfix) {
		if (!array.getClass().isArray()) {
			throw new IllegalArgumentException("Given object is not an array!");
		}

		boolean isArray = postfix.getClass().isArray();
		String[] result = new String[Array.getLength(array)];

		for (int i = 0; i < result.length; i++) {
			result[i] = String.valueOf(String.valueOf(Array.get(array, i))) + String.valueOf(isArray ? Array.get(postfix, i) : postfix);
		}

		return result;
	}

	/**
	 * Prepend and append value(s) to an array.<br>
	 * <br>
	 * It works the same way as {@link #prepend(Object, Object)} and {@link #append(Object, Object)} but it's a little bit faster since operations will be performed on the same time.<br>
	 * <br>
	 * You can pass anything as {@code array}, {@code prefix} and {@code postfix} as long its {@code toString()} method makes sense.
	 * 
	 * @param array
	 *            object which is an array with elements on which {@code toString()} will be called
	 *            to join them to a string
	 * @param prefix
	 *            object or array on which {@code toString()} will be called
	 * @param postfix
	 *            object or array on which {@code toString()} will be called
	 * 
	 * @return string array with prepended prefix
	 * 
	 * @throws NullPointerException
	 * @throws IllegalArgumentException
	 *             {@code array} is not an array
	 * @throws ArrayIndexOutOfBoundsException
	 *             if {@code prefix} or {@code postfix} array length is less the length of {@code array}
	 */
	public static String[] prependAndAppend(Object array, Object prefix, Object postfix) {
		if (!array.getClass().isArray()) {
			throw new IllegalArgumentException("Given object is not an array!");
		}

		boolean isPrefixArray = prefix.getClass().isArray();
		boolean isPostfixArray = postfix.getClass().isArray();
		String[] result = new String[Array.getLength(array)];

		for (int i = 0; i < result.length; i++) {
			result[i] = String.valueOf(isPrefixArray ? Array.get(prefix, i) : prefix) + String.valueOf(String.valueOf(Array.get(array, i))) + String.valueOf(isPostfixArray ? Array.get(postfix, i) : postfix);
		}

		return result;
	}

	/**
	 * Does an array contain a value?
	 * 
	 * @param array
	 *            object which is an array with elements on which {@code toString()} will be called
	 *            to compare them with the given value
	 * @param value
	 *            value to check whether it's in the given array
	 * 
	 * @return {@code true} {@code array} contains {@code value}
	 * 
	 * @throws IllegalArgumentException
	 *             {@code array} is not an array
	 */
	public static boolean contains(Object array, String value) {
		if (!array.getClass().isArray()) {
			throw new IllegalArgumentException("Given object is not an array!");
		}

		for (int i = 0; i < Array.getLength(array); i++) {
			if (String.valueOf(Array.get(array, i)).equals(String.valueOf(value))) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Parse a {@code Long} value from string.
	 * 
	 * @param s
	 *            string to parse
	 * 
	 * @return value or {@code null} when {@code s} is {@code null} or {@code s} does not represent
	 *         a {@code Long}
	 */
	public static Long getLong(String s) {
		if (s != null) {
			try {
				return Long.parseLong(s);
			} catch (NumberFormatException e) {
			}
		}

		return null;
	}

	/**
	 * Parse a {@code Integer} value from string.
	 * 
	 * @param s
	 *            string to parse
	 * 
	 * @return value or {@code null} when {@code s} is {@code null} or {@code s} does not represent
	 *         an {@code Integer}
	 */
	public static Integer getInteger(String s) {
		if (s != null) {
			try {
				return Integer.parseInt(s);
			} catch (NumberFormatException e) {
			}
		}

		return null;
	}

	/**
	 * Parse a {@code Boolean} value from string.
	 * 
	 * @param s
	 *            string to parse
	 * 
	 * @return value or {@code null} when {@code s} is {@code null} or {@code s} does not represent
	 *         a {@code Boolean}
	 */
	public static Boolean getBoolean(String s) {
		if (s != null) {
			try {
				return Boolean.parseBoolean(s);
			} catch (NumberFormatException e) {
			}
		}

		return null;
	}

	/**
	 * Parse a {@code Float} value from string.
	 * 
	 * @param s
	 *            string to parse
	 * 
	 * @return value or {@code null} when {@code s} is {@code null} or {@code s} does not represent
	 *         a {@code Float}
	 */
	public static Float getFloat(String s) {
		if (s != null) {
			try {
				return Float.parseFloat(s);
			} catch (NumberFormatException e) {
			}
		}

		return null;
	}

	/**
	 * Parse a {@code Double} value from string.
	 * 
	 * @param s
	 *            string to parse
	 * 
	 * @return value or {@code null} when {@code s} is {@code null} or {@code s} does not represent
	 *         a {@code Double}
	 */
	public static Double getDouble(String s) {
		if (s != null) {
			try {
				return Double.parseDouble(s);
			} catch (NumberFormatException e) {
			}
		}

		return null;
	}

	/**
	 * Parse an {@code int} value from string.
	 * 
	 * @param s
	 *            string to parse
	 * @param defaultValue
	 *            value returned if string can't be parsed
	 * 
	 * @return parsed (default) value
	 */
	public static int getInteger(String s, int defaultValue) {
		Integer v = getInteger(s);
		return v == null ? defaultValue : v;
	}

	/**
	 * Parse an {@code long} value from string.
	 * 
	 * @param s
	 *            string to parse
	 * @param defaultValue
	 *            value returned if string can't be parsed
	 * 
	 * @return parsed (default) value
	 */
	public static long getLong(String s, long defaultValue) {
		Long v = getLong(s);
		return v == null ? defaultValue : v;
	}

	/**
	 * Parse an {@code float} value from string.
	 * 
	 * @param s
	 *            string to parse
	 * @param defaultValue
	 *            value returned if string can't be parsed
	 * 
	 * @return parsed (default) value
	 */
	public static float getFloat(String s, float defaultValue) {
		Float v = getFloat(s);
		return v == null ? defaultValue : v;
	}

	/**
	 * Parse an {@code double} value from string.
	 * 
	 * @param s
	 *            string to parse
	 * @param defaultValue
	 *            value returned if string can't be parsed
	 * 
	 * @return parsed (default) value
	 */
	public static double getDouble(String s, double defaultValue) {
		Double v = getDouble(s);
		return v == null ? defaultValue : v;
	}

	/**
	 * Parse an {@code boolean} value from string.
	 * 
	 * @param s
	 *            string to parse
	 * @param defaultValue
	 *            value returned if string can't be parsed
	 * 
	 * @return parsed (default) value
	 */
	public static boolean getBoolean(String s, boolean defaultValue) {
		Boolean v = getBoolean(s);
		return v == null ? defaultValue : v;
	}

	/**
	 * Compare to version strings.<br>
	 * <br>
	 * String will be split by delimiter, each part will be converted to an integer and then the
	 * parts will be compared.<br>
	 * <br>
	 * Examples:<br>
	 * {@code compareVersion(".", "1.4.5", "1.4.4") == 1}<br>
	 * {@code compareVersion(".", "1.4.5", "1.4.5.0") == 0}<br>
	 * {@code compareVersion(".", "1.4.5", "1.4.5.2") == -1}
	 * 
	 * @param delimiter
	 *            delimiter between version numbers
	 * @param version1
	 *            first version
	 * @param version2
	 *            second version
	 * 
	 * @return {@code 0} when equal, {@code 1} when first version is greater than second or {@code -1} when first version is less than second
	 */
	public static int compareVersion(String delimiter, String version1, String version2) {
		String[] v1Parts = split(delimiter, version1, false);
		String[] v2Parts = split(delimiter, version2, false);
		int count = v1Parts.length < v2Parts.length ? v1Parts.length : v2Parts.length;

		for (int i = 0; i < count; i++) {
			int v1 = getInteger(v1Parts[i], 0);
			int v2 = getInteger(v2Parts[i], 0);

			if (v1 > v2) {
				return 1;
			} else if (v1 < v2) {
				return -1;
			}
		}

		for (int i = count; i < v1Parts.length; i++) {
			if (getInteger(v1Parts[i], 0) > 0) {
				return 1;
			}
		}

		for (int i = count; i < v2Parts.length; i++) {
			if (getInteger(v2Parts[i], 0) > 0) {
				return -1;
			}
		}

		return 0;
	}

	/**
	 * Extract a name from a URL.<br>
	 * <br>
	 * E.g. {@code http://mail.google.com/param} becomes {@code Google}.
	 * 
	 * @param url
	 *            URL to extract from
	 * 
	 * @return name
	 */
	public static String extractNameFromUrl(URL url) {
		String host = url.getHost();
		int lastDot = host.lastIndexOf('.');

		if (lastDot != -1) {
			host = host.substring(0, lastDot);

			lastDot = host.lastIndexOf('.');

			if (lastDot != -1) {
				host = host.substring(lastDot + 1);
			}
		}

		if (!host.equals("")) {
			host = Character.toUpperCase(host.charAt(0)) + host.substring(1);
		}

		return host;
	}

	// PRIVATE ====================================================================================

	/**
	 * No constructor for a static class.
	 */
	private StringUtils() {

	}
}
