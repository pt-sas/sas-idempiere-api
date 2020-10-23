package com.sahabatabadi.api;

public class DocumentInjector {
    public static boolean injectDocument(int windowId, int menuId, DocHeader headerObj) {
    	DocumentInjectorThread task = new DocumentInjectorThread(windowId, menuId, headerObj);
        ThreadPoolManager.submitTask(task);
        return true;
    }
}
