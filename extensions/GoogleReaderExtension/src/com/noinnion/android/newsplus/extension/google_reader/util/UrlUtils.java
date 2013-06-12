package com.noinnion.android.newsplus.extension.google_reader.util;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.webkit.MimeTypeMap;

public class UrlUtils {

	// Set of regular expression rules for parsing 
	// URL and exracting its parts 
	static String protocolPattern	= "((http|https|ftp)://)?"; 
	static String accessPattern	= "(([a-z0-9_]+):([a-z0-9-_]*)@)?"; 
	static String subDomainPattern	= "(([a-z0-9_-]+\\.)*)"; 
	static String domainPattern	= "(([a-z0-9-]{2,})\\.)"; 
	static String tldPattern		= "(com|net|org|edu|gov|mil|int|arpa|aero|biz|coop|info|museum|name|ad|ae|af|ag|ai|al|am|an|ao|aq|ar|as|at|au|aw|az|ba|bb|bd|be|bf|bg|bh|bi|bj|bm|bn|bo|br|bs|bt|bv|bw|by|bz|ca|cc|cf|cd|cg|ch|ci|ck|cl|cm|cn|co|cr|cs|cu|cv|cx|cy|cz|de|dj|dk|dm|do|dz|ec|ee|eg|eh|er|es|et|fi|fj|fk|fm|fo|fr|fx|ga|gb|gd|ge|gf|gh|gi|gl|gm|gn|gp|gq|gr|gs|gt|gu|gw|gy|hk|hm|hn|hr|ht|hu|id|ie|il|in|io|iq|ir|is|it|jm|jo|jp|ke|kg|kh|ki|km|kn|kp|kr|kw|ky|kz|la|lb|lc|li|lk|lr|ls|lt|lu|lv|ly|ma|mc|md|mg|mh|mk|ml|mm|mn|mo|mp|mq|mr|ms|mt|mu|mv|mw|mx|my|mz|na|nc|ne|nf|ng|ni|nl|no|np|nr|nt|nu|nz|om|pa|pe|pf|pg|ph|pk|pl|pm|pn|pr|pt|pw|py|qa|re|ro|ru|rw|sa|sb|sc|sd|se|sg|sh|si|sj|sk|sl|sm|sn|so|sr|st|su|sv|sy|sz|tc|td|tf|tg|th|tj|tk|tm|tn|to|tp|tr|tt|tv|tw|tz|ua|ug|uk|um|us|uy|uz|va|vc|ve|vg|vi|vn|vu|wf|ws|ye|yt|yu|za|zm|zr|zw)"; 
	static String portPattern		= "(:(\\d+))?"; 
	static String pathPattern		= "((/[a-z0-9-_.%~]*)*)?"; 
	static String queryPattern		= "(\\?[^? ]*)?";
	static String urlPattern 		= protocolPattern + accessPattern + subDomainPattern + domainPattern + tldPattern + portPattern + pathPattern + queryPattern;
	
    public static boolean validUrl(String s)
    {
            if (s.trim().equals("")) return false;
            // SCHEME
            String regex = "^(https?|ftp)://";
            // USER AND PASS (optional)
            regex += "([a-z0-9+!*(),;?&=\\$_.-]+(:[a-z0-9+!*(),;?&=\\$_.-]+)?@)?";
            // HOSTNAME OR IP
            regex += "[a-z0-9+\\$_-]+(\\.[a-z0-9+\\$_-]+)*"; // http://x = allowed (ex. http://localhost, http://routerlogin)
            //$urlregex .= "[a-z0-9+\$_-]+(\.[a-z0-9+\$_-]+)+";  // http://x.x = minimum
            //$urlregex .= "([a-z0-9+\$_-]+\.)*[a-z0-9+\$_-]{2,3}";  // http://x.xx(x) = minimum
            //use only one of the above
            // PORT (optional)
            //regex += "(\:[0-9]{2,5})?";
            // PATH  (optional)
            //regex += "(\/([a-z0-9+\$_-]\.?)+)*\/?";
            // GET Query (optional)
            //regex += "(\?[a-z+&\$_.-][a-z0-9;:@/&%=+\$_.-]*)?";
            // ANCHOR (optional)
            //regex += "(#[a-z_.-][a-z0-9+\$_.-]*)?\$";
            
            // check
    		Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    		Matcher matcher = pattern.matcher(s);
    		if (matcher.find()) { return true; }
    		return false;
    }	
	
	
	public static boolean hasUrl(String url) {
		Pattern p = Pattern.compile(urlPattern, Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(url);
		if (m.find()) return true;
		return false;
	}
	
	public static String getDomain(String url) {
		Pattern p = Pattern.compile(urlPattern, Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(url);
		while (m.find()) {
			return m.group(9) + "." + m.group(10) ;
		}
			
		return null;
	}
	
	public static String removeUrl(String s) {
		Pattern p = Pattern.compile(urlPattern, Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(s);
		while (m.find()) {
			s = s.replaceAll(m.group(0), "");
		}
		return s.trim();
	}
	
	public static String cleanUrl(String url) {
		// remove session id
		String u = url.replaceAll("[?&]\\w+=\\w{32}$$", "").trim();
		// remove ./
		if (!u.contains("../")) {
			u = u.replaceAll("\\./", "");			
		}
		return u;
	}
	
	public static String getLink(URL url, String str) throws MalformedURLException {
		if (str == null) throw new MalformedURLException();
		
		// ?go=home
		if (!str.equals("") && str.startsWith("?")) {
			String base = "";
			if (url.toString().indexOf("?") != -1) 
				base = url.toString().substring(0, url.toString().indexOf("?"));
			else 
				base = url.toString();
			
			str = base + str;			
		}
		URL l = new URL(url, str);
		return l.toString(); 
	}	
	
	public static String getFileName(String url, boolean withExt) {
		int slashIndex = url.lastIndexOf('/');
		String filename = null;
		if (withExt) {
			int questionIndex = url.indexOf('?', slashIndex);
			if (questionIndex == -1) filename = url.substring(slashIndex + 1);		
			else filename = url.substring(slashIndex + 1, questionIndex);
		} else {
			int dotIndex = url.indexOf('.', slashIndex);
			if (dotIndex == -1) filename = url.substring(slashIndex + 1);
			else filename = url.substring(slashIndex + 1, dotIndex);
		}
		return filename;
	}

	public static boolean isImageLink(String url) {
		if (url == null) return false;
		return (url.endsWith("jpg") || url.endsWith("jpeg") || url.endsWith("png") || url.endsWith("gif"));
	}
	
	public static boolean isAudioLink(String url) {
		return (url.endsWith("mp3") || url.endsWith("wav") || url.endsWith("m4a"));
	}
	
	public static boolean isVideoLink(String url) {
		return (url.endsWith("mp4"));
	}
	
	public static boolean isWordDocument(String url) {
		return (url.endsWith("doc") || url.endsWith("docx"));
	}
	
	public static boolean isPowerPoint(String url) {
		return (url.endsWith("ppt") || url.endsWith("pptx"));
	}
	
	public static boolean isPdf(String url) {
		return (url.endsWith("pdf"));
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
	
	// url = file path or whatever suitable URL you want.
	public static String getMimeType(String url) {
	    String type = null;
	    String extension = MimeTypeMap.getFileExtensionFromUrl(url);
	    if (extension != null) {
	        MimeTypeMap mime = MimeTypeMap.getSingleton();
	        type = mime.getMimeTypeFromExtension(extension);
	    }
	    return type;
	}	
	
}
