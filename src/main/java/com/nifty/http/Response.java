package com.nifty.http;

import com.nifty.http.cache.Cache;
import com.nifty.http.error.VolleyError;

import java.io.UnsupportedEncodingException;
import java.util.Map;

/**
 * Created by BaoRui on 2014/12/9.
 */
public class Response {

	public interface ResponseListener {

		public void onFinish(Response response);

		public void onError(VolleyError error);
	}

	public interface CacheListener {

		public void onCache(Response response);

		public void onNoCache();
	}

	//.String  .json .image

	public int statusCode;

	public byte[] body;

	public Map<String, String> headers;

	public Cache.Entry cacheEntry;

	public boolean intermediate = false;

	public final VolleyError error;

	public boolean isSuccess() {
		return error == null;
	}

	public String toStr() {
		String result;
		try {
			result = new String(body, HttpHeaderParser.parseCharset(headers));
		} catch (UnsupportedEncodingException e) {
			result = new String(body);
		}

		return result;
	}

	private Response(NetworkResponse networkResponse, Cache.Entry cacheEntry) {
		body = networkResponse.data;
		headers = networkResponse.headers;
		statusCode = networkResponse.statusCode;
		error = null;
		this.cacheEntry = cacheEntry;
	}

	private Response(VolleyError error) {
		this.body = null;
		this.headers = null;
		this.cacheEntry = null;
		this.error = error;
	}

	public static Response error(VolleyError error) {
		return new Response(error);
	}

	public static Response success(NetworkResponse networkResponse, Cache.Entry cacheEntry) {
		return new Response(networkResponse, cacheEntry);
	}

	protected static Response parseNetworkResponse(NetworkResponse response, boolean shouldCache, boolean wayward) {

		return Response.success(response, HttpHeaderParser.parseCacheHeaders(response,shouldCache, wayward));

	}

	//	protected static VolleyError parseNetworkError(VolleyError volleyError) {
	//		return volleyError;
	//	}

}
