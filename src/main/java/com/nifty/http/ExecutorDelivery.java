package com.nifty.http;

import android.os.Handler;
import android.util.Log;
import com.nifty.http.error.VolleyError;

import java.util.concurrent.Executor;

/**
 * Delivers responses and errors.
 */
public class ExecutorDelivery implements ResponseDelivery {

	/**
	 * Used for posting responses, typically to the main thread.
	 */
	private final Executor mResponsePoster;

	/**
	 * Creates a new response delivery interface.
	 *
	 * @param handler {@link android.os.Handler} to post responses on
	 */
	public ExecutorDelivery(final Handler handler) {
		// Make an Executor that just wraps the handler.
		mResponsePoster = new Executor() {
			@Override
			public void execute(Runnable command) {
				handler.post(command);
			}
		};
	}

	/**
	 * Creates a new response delivery interface, mockable version
	 * for testing.
	 *
	 * @param executor For running delivery tasks
	 */
	public ExecutorDelivery(Executor executor) {
		mResponsePoster = executor;
	}

	@Override
	public void postResponse(Request request, Response response) {
		postResponse(request, response, null);
	}

	@Override public void postLastCache(Request request, Response response) {
		mResponsePoster.execute(new ResponseDeliveryRunnable(request, response, null, true));
	}

	@Override public void postNoLastCache(Request request) {
		mResponsePoster.execute(new ResponseDeliveryRunnable(request, null, null, true));
	}

	@Override
	public void postResponse(Request request, Response response, Runnable runnable) {
		request.markDelivered();
		mResponsePoster.execute(new ResponseDeliveryRunnable(request, response, runnable, false));
	}

	@Override
	public void postError(Request request, VolleyError error) {
		Response response = Response.error(error);
		mResponsePoster.execute(new ResponseDeliveryRunnable(request, response, null, false));
	}

	/**
	 * A Runnable used for delivering network responses to a listener on the
	 * main thread.
	 */
	@SuppressWarnings("rawtypes")
	private class ResponseDeliveryRunnable implements Runnable {
		private final Request mRequest;
		private final Response mResponse;
		private final Runnable mRunnable;
		private final boolean isLastCache;

		public ResponseDeliveryRunnable(Request request, Response response, Runnable runnable, boolean isLastCache) {
			mRequest = request;
			mResponse = response;
			mRunnable = runnable;
			this.isLastCache = isLastCache;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void run() {
			// If this request has canceled, finish it and don't deliver.
			if (mRequest.isCanceled()) {
				mRequest.finish("canceled-at-delivery");
				return;
			}

			if (isLastCache) {
				if (mResponse != null) {
					mRequest.deliverCacheResponse(mResponse);
				} else {
					mRequest.deliverNoCache();
				}
			} else {
				// Deliver a normal response or error, depending.
				if (mResponse.isSuccess()) {
					mRequest.deliverResponse(mResponse);
				} else {
					mRequest.deliverError(mResponse.error);
				}

				// If this is an intermediate response, add a marker, otherwise we're done
				// and the request can be finished.
				//TODO
				if (!mResponse.intermediate) {
					mRequest.finish("done");
				}

				// If we have been provided a post-delivery runnable, run it.
				if (mRunnable != null) {
					mRunnable.run();
				}
			}

		}
	}
}
