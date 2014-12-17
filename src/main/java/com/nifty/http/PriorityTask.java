package com.nifty.http;

/**
 * Created by BaoRui on 2014/12/9.
 */
abstract class PriorityTask implements Runnable, Comparable<PriorityTask> {

	private final Priority priority;

	public PriorityTask() {
		this(Priority.NORMAL);
	}

	public PriorityTask(Priority priority) {
		this.priority = priority;
	}

	@Override
	public final int compareTo(PriorityTask another) {
		return another.priority.ordinal() - priority.ordinal();

	}

}
