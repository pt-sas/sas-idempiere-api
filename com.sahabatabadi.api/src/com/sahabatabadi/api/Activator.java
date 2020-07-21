package com.sahabatabadi.api;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.sahabatabadi.api.salesorder.SOInjector;

public class Activator implements BundleActivator {

	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bundleContext) throws Exception {
		System.out.println("SAS SO Injector is starting");
		Activator.context = bundleContext;
		
//		SOInjector.dummyTrigger();
		System.out.println("Dummy data inserted");
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		System.out.println("SAS SO Injector is stopping");
		Activator.context = null;
	}

}
