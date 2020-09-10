package com.sahabatabadi.api;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Adapter class to manage the injector thread pool
 */
public class ThreadPoolManager {
    /**
     * Executor Service as the adaptee object responsible for the thread pool
     */
    private static ExecutorService executor = createExecutor();

    /**
     * Stops the running Executor Service.
     */
    public static void stop() {
        ThreadPoolManager.executor.shutdown();
    }

    /**
     * Destroys the old Executor Service and creates/reinitializes a new one.
     */
    public static void reinitialize() {
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

    /**
     * Submits a value-returning task for execution and returns a Future
     * representing the pending results of the task. The Future's get method will
     * return the task's result upon successful completion.
     * 
     * @param <T>  return type of the task's result
     * @param task task to be run in a worker thread
     * @return a Future object representing pending completion of the task
     */
    public static <T> Future<T> submitTask(Callable<T> task) {
        return executor.submit(task);
    }
}
