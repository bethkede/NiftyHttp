package com.nifty.http;

import android.os.SystemClock;
import com.nifty.http.cache.Cache;

import java.util.Queue;

/**
 * Created by BaoRui on 2014/12/15.
 */
public class CacheDispatcher {

	private static final int DEFAULT_CORE_POOL_SIZE = 1;

	private final Cache mCache;

	private final ResponseDelivery mDelivery;

	private final NetworkDispatcher networkDispatcher;

	private final PriorityThreadPool executor;

	public CacheDispatcher(NetworkDispatcher networkDispatcher, Cache mCache, ResponseDelivery mDelivery) {
		this.networkDispatcher = networkDispatcher;
		this.mCache = mCache;
		this.mDelivery = mDelivery;
		executor = new PriorityThreadPool(DEFAULT_CORE_POOL_SIZE);
		init();

	}

	//TODO  sync
	private synchronized void init() {
		new Thread() {
			@Override public void run() {
				mCache.initialize();
			}
		}.start();

	}

	public void addAll(Queue<Request> requests) {
		for (Request request : requests) {
			add(request);
		}
	}

	public synchronized void add(final Request request) {
		if (request == null) {
			throw new NullPointerException("request is null");
		}
		executor.execute(new Runnable() {
			@Override public void run() {
				//	addTrafficStatsTag(request);
				long startTimeMs = SystemClock.elapsedRealtime();
				// If the request has been canceled, don't bother dispatching it.
				if (request.isCanceled()) {
					return;
				}
				// Attempt to retrieve this item from cache.
				Cache.Entry entry = mCache.get(request.getCacheKey());
				if (entry == null) {
					// Cache miss; send off to the network dispatcher.
					networkDispatcher.add(request);
					return;
				}
				// If it is completely expired, just send it to the network.
				if (entry.isExpired()) {
					request.setCacheEntry(entry);
					networkDispatcher.add(request);
					return;
				}

				Response response = request.parseNetworkResponse(
						new NetworkResponse(entry.data, entry.responseHeaders));

				if (!entry.refreshNeeded()) {
					// Completely unexpired cache hit. Just deliver the response.
					mDelivery.postResponse(request, response);
				} else {
					// Soft-expired cache hit. We can deliver the cached response,
					// but we need to also send the request to the network for
					// refreshing.
					request.setCacheEntry(entry);

					// Mark the response as intermediate.
					response.intermediate = true;

					// Post the intermediate response back to the user and have
					// the delivery then forward the request along to the network.
					mDelivery.postResponse(request, response, new Runnable() {
						@Override
						public void run() {
							try {
								mNetworkQueue.put(request);
							} catch (InterruptedException e) {
								// Not much we can do about this.
							}
						}
					});
				}

			}
		}, request.getPriority());
	}

	public void stop() {
		if (executor != null) {
			executor.stop();
		}
	}

}
