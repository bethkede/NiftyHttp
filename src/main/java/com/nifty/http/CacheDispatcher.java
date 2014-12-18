package com.nifty.http;

import android.os.SystemClock;
import android.util.Log;
import com.nifty.http.cache.Cache;

import java.util.Queue;

/**
 * Created by BaoRui on 2014/12/15.
 */
public class CacheDispatcher {

	private static final int DEFAULT_CORE_POOL_SIZE = 4;

	private final Cache mCache;

	private final ResponseDelivery mDelivery;

	private final NetworkDispatcher networkDispatcher;

	private final PriorityThreadPool executor;

	private Object lock = new Object();

	public CacheDispatcher(NetworkDispatcher networkDispatcher, Cache mCache, ResponseDelivery mDelivery) {
		this.networkDispatcher = networkDispatcher;
		this.mCache = mCache;
		this.mDelivery = mDelivery;
		executor = new PriorityThreadPool(DEFAULT_CORE_POOL_SIZE);
		init();

	}

	//  sync
	private void init() {
		new Thread() {
			@Override public void run() {
				synchronized (lock) {
					mCache.initialize();
				}

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
				synchronized (lock) {
					//	addTrafficStatsTag(request);
					long startTimeMs = SystemClock.elapsedRealtime();
					// If the request has been canceled, don't bother dispatching it.
					if (request.isCanceled()) {
						request.finish("cache-discard-canceled");
						return;
					}

					// Attempt to retrieve this item from cache.
					Cache.Entry entry = mCache.get(request.getCacheKey());

					Log.e("x", "Cache entry    ==   " + entry);
					if (entry == null) {
						// Cache miss; send off to the network dispatcher.
						networkDispatcher.add(request);
						mDelivery.postNoLastCache(request);
						return;
					}

					Response response = Response.parseNetworkResponse(
							new NetworkResponse(entry.data, entry.responseHeaders), request.shouldCache(),
							request.isCacheLastResponse());

					if (request.isCacheLastResponse()) {
						mDelivery.postLastCache(request, response);
					}

					//只有2中情况能进来这种，一种 ttl 一种cache last
					//如果使用了last cache 已经返回了。
					//缓存如果使用ttl+etag，那么过去的请求已经加入net请求，没过期不需要请求。
					//如果没有使用ttl+etag，就在加入到网络请求一次
					//ttl  和softTtl 看代码是一样的，eoe的帖子说是因为解析图片之类的可能时间会唱导致过期，这次不考虑
					//缓存区分是否是last cache or ttl+etag cache

					if (request.shouldCache() && !entry.isWayward()) {
						// If it is completely expired, just send it to the network.

						Log.e("x", "etag      ==    " + entry.etag);
						Log.e("x", "serverDate      ==    " + entry.serverDate);
						Log.e("x", "ttl      ==    " + entry.ttl);

						if (entry.isExpired()) {
							Log.e("x", "------- ttl cache add net ---------");
							request.setCacheEntry(entry);
							networkDispatcher.add(request);
						} else {
							Log.e("x", "------- ttl cache hit ---------");
							mDelivery.postResponse(request, response);
						}

					} else {
						Log.e("x", "------- wayward cache add net ---------");
						networkDispatcher.add(request);
					}

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
