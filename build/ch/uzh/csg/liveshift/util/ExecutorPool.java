package net.liveshift.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


public class ExecutorPool {

	final static private ExecutorService generalExecutorService = Executors.newCachedThreadPool();
	final static private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(10);

	public static ExecutorService getGeneralExecutorService() {
		return generalExecutorService;
	}
	public static ScheduledExecutorService getScheduledExecutorService() {
		return scheduledExecutorService;
	}
}
