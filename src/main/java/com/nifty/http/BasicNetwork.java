package com.nifty.http;

import android.os.SystemClock;
import android.util.Log;
import com.nifty.http.cache.Cache;
import com.nifty.http.error.*;
import org.apache.http.*;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.cookie.DateUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.util.*;

/**
 * Created by BaoRui on 2014/12/11.
 */
public class BasicNetwork {

	protected final HttpStack mHttpStack;
	//	TODO 待研究
	//	protected final ByteArrayPool mPool;

	public BasicNetwork(HttpStack httpStack) {
		this.mHttpStack = httpStack;
	}

	public NetworkResponse performRequest(Request request) throws VolleyError {
		long requestStart = SystemClock.elapsedRealtime();
		while (true) {

			HttpResponse httpResponse = null;
			byte[] responseContents = null;
			Map<String, String> responseHeaders = Collections.emptyMap();
			try {

				// Gather headers.
				Map<String, String> headers = new HashMap<String, String>();
				addCacheHeaders(headers, request.getCacheEntry());
				httpResponse = mHttpStack.performRequest(request, headers);
				StatusLine statusLine = httpResponse.getStatusLine();
				int statusCode = statusLine.getStatusCode();
				responseHeaders = convertHeaders(httpResponse.getAllHeaders());
				// Handle cache validation.
				if (statusCode == HttpStatus.SC_NOT_MODIFIED) {
					Log.e("x", "----------statusCode 304---------");
					Cache.Entry entry = request.getCacheEntry();
					if (entry == null) {
						return new NetworkResponse(HttpStatus.SC_NOT_MODIFIED, null,
								responseHeaders, true,
								SystemClock.elapsedRealtime() - requestStart);
					}

					// A HTTP 304 response does not have all header fields. We
					// have to use the header fields from the cache entry plus
					// the new ones from the response.
					// http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.3.5
					entry.responseHeaders.putAll(responseHeaders);
					return new NetworkResponse(HttpStatus.SC_NOT_MODIFIED, entry.data,
							entry.responseHeaders, true,
							SystemClock.elapsedRealtime() - requestStart);
				}

				// Some responses such as 204s do not have content.  We must check.
				if (httpResponse.getEntity() != null) {
					responseContents = entityToBytes(httpResponse.getEntity());
				} else {
					// Add 0 byte response as a way of honestly representing a
					// no-content request.
					responseContents = new byte[0];
				}

				// if the request is slow, log it.
				long requestLifetime = SystemClock.elapsedRealtime() - requestStart;

				if (statusCode < 200 || statusCode > 299) {
					throw new IOException();
				}
				return new NetworkResponse(statusCode, responseContents, responseHeaders, false,
						SystemClock.elapsedRealtime() - requestStart);
			} catch (SocketTimeoutException e) {
				Log.e("x", "------------------SocketTimeoutException--------------------------");
				attemptRetryOnException("socket", request, new TimeoutError());
			} catch (ConnectTimeoutException e) {
				Log.e("x", "------------------ConnectTimeoutException--------------------------");
				attemptRetryOnException("connection", request, new TimeoutError());
			} catch (MalformedURLException e) {
				throw new RuntimeException("Bad URL " + request.getUrl(), e);
			} catch (IOException e) {
				int statusCode = 0;
				NetworkResponse networkResponse = null;
				if (httpResponse != null) {
					statusCode = httpResponse.getStatusLine().getStatusCode();
				} else {
					throw new NoConnectionError(e);
				}
				if (responseContents != null) {
					networkResponse = new NetworkResponse(statusCode, responseContents,
							responseHeaders, false, SystemClock.elapsedRealtime() - requestStart);
					if (statusCode == HttpStatus.SC_UNAUTHORIZED ||
							statusCode == HttpStatus.SC_FORBIDDEN) {
						attemptRetryOnException("auth",
								request, new AuthFailureError(networkResponse));
					} else {
						// TODO: Only throw ServerError for 5xx status codes.
						throw new ServerError(networkResponse);
					}
				} else {
					throw new NetworkError(networkResponse);
				}
			}
		}
	}

	private static void attemptRetryOnException(String logPrefix, Request request,
			VolleyError exception) throws VolleyError {
		RetryPolicy retryPolicy = request.getRetryPolicy();
		int oldTimeout = request.getTimeoutMs();

		try {
			retryPolicy.retry(exception);
		} catch (VolleyError e) {
			throw e;
		}
	}

	private void addCacheHeaders(Map<String, String> headers, Cache.Entry entry) {
		// If there's no cache entry, we're done.
		if (entry == null) {
			return;
		}
		if (entry.etag != null) {
			headers.put("If-None-Match", entry.etag);
		}
		if (entry.serverDate > 0) {
			Date refTime = new Date(entry.serverDate);
			headers.put("If-Modified-Since", DateUtils.formatDate(refTime));
		}
	}

	protected Map<String, String> convertHeaders(Header[] headers) {
		Map<String, String> result = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
		for (int i = 0; i < headers.length; i++) {
			result.put(headers[i].getName(), headers[i].getValue());
		}
		return result;
	}

	private byte[] entityToBytes(HttpEntity entity) throws IOException {

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		InputStream input = entity.getContent();
		byte[] buffer = new byte[4096];
		int n = 0;
		while (-1 != (n = input.read(buffer))) {
			output.write(buffer, 0, n);
		}
		return output.toByteArray();
	}

}
