package com.nifty.http;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by BaoRui on 2014/12/9.
 */
public class PriorityThreadPool {

	private static final int CORE_POOL_SIZE = 5;
	//	private static final int MAXIMUM_POOL_SIZE = 10;
	private static final int KEEP_ALIVE = 1;

	private final PriorityBlockingQueue<Runnable> priorityQueue =
			new PriorityBlockingQueue<Runnable>();

	private final ThreadFactory sThreadFactory = new ThreadFactory() {
		private final AtomicInteger mCount = new AtomicInteger(1);

		public Thread newThread(Runnable r) {
			return new Thread(r, "AsyncTask #" + mCount.getAndIncrement());
		}
	};

	//	public final ThreadPoolExecutor PRIORITY_THREAD_POOL
	//			= new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE,
	//			TimeUnit.SECONDS, priorityQueue, sThreadFactory,
	//			new ThreadPoolExecutor.DiscardOldestPolicy());

	private ThreadPoolExecutor executor;

	public PriorityThreadPool() {
		this(CORE_POOL_SIZE);
	}

	public PriorityThreadPool(int corePoolSize) {
		executor = new ThreadPoolExecutor(CORE_POOL_SIZE, CORE_POOL_SIZE, KEEP_ALIVE,
				TimeUnit.SECONDS, priorityQueue, sThreadFactory,
				new ThreadPoolExecutor.DiscardOldestPolicy());
	}

	public void execute(final Runnable command) {
		executor.execute(new PriorityTask() {
			@Override public void run() {
				command.run();
			}
		});
	}

	public void execute(final Runnable command, final Priority priority) {
		executor.execute(new PriorityTask(priority) {
			@Override public void run() {
				command.run();
			}
		});
	}

	public void execute(final PriorityTask command) {
		executor.execute(command);
		//		executor.remove()
	}

	public void stop() {
		if (executor != null && executor.isShutdown()) {
			executor.shutdown();
		}
	}
}
