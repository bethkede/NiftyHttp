package com.nifty.http;

import android.text.TextUtils;
import com.nifty.http.cache.Cache;
import com.nifty.http.error.VolleyError;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by BaoRui on 2014/12/9.
 */
public class Request {

	private static final String DEFAULT_PARAMS_ENCODING = "UTF-8";

	/**
	 * The default socket timeout in milliseconds
	 */
	public static final int DEFAULT_TIMEOUT_MS = 2500;

	private final Method mMethod;
	private Priority priority;
	private final String mUrl;
	private final Map<String, String> params;
	private final Map<String, String> headers;
	private boolean mShouldCache = true;
	private RetryPolicy mRetryPolicy;
	private Object mTag;
	private final Response.ErrorListener mErrorListener;
	private final Response.FinishListener mFinishListener;
	//sets
	private boolean mCanceled = false;
	private boolean mResponseDelivered = false;
	private Cache.Entry mCacheEntry = null;

	private Request(Builder builder) {
		mMethod = builder.mMethod;
		priority = builder.priority;
		mUrl = builder.urlString;
		mShouldCache = builder.mShouldCache;
		mRetryPolicy = builder.mRetryPolicy;
		mTag = builder.mTag;
		params = builder.params;
		headers = builder.headers;
		mErrorListener = builder.mErrorListener;
		mFinishListener = builder.mFinishListener;

	}

	public Method getMethod() {
		return mMethod;
	}

	public Object getTag() {
		return mTag;
	}

	public String getUrl() {
		//		POST,PUT,PATCH
		if (mMethod == Method.POST || mMethod == Method.PUT || mMethod == Method.PATCH) {
			return mUrl;
		}
		return mUrl + "?" + getPramsBody();
	}

	public String getCacheKey() {
		String body = getPramsBody();
		return body == null ? getUrl() : getUrl() + body.toString();
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public Cache.Entry getCacheEntry() {
		return mCacheEntry;
	}

	public boolean isCanceled() {
		return mCanceled;
	}

	public String getBodyContentType() {
		return "application/x-www-form-urlencoded; charset=" + getParamsEncoding();
	}

	public final boolean shouldCache() {
		return mShouldCache;
	}

	public Priority getPriority() {
		return priority;
	}

	public final int getTimeoutMs() {
		return mRetryPolicy.getCurrentTimeout();
	}

	public RetryPolicy getRetryPolicy() {
		return mRetryPolicy;
	}

	public boolean hasHadResponseDelivered() {
		return mResponseDelivered;
	}

	public String getPramsBody() {
		if (params != null && params.size() > 0) {
			return encodeParameters(params, getParamsEncoding());
		}
		return null;
	}

	public byte[] getBody() {
		if (params != null && params.size() > 0) {
			try {
				return encodeParameters(params, getParamsEncoding()).getBytes(getParamsEncoding());
			} catch (UnsupportedEncodingException uee) {
			}
		}
		return null;
	}

	/**
	 * Converts <code>params</code> into an application/x-www-form-urlencoded encoded string.
	 */
	private String encodeParameters(Map<String, String> params, String paramsEncoding) {
		StringBuilder encodedParams = new StringBuilder();
		try {
			for (Map.Entry<String, String> entry : params.entrySet()) {
				encodedParams.append(URLEncoder.encode(entry.getKey(), paramsEncoding));
				encodedParams.append('=');
				encodedParams.append(URLEncoder.encode(entry.getValue(), paramsEncoding));
				encodedParams.append('&');
			}
			return encodedParams.toString();
		} catch (UnsupportedEncodingException uee) {

		}
		return "";
	}

	//--------------------------------------------------------------------------

	void finish(final String tag) {
		if (mRequestQueue != null) {
			mRequestQueue.finish(this);
		}

	}

	public void markDelivered() {
		mResponseDelivered = true;
	}

	public void cancel() {
		mCanceled = true;
	}

	public Request setCacheEntry(Cache.Entry entry) {
		mCacheEntry = entry;
		return this;
	}

	protected void deliverResponse(Response response) {
		if (mFinishListener != null) {
			mFinishListener.onResponse(response);
		}
	}

	public void deliverError(VolleyError error) {
		if (mErrorListener != null) {
			mErrorListener.onErrorResponse(error);
		}
	}

	protected Response parseNetworkResponse(NetworkResponse networkResponse) {
		Response response = new Response(networkResponse.data, networkResponse.headers, networkResponse.statusCode);
		return response;

	}

	protected VolleyError parseNetworkError(VolleyError volleyError) {
		return volleyError;
	}

	public static class Builder {

		private String urlString;
		private Method mMethod;
		private Priority priority;
		private Map<String, String> params;
		private Map<String, String> headers;
		private boolean mShouldCache;
		private int timeoutMs = -1;
		private int retriesTime = -1;
		private RetryPolicy mRetryPolicy;
		private Object mTag;
		private Response.ErrorListener mErrorListener;
		private Response.FinishListener mFinishListener;

		public Builder() {
			this.mMethod = Method.GET;
			this.priority = Priority.NORMAL;
			this.mShouldCache = false;
			params = new HashMap<String, String>();
			headers = new HashMap<String, String>();
			this.mShouldCache = false;
			this.mShouldCache = false;
		}

		public Builder url(String url) {
			if (url == null) {
				throw new IllegalArgumentException("url == null");
			}
			urlString = url;
			return this;
		}

		public Builder url(URL url) {
			if (url == null) {
				throw new IllegalArgumentException("url == null");
			}
			this.urlString = url.toString();
			return this;
		}

		public Builder method(Method method) {
			this.mMethod = method;
			return this;
		}

		public Builder priority(Priority priority) {
			this.priority = priority;
			return this;
		}

		public Builder shouldCache(boolean mShouldCache) {
			this.mShouldCache = mShouldCache;
			return this;
		}

		public Builder tag(int mTag) {
			this.mTag = mTag;
			return this;
		}

		public Builder timeoutMs(int timeoutMs) {
			this.timeoutMs = timeoutMs;
			return this;
		}

		public Builder retriesTime(int retriesTime) {
			this.retriesTime = retriesTime;
			return this;
		}

		public Builder header(String name, String value) {
			if (TextUtils.isEmpty(name)) {
				return this;
			}
			headers.put(name, value);
			return this;
		}

		public Builder params(String name, String value) {
			if (TextUtils.isEmpty(name)) {
				return this;
			}
			params.put(name, value);
			return this;
		}

		public Builder errorListener(Response.ErrorListener mErrorListener) {
			this.mErrorListener = mErrorListener;
			return this;
		}

		public Builder finishListener(Response.FinishListener mFinishListener) {
			this.mErrorListener = mErrorListener;
			return this;
		}

		public Request build() {
			if (urlString == null) {
				throw new IllegalStateException("url == null");
			}
			this.mRetryPolicy = new DefaultRetryPolicy(
					timeoutMs == -1 ? DefaultRetryPolicy.DEFAULT_TIMEOUT_MS : timeoutMs,
					retriesTime == -1 ? DefaultRetryPolicy.DEFAULT_MAX_RETRIES : retriesTime);

			return new Request(this);
		}
	}

	protected static String getParamsEncoding() {
		return DEFAULT_PARAMS_ENCODING;
	}

}
