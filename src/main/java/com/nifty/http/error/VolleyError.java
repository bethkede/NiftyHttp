package com.nifty.http.error;

import com.nifty.http.NetworkResponse;

/**
 * Created by BaoRui on 2014/12/12.
 */
public class VolleyError extends Exception {
	public final NetworkResponse networkResponse;
	private long networkTimeMs;

	public VolleyError() {
		networkResponse = null;
	}

	public VolleyError(NetworkResponse response) {
		networkResponse = response;
	}

	public VolleyError(String exceptionMessage) {
		super(exceptionMessage);
		networkResponse = null;
	}

	public VolleyError(String exceptionMessage, Throwable reason) {
		super(exceptionMessage, reason);
		networkResponse = null;
	}

	public VolleyError(Throwable cause) {
		super(cause);
		networkResponse = null;
	}

	public void setNetworkTimeMs(long networkTimeMs) {
		this.networkTimeMs = networkTimeMs;
	}

	public long getNetworkTimeMs() {
		return networkTimeMs;
	}
}
