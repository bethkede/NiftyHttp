package com.nifty.http;

import android.text.TextUtils;
import com.nifty.http.cache.Cache;
import com.nifty.http.error.VolleyError;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by BaoRui on 2014/12/9.
 */
public class Request {

	private static final String DEFAULT_PARAMS_ENCODING = "UTF-8";

	public String getBoundary() {
		return boundary;
	}

	private String boundary = "3i2ndDfv2rTHiSisAbouNdArYfORhtTPEefj3q2f";


	/**
	 * The default socket timeout in milliseconds
	 */
	public static final int DEFAULT_TIMEOUT_MS = 2500;

	private final Method mMethod;
	private Priority priority;
	private final String mUrl;
	private final Map<String, String> params;
	private final Map<String, String> headers;

	public List<FileParam> getFileParams() {
		return fileParams;
	}

	private final List<FileParam> fileParams;
	private boolean mShouldCache;
	private boolean isCacheLastResponse;
	private RetryPolicy mRetryPolicy;
	private Object mTag;

	private final Response.ResponseListener listener;

	private final Response.CacheListener cacheListener;
	//sets
	private boolean mCanceled = false;
	private Cache.Entry mCacheEntry = null;

	private boolean isClearCacheRequest = false;

	private NiftyHttp niftyHttp;

	private Request(Builder builder) {
		mMethod = builder.mMethod;
		priority = builder.priority;
		mUrl = builder.urlString;
		mShouldCache = builder.mShouldCache;
		isCacheLastResponse = builder.isCacheLastResponse;
		mRetryPolicy = builder.mRetryPolicy;
		mTag = builder.mTag;
		params = builder.params;
		fileParams = builder.fileParams;
		headers = builder.headers;
		listener = builder.listener;
		cacheListener = builder.cacheListener;

	}

	private Request(ClearCacheBuilder builder) {
		mMethod = builder.mMethod;
		priority = builder.priority;
		mUrl = builder.urlString;
		mShouldCache = builder.mShouldCache;
		isCacheLastResponse = builder.isCacheLastResponse;
		mRetryPolicy = builder.mRetryPolicy;
		mTag = builder.mTag;
		params = builder.params;
		fileParams = builder.fileParams;
		headers = builder.headers;
		listener = builder.listener;
		cacheListener = builder.cacheListener;
		isClearCacheRequest = true;

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
		return mUrl + "?" + getPramsBodyForUrl();
	}

	public String getCacheKey() {
		String body = getPramsBodyForUrl();
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
		if (fileParams != null && fileParams.size() > 0) {
			return "multipart/form-data;boundary=" + getBoundary();
		} else {
			return "application/x-www-form-urlencoded; charset=" + getParamsEncoding();
		}

	}

	public final boolean shouldCache() {
		return mShouldCache;
	}

	public final boolean isCacheLastResponse() {
		return isCacheLastResponse;
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

	public String getPramsBodyForUrl() {
		if (params != null && params.size() > 0) {
			return encodeParametersNoFile(params, getParamsEncoding());
		}
		return null;
	}

	public byte[] getParamsBodyHaveFile() {
		if (params != null && params.size() > 0) {
			try {
				return encodeParametersHaveFile(params, getParamsEncoding(), getBoundary())
						.getBytes(getParamsEncoding());
			} catch (UnsupportedEncodingException uee) {
			}
		}
		return null;
	}

	public byte[] getParamsBodyNoFile() {
		if (params != null && params.size() > 0) {
			try {
				return encodeParametersNoFile(params, getParamsEncoding()).getBytes(getParamsEncoding());
			} catch (UnsupportedEncodingException uee) {
			}
		}
		return null;
	}

	/**
	 * Converts <code>params</code> into an application/x-www-form-urlencoded encoded string.
	 */
	private String encodeParametersHaveFile(Map<String, String> params, String paramsEncoding, String boundary) {
		StringBuilder encodedParams = new StringBuilder();
		try {
			for (Map.Entry<String, String> entry : params.entrySet()) {
				encodedParams.append("Content-Disposition: form-data; name=\"");
				encodedParams.append(URLEncoder.encode(entry.getKey(), paramsEncoding));
				encodedParams.append("\"\r\n\r\n");
				encodedParams.append(URLEncoder.encode(entry.getValue(), paramsEncoding));
				encodedParams.append("\r\n--");
				encodedParams.append(boundary);
				encodedParams.append("\r\n");

			}
			return encodedParams.toString();
		} catch (UnsupportedEncodingException uee) {

		}
		return "";
	}

	/**
	 * Converts <code>params</code> into an application/x-www-form-urlencoded encoded string.
	 */
	private String encodeParametersNoFile(Map<String, String> params, String paramsEncoding) {
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

	public void setNiftyHttp(NiftyHttp niftyHttp) {
		this.niftyHttp = niftyHttp;
	}

	void finish(final String tag) {
		if (niftyHttp != null) {
			niftyHttp.finish(this);
		}
	}

	public boolean isClearCacheRequest() {
		return isClearCacheRequest;
	}

	public void cancel() {
		mCanceled = true;
	}

	public Request setCacheEntry(Cache.Entry entry) {
		mCacheEntry = entry;
		return this;
	}

	public void deliverResponse(Response response) {
		if (listener != null) {
			listener.onFinish(response);
		}
	}

	public void deliverError(VolleyError error) {
		if (listener != null) {
			listener.onError(error);
		}
	}

	public void deliverCacheResponse(Response response) {
		if (cacheListener != null) {
			cacheListener.onCache(response);
		}
	}

	public void deliverNoCache() {
		if (cacheListener != null) {
			cacheListener.onNoCache();
		}
	}

//	public static class DownloadBuilder {
//
//		//		public final static String CLEAR_CACHE="CLEAR_CACHE";
//
//		private String urlString;
//		private Method mMethod;
//		private Priority priority;
//		private Map<String, String> params;
//		private List<FileParam> fileParams;
//		private Map<String, String> headers;
//		private boolean mShouldCache;
//		private boolean isCacheLastResponse;
//		private int timeoutMs = -1;
//		private int retriesTime = -1;
//		private RetryPolicy mRetryPolicy;
//		private Object mTag;
//		private Response.ResponseListener listener;
//		private Response.CacheListener cacheListener;
//
//		public DownloadBuilder(String url) {
//			this.mMethod = Method.GET;
//			this.priority = Priority.NORMAL;
//			this.mShouldCache = false;
//			this.isCacheLastResponse = false;
//			params = new HashMap<String, String>();
//			headers = new HashMap<String, String>();
//			fileParams = new LinkedList<FileParam>();
//			this.mShouldCache = false;
//			this.mShouldCache = false;
//
//			if (url == null) {
//				throw new IllegalArgumentException("url == null");
//			}
//			urlString = url;
//		}
//
//		public DownloadBuilder method(Method method) {
//			if (method != Method.GET || method != Method.POST) {
//				throw new IllegalArgumentException("method not get or post");
//			}
//
//			this.mMethod = method;
//			return this;
//		}
//
//		public DownloadBuilder priority(Priority priority) {
//			this.priority = priority;
//			return this;
//		}
//
//		public Builder isCacheLastResponse(boolean isCacheLastResponse) {
//			this.isCacheLastResponse = isCacheLastResponse;
//			return this;
//		}
//
//		public Builder shouldCache(boolean mShouldCache) {
//			this.mShouldCache = mShouldCache;
//			return this;
//		}
//
//		public Builder tag(int mTag) {
//			this.mTag = mTag;
//			return this;
//		}
//
//		public Builder timeoutMs(int timeoutMs) {
//			this.timeoutMs = timeoutMs;
//			return this;
//		}
//
//		public Builder retriesTime(int retriesTime) {
//			this.retriesTime = retriesTime;
//			return this;
//		}
//
//		public Builder header(String name, String value) {
//			if (TextUtils.isEmpty(name)) {
//				return this;
//			}
//			headers.put(name, value);
//			return this;
//		}
//
//		public Builder params(String name, String value) {
//			if (TextUtils.isEmpty(name)) {
//				return this;
//			}
//			params.put(name, value);
//			return this;
//		}
//
//		public Builder fileParams(String key, String path) {
//			if (TextUtils.isEmpty(path)) {
//				return this;
//			}
//			File file = new File(path);
//			fileParams(key, file);
//			return this;
//		}
//
//		public Builder fileParams(String key, File file) {
//			if (file == null) {
//				return this;
//			}
//			fileParams.add(new FileParam(key, file));
//			return this;
//		}
//
//		public Builder listener(Response.ResponseListener listener) {
//			this.listener = listener;
//			return this;
//		}
//
//		public Builder cacheListener(Response.CacheListener cacheListener) {
//			this.cacheListener = cacheListener;
//			return this;
//		}
//
//		public Request build() {
//			if (urlString == null) {
//				throw new IllegalStateException("url == null");
//			}
//			this.mRetryPolicy = new DefaultRetryPolicy(
//					timeoutMs == -1 ? DefaultRetryPolicy.DEFAULT_TIMEOUT_MS : timeoutMs,
//					retriesTime == -1 ? DefaultRetryPolicy.DEFAULT_MAX_RETRIES : retriesTime);
//
//			return new Request(this);
//		}
//	}

	public static class ClearCacheBuilder {

		private String urlString;
		private Method mMethod;
		private Priority priority;
		private Map<String, String> params;
		private Map<String, String> headers;
		private List<FileParam> fileParams;
		private boolean mShouldCache;
		private boolean isCacheLastResponse;
		private int timeoutMs = -1;
		private int retriesTime = -1;
		private RetryPolicy mRetryPolicy;
		private Object mTag;
		private Response.ResponseListener listener;
		private Response.CacheListener cacheListener;

		public ClearCacheBuilder() {
			this.mMethod = Method.GET;
			this.priority = Priority.NORMAL;
			this.mShouldCache = false;
			this.isCacheLastResponse = false;
			fileParams = new LinkedList<FileParam>();
			params = new HashMap<String, String>();
			headers = new HashMap<String, String>();
			this.mShouldCache = false;
			this.mShouldCache = false;
			urlString = "";
		}

		public Request build() {

			return new Request(this);
		}
	}

	public static class Builder {

		//		public final static String CLEAR_CACHE="CLEAR_CACHE";

		private String urlString;
		private Method mMethod;
		private Priority priority;
		private Map<String, String> params;
		private List<FileParam> fileParams;
		private Map<String, String> headers;
		private boolean mShouldCache;
		private boolean isCacheLastResponse;
		private int timeoutMs = -1;
		private int retriesTime = -1;
		private RetryPolicy mRetryPolicy;
		private Object mTag;
		private Response.ResponseListener listener;
		private Response.CacheListener cacheListener;

		public Builder(String url) {
			this.mMethod = Method.GET;
			this.priority = Priority.NORMAL;
			this.mShouldCache = false;
			this.isCacheLastResponse = false;
			params = new HashMap<String, String>();
			headers = new HashMap<String, String>();
			fileParams = new LinkedList<FileParam>();
			this.mShouldCache = false;
			this.mShouldCache = false;

			if (url == null) {
				throw new IllegalArgumentException("url == null");
			}
			urlString = url;
		}

		public Builder method(Method method) {
			this.mMethod = method;
			return this;
		}

		public Builder priority(Priority priority) {
			this.priority = priority;
			return this;
		}

		public Builder isCacheLastResponse(boolean isCacheLastResponse) {
			this.isCacheLastResponse = isCacheLastResponse;
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

		public Builder fileParams(String key, String path) {
			if (TextUtils.isEmpty(path)) {
				return this;
			}
			File file = new File(path);
			fileParams(key, file);
			return this;
		}

		public Builder fileParams(String key, File file) {
			if (file == null) {
				return this;
			}
			fileParams.add(new FileParam(key, file));
			return this;
		}

		public Builder listener(Response.ResponseListener listener) {
			this.listener = listener;
			return this;
		}

		public Builder cacheListener(Response.CacheListener cacheListener) {
			this.cacheListener = cacheListener;
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
