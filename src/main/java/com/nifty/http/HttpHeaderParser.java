package com.nifty.http;

import com.nifty.http.cache.Cache;
import org.apache.http.impl.cookie.DateParseException;
import org.apache.http.impl.cookie.DateUtils;
import org.apache.http.protocol.HTTP;

import java.util.Map;

/**
 * Created by BaoRui on 2014/12/17.
 */
public class HttpHeaderParser {

	//Expires 表示存在时间，允许客户端在这个时间之前不去检查（发请求），等同max-age的
	//效果。但是如果同时存在，则被Cache-Control的max-age覆盖。

	/**
	 * Extracts a {@link com.nifty.http.cache.Cache.Entry} from a {@link com.nifty.http.NetworkResponse}.
	 *
	 * @param response The network response to parse headers from
	 * @return a cache entry for the given response, or null if the response is not cacheable.
	 */
	public static Cache.Entry parseCacheHeaders(NetworkResponse response, boolean shouldCache, boolean wayward) {
		long now = System.currentTimeMillis();

		boolean haveCache = true;

		Map<String, String> headers = response.headers;

		long serverDate = 0;
		long lastModified = 0;
		long serverExpires = 0;
		long softExpire = 0;
		long maxAge = 0;
		boolean hasCacheControl = false;
		String serverEtag = null;
		//--------------------
		if (shouldCache) {
			String headerValue;
			headerValue = headers.get("Date");
			if (headerValue != null) {
				serverDate = parseDateAsEpoch(headerValue);
			}

			//	    Expires 表示存在时间，允许客户端在这个时间之前不去检查（发请求），等同max-age的
			//	    效果。但是如果同时存在，则被Cache-Control的max-age覆盖

			headerValue = headers.get("Cache-Control");

			if (headerValue != null) {
				hasCacheControl = true;
				String[] tokens = headerValue.split(",");
				for (int i = 0; i < tokens.length; i++) {
					String token = tokens[i].trim();

					if (token.equals("no-cache") || token.equals("no-store")) {
						haveCache = false;

					} else if (token.startsWith("max-age=")) {
						try {
							maxAge = Long.parseLong(token.substring(8));
						} catch (Exception e) {
						}
					} else if (token.equals("must-revalidate") || token.equals("proxy-revalidate")) {
						maxAge = 0;
					}
				}
			}

			headerValue = headers.get("Expires");

			if (headerValue != null) {
				serverExpires = parseDateAsEpoch(headerValue);
			}

			headerValue = headers.get("Last-Modified");
			if (headerValue != null) {
				lastModified = parseDateAsEpoch(headerValue);
			}

			serverEtag = headers.get("ETag");

			// Cache-Control takes precedence over an Expires header, even if both exist and Expires
			// is more restrictive.
			if (hasCacheControl) {
				softExpire = now + maxAge * 1000;
			} else if (serverDate > 0 && serverExpires >= serverDate) {
				// Default semantic for Expire header in HTTP specification is softExpire.
				softExpire = now + (serverExpires - serverDate);
			}
		}

		Cache.Entry entry = new Cache.Entry();

		if (shouldCache && haveCache) {
			entry.data = response.data;
			entry.etag = serverEtag;
			entry.ttl = softExpire;
			entry.wayward = 1;
			entry.lastModified = lastModified;
			entry.responseHeaders = headers;
			return entry;
		} else if (wayward) {
			entry.data = response.data;
			entry.etag = "";
			entry.ttl = 0;
			entry.wayward = 0;
			entry.lastModified = 0;
			entry.responseHeaders = headers;
			return entry;
		} else {
			return null;
		}

	}

	/**
	 * Parse date in RFC1123 format, and return its value as epoch
	 */
	public static long parseDateAsEpoch(String dateStr) {
		try {
			// Parse date in RFC1123 format if this header contains one
			return DateUtils.parseDate(dateStr).getTime();
		} catch (DateParseException e) {
			// Date in invalid format, fallback to 0
			return 0;
		}
	}

	/**
	 * Returns the charset specified in the Content-Type of this header,
	 * or the HTTP default (ISO-8859-1) if none can be found.
	 */
	public static String parseCharset(Map<String, String> headers) {
		String contentType = headers.get(HTTP.CONTENT_TYPE);
		if (contentType != null) {
			String[] params = contentType.split(";");
			for (int i = 1; i < params.length; i++) {
				String[] pair = params[i].trim().split("=");
				if (pair.length == 2) {
					if (pair[0].equals("charset")) {
						return pair[1];
					}
				}
			}
		}

		return HTTP.DEFAULT_CONTENT_CHARSET;
	}
}

