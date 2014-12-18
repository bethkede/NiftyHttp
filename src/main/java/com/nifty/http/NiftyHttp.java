package com.nifty.http;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.nifty.http.cache.Cache;
import com.nifty.http.cache.HttpDiskCache;

import java.io.File;
import java.util.*;

/**
 * Created by BaoRui on 2014/12/9.
 */
public class NiftyHttp {

	public static NiftyHttp instance;

	public static NiftyHttp getInstance(Context context) {
		if (instance == null) {
			instance = new NiftyHttp(context);
		}
		return instance;
	}

	private static final String DEFAULT_CACHE_DIR = "volley";

	private final Cache mCache;

	private final BasicNetwork mNetwork;

	private final Set<Request> mCurrentRequests = new HashSet<Request>();
	private final Map<String, Queue<Request>> mWaitingRequests =
			new HashMap<String, Queue<Request>>();

	private final NetworkDispatcher networkDispatcher;
	private final CacheDispatcher cacheDispatcher;
	private final ResponseDelivery mDelivery;

	public interface RequestFilter {
		public boolean apply(Request request);
	}

	private NiftyHttp(Context context) {
		File cacheDir = new File(context.getCacheDir(), DEFAULT_CACHE_DIR);
		HurlStack stack = null;
		if (Build.VERSION.SDK_INT >= 9) {
			stack = new HurlStack();
		} else {
			throw new RuntimeException();
		}

		mNetwork = new BasicNetwork(stack);

		mCache = new HttpDiskCache(cacheDir);
		mDelivery = new ExecutorDelivery(new Handler(
				Looper.getMainLooper()));

		networkDispatcher = new NetworkDispatcher(mNetwork, mCache, mDelivery);

		cacheDispatcher = new CacheDispatcher(networkDispatcher, mCache, mDelivery);

		Log.e("x", "----------init---------");

	}

	/**
	 * Adds a Request to the dispatch queue.
	 *
	 * @param request The request to service
	 * @return The passed-in request
	 */
	public Request add(Request request) {
		// Tag the request as belonging to this queue and add it to the set of current requests.
		//		request.setRequestQueue(this);
		synchronized (mCurrentRequests) {
			mCurrentRequests.add(request);
		}

		request.setNiftyHttp(this);

		// If the request is uncacheable, skip the cache queue and go straight to the network.
		if (!request.shouldCache() && !request.isCacheLastResponse()) {
			Log.e("x", "---add to net--------");
			networkDispatcher.add(request);
			return request;
		}

		//重复请求，等待一次请求完成之后，在放入其他的请求(等待缓存写完)
		// Insert request into stage if there's already a request with the same cache key in flight.
		synchronized (mWaitingRequests) {
			String cacheKey = request.getCacheKey();
			if (mWaitingRequests.containsKey(cacheKey)) {

				Log.e("x", "---add to waitting--------");
				// There is already a request in flight. Queue up.
				Queue<Request> stagedRequests = mWaitingRequests.get(cacheKey);
				if (stagedRequests == null) {
					stagedRequests = new LinkedList<Request>();
				}
				stagedRequests.add(request);
				mWaitingRequests.put(cacheKey, stagedRequests);

			} else {
				// Insert 'null' queue for this cacheKey, indicating there is now a request in
				// flight.
				mWaitingRequests.put(cacheKey, null);
				Log.e("x", "---add to cache--------");
				cacheDispatcher.add(request);
			}
			return request;
		}
	}

	/**
	 * Called from  indicating that processing of the given request
	 * has finished.
	 * <p/>
	 * <p>Releases waiting requests for <code>request.getCacheKey()</code> if
	 * <code>request.shouldCache()</code>.</p>
	 */
	public void finish(Request request) {
		// Remove from the set of requests currently being processed.
		Log.e("x", "----------finish---------");
		synchronized (mCurrentRequests) {
			mCurrentRequests.remove(request);
		}

		if (request.shouldCache() || request.isCacheLastResponse()) {
			synchronized (mWaitingRequests) {
				String cacheKey = request.getCacheKey();
				Queue<Request> waitingRequests = mWaitingRequests.remove(cacheKey);
				if (waitingRequests != null) {

					Log.e("x", "----------waiting request add---------");

					// Process all queued up requests. They won't be considered as in flight, but
					// that's not a problem as the cache has been primed by 'request'.
					cacheDispatcher.addAll(waitingRequests);
				}
			}
		}
	}

	public void cancelAll(RequestFilter filter) {
		synchronized (mCurrentRequests) {
			for (Request request : mCurrentRequests) {
				if (filter.apply(request)) {
					request.cancel();
				}
			}
		}
	}

	public void cancelAll(final Object tag) {
		if (tag == null) {
			throw new IllegalArgumentException("Cannot cancelAll with a null tag");
		}
		cancelAll(new RequestFilter() {
			@Override
			public boolean apply(Request request) {
				return request.getTag() == tag;
			}
		});
	}

	public void stop() {
		networkDispatcher.stop();
		cacheDispatcher.stop();
	}

}
