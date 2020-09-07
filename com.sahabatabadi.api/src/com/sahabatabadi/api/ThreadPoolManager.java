package com.sahabatabadi.api;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPoolManager {
	private static ExecutorService executor = createExecutor();
	
	public static ExecutorService getExecutor() {
		return ThreadPoolManager.executor;
	}
	
	public static void stopExecutor() {
		ThreadPoolManager.executor.shutdown();
	}
	
	public static void reinitializeExecutor() {
		ExecutorService oldExecutor = ThreadPoolManager.executor;
		
		ThreadPoolManager.executor = createExecutor();
		
		oldExecutor.shutdown();
	}
	
	private static ExecutorService createExecutor() {
		return Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	}
}
