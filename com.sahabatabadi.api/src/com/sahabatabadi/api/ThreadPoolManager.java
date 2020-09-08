package com.sahabatabadi.api;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Adapter class to manage the injector thread pool
 */
public class ThreadPoolManager {
    /**
     * Executor Service as the adaptee object responsible for the thread pool
     */
    private static ExecutorService executor = createExecutor();

    /**
     * Getter for the executor service object
     * 
     * @return Executor Service object of the class
     */
    public static ExecutorService getExecutor() {
        return ThreadPoolManager.executor;
    }

    /**
     * Stops the running Executor Service.
     */
    public static void stopExecutor() {
        ThreadPoolManager.executor.shutdown();
    }

    /**
     * Destroys the old Executor Service and creates/reinitializes a new one.
     */
    public static void reinitializeExecutor() {
        ExecutorService oldExecutor = ThreadPoolManager.executor;

        ThreadPoolManager.executor = createExecutor();

        oldExecutor.shutdown();
    }

    /**
     * Helper method to create a new executor service with thread count matching
     * available logical CPU cores.
     * 
     * @return Executor Service with thread count matching available logical cores
     */
    private static ExecutorService createExecutor() {
        return Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }
}
