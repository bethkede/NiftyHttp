package com.nifty.http;

import android.os.SystemClock;
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
						return;
					}

					NetworkResponse networkResponse = mNetwork.performRequest(request);

					Response response = request.parseNetworkResponse(networkResponse);

					if (request.shouldCache() && response.cacheEntry != null) {
						mCache.put(request.getCacheKey(), response.cacheEntry);
					}

				} catch (VolleyError volleyError) {
					volleyError.setNetworkTimeMs(SystemClock.elapsedRealtime() - startTimeMs);
					parseAndDeliverNetworkError(request, volleyError);
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		}, request.getPriority());
	}

	private void parseAndDeliverNetworkError(Request request, VolleyError error) {
		error = request.parseNetworkError(error);
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
