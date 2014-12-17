package com.nifty.http;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import com.nifty.http.cache.Cache;
import com.nifty.http.cache.DiskBasedCache;
import com.nifty.http.error.VolleyError;

import java.io.File;
import java.util.*;

/**
 * Created by BaoRui on 2014/12/9.
 */
public class NiftyHttp {

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

	public NiftyHttp(Context context) throws VolleyError {
		File cacheDir = new File(context.getCacheDir(), DEFAULT_CACHE_DIR);
		HurlStack stack = null;
		if (Build.VERSION.SDK_INT >= 9) {
			stack = new HurlStack();
		} else {
			throw new VolleyError("SDK VERSION < 9");
		}

		mNetwork = new BasicNetwork(stack);

		mCache = new DiskBasedCache(cacheDir);
		mDelivery = new ExecutorDelivery(new Handler(
				Looper.getMainLooper()));

		networkDispatcher = new NetworkDispatcher(mNetwork, mCache, mDelivery);

		cacheDispatcher = new CacheDispatcher(networkDispatcher, mCache, mDelivery);

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

		// If the request is uncacheable, skip the cache queue and go straight to the network.
		if (!request.shouldCache()) {
			networkDispatcher.add(request);
			return request;
		}

		// Insert request into stage if there's already a request with the same cache key in flight.
		synchronized (mWaitingRequests) {
			String cacheKey = request.getCacheKey();
			if (mWaitingRequests.containsKey(cacheKey)) {
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

		synchronized (mCurrentRequests) {
			mCurrentRequests.remove(request);
		}

		if (request.shouldCache()) {
			synchronized (mWaitingRequests) {
				String cacheKey = request.getCacheKey();
				Queue<Request> waitingRequests = mWaitingRequests.remove(cacheKey);
				if (waitingRequests != null) {

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
