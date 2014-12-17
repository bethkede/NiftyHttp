package com.nifty.test;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by BaoRui on 2014/12/16.
 */
public class SyncTest {

	private static Executor executor = Executors.newSingleThreadExecutor();

	//	private static ReentrantLock lock = new ReentrantLock();
	//	private Condition condition = lock.newCondition();

	public static void main(String[] args) {
		System.out.println("--------main----------");
		init();
		for (int i = 0; i < 5; i++) {
			execute();
		}
	}
	private static void init() {
		synchronized (executor) {
			System.out.println("--------init----------");
			new Thread() {
				@Override public void run() {
					try {
						System.out.println("--------init run----------");
						Thread.sleep(5000);
						System.out.println("--------init end----------");
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}.start();
		}
	}

	private static void execute() {
		synchronized (executor) {
			System.out.println("--------execute----------");
			executor.execute(new Runnable() {
				@Override public void run() {
					System.out.println("--------execute run----------");
				}
			});
		}
	}
}
