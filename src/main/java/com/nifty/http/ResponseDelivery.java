package com.nifty.http;

import com.nifty.http.error.VolleyError;

/**
 * Created by BaoRui on 2014/12/16.
 */
public interface ResponseDelivery {
	/**
	 * Parses a response from the network or cache and delivers it.
	 */
	public void postResponse(Request request, Response response);

	public void postLastCache(Request request, Response response);

	public void postNoLastCache(Request request);

	/**
	 * Parses a response from the network or cache and delivers it. The provided
	 * Runnable will be executed after delivery.
	 */
	public void postResponse(Request request, Response response, Runnable runnable);

	/**
	 * Posts an error for the given request.
	 */
	public void postError(Request request, VolleyError error);
}

