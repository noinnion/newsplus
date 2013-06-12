package com.noinnion.android.newsplus.extension.google_reader.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlUtils {

	public static String stripWhitespaces(String text) {
		if (text == null || text.length() == 0) { return text; }
		text = text.replaceAll("(\n|\r|\t| |\\s{2,})", " ");
		//   = &nbsp;
//		text = text.replaceAll("", " ").replaceAll("", " ");
		return text.trim();
	}

	public static String stripScriptsAndStyles(String s) {
		try {
			if (s == null) return "";
			s = s.replaceAll("(?i)<(style|script)>.*?</(style|script)>", "");
			return s.trim();			
		} catch (Throwable t) {
			// NOTE: OutOfMemoryError
			return "";
		}
	}
	
	public static String stripTags(String s) {
		try {
			if (s == null) return "";
			s = s.replaceAll("(?i)<(style|script)>.*?</(style|script)>", "").replaceAll("<.*?>", "");
			return s.trim();			
		} catch (Throwable t) {
			// NOTE: OutOfMemoryError
			return "";
		}
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

	public static String htmlAsPlainText(String value) {
		if (value == null || value.length() == 0) { return value; }
		value = value.replaceAll("\\s+", " ");
		value = value.replaceAll("<br\\s?/?>", "\n");
		value = value.replaceAll("<.*?>", " ");

		// NOTE: some html entities
		value = value.replaceAll("&lt;", "<");
		value = value.replaceAll("&gt;", ">");
		value = value.replaceAll("&quot;", "\"");
		value = value.replaceAll("&apos;", "\'");
		value = value.replaceAll("&nbsp;", " ");
		value = value.replaceAll("&amp;", "&");

		value = value.replaceAll("  +", " ");
		return value;
	}

	public static String htmlEscape(String value) {
		if (value == null || value.length() == 0) { return value; }
		value = value.replaceAll("&", "&amp;");
		value = value.replaceAll("<", "&lt;");
		value = value.replaceAll(">", "&gt;");
		value = value.replaceAll("\"", "&quot;");
		return value;
	}	
		
	public static String extractImageSrc(String html) {
        Pattern p = Pattern.compile("<img [^>]*?src=[\"\'](http.*?[\\.](jpeg|jpg|png|gif)).*?[\"\']", Pattern.CASE_INSENSITIVE);
        // Create a matcher with an input string
        Matcher m = p.matcher(html);
        boolean result = m.find();
        while(result) {
            return m.group(1);
        }
        return null;
	}

}
