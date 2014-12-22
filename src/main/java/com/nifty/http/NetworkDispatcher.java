package com.nifty.http;

import android.os.SystemClock;
import android.util.Log;
import com.nifty.http.cache.Cache;
import com.nifty.http.error.VolleyError;

/**
 * Created by BaoRui on 2014/12/16.
 */
public class NetworkDispatcher {

	private PriorityThreadPool executor;
	private final BasicNetwork mNetwork;
	private final Cache mCache;
	private final ResponseDelivery mDelivery;

	public NetworkDispatcher(BasicNetwork mNetwork, Cache mCache, ResponseDelivery mDelivery) {
		this(mNetwork, mCache, mDelivery, -1);
	}

	public NetworkDispatcher(BasicNetwork mNetwork, Cache mCache, ResponseDelivery mDelivery, int corePoolSize) {
		this.mNetwork = mNetwork;
		this.mCache = mCache;
		this.mDelivery = mDelivery;
		if (corePoolSize > 0) {
			executor = new PriorityThreadPool(corePoolSize);
		} else {
			executor = new PriorityThreadPool();
		}
	}

	public void add(final Request request) {
		if (request == null) {
			throw new NullPointerException("request is null");
		}
		executor.execute(new Runnable() {
			@Override public void run() {
				//	addTrafficStatsTag(request);
				long startTimeMs = SystemClock.elapsedRealtime();
				try {
					if (request.isCanceled()) {
						request.finish("network-discard-cancelled");
						return;
					}

					NetworkResponse networkResponse = mNetwork.performRequest(request);

					if (networkResponse.notModified) {

						Cache.Entry entry = mCache.get(request.getCacheKey());
						Response response = Response.parseNetworkResponse(
								new NetworkResponse(entry.data, entry.responseHeaders), request.shouldCache(),
								request.isCacheLastResponse());
						mDelivery.postResponse(request, response);

						if (request.shouldCache() && response.cacheEntry != null) {
							mCache.put(request.getCacheKey(), response.cacheEntry);
						}

						request.finish("not-modified");

						return;
					}

					Response response = Response.parseNetworkResponse(networkResponse,request.shouldCache() , request.isCacheLastResponse());

					if (response.cacheEntry != null && (request.shouldCache() || request.isCacheLastResponse())) {

						Log.e("x", "----------put to cache----------------");
						mCache.put(request.getCacheKey(), response.cacheEntry);
					}

					mDelivery.postResponse(request, response);

				} catch (VolleyError volleyError) {
					volleyError.setNetworkTimeMs(SystemClock.elapsedRealtime() - startTimeMs);
					parseAndDeliverNetworkError(request, volleyError);
				} catch (Exception e) {
					VolleyError volleyError = new VolleyError(e);
					volleyError.setNetworkTimeMs(SystemClock.elapsedRealtime() - startTimeMs);
					mDelivery.postError(request, volleyError);
				}

			}
		}, request.getPriority());
	}

	private void parseAndDeliverNetworkError(Request request, VolleyError error) {
		//		error = Response.parseNetworkError(error);
		mDelivery.postError(request, error);
	}

	public void stop() {
		if (executor != null) {
			executor.stop();
		}
	}

	//	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	//	private void addTrafficStatsTag(Request request) {
	//		// Tag the request (if API >= 14)
	//		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
	//			TrafficStats.setThreadStatsTag(request.getTrafficStatsTag());
	//		}
	//	}
}
