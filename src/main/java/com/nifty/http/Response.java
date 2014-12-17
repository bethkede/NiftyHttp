package com.nifty.http;

import com.nifty.http.cache.Cache;
import com.nifty.http.error.VolleyError;

import java.util.Map;

/**
 * Created by BaoRui on 2014/12/9.
 */
public class Response {

	//.String  .json .image

	public int statusCode;

	public byte[] body;

	public Map<String, String> headers;

	public Cache.Entry cacheEntry;

	public Response(byte[] body, Map<String, String> headers, int statusCode) {
		this.body = body;
		this.headers = headers;
		this.statusCode = statusCode;
	}

	/**
	 * Callback interface for delivering parsed responses.
	 */
	public interface FinishListener {
		/**
		 * Called when a response is received.
		 */
		public void onResponse(Response response);
	}

	/**
	 * Callback interface for delivering error responses.
	 */
	public interface ErrorListener {
		/**
		 * Callback method that an error has been occurred with the
		 * provided error code and optional user-readable message.
		 */
		public void onErrorResponse(VolleyError error);
	}

}
