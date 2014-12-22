package com.nifty.http;

import android.os.Handler;
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

		mResponsePoster.execute(new ResponseDeliveryRunnable(request, response, false));
	}

	@Override public void postLastCache(Request request, Response response) {
		mResponsePoster.execute(new ResponseDeliveryRunnable(request, response, true));
	}

	@Override public void postNoLastCache(Request request) {
		mResponsePoster.execute(new ResponseDeliveryRunnable(request, null, true));
	}

	@Override
	public void postError(Request request, VolleyError error) {
		Response response = Response.error(error);
		mResponsePoster.execute(new ResponseDeliveryRunnable(request, response, false));
	}

	/**
	 * A Runnable used for delivering network responses to a listener on the
	 * main thread.
	 */
	private class ResponseDeliveryRunnable implements Runnable {
		private final Request mRequest;
		private final Response mResponse;
		private final boolean isLastCache;

		public ResponseDeliveryRunnable(Request request, Response response, boolean isLastCache) {
			mRequest = request;
			mResponse = response;
			this.isLastCache = isLastCache;
		}

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

			}

		}
	}
}
