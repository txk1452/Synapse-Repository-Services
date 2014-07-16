package org.sagebionetworks.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;

public class ThreadTestUtils {
	private static class ThreadException{
		final Thread t;
		final Throwable e;

		public ThreadException(Thread t, Throwable e) {
			this.t = t;
			this.e = e;
		}
	};

	private static List<ThreadException> exceptions = Collections.synchronizedList(Lists.<ThreadException> newArrayList());
	private static int threadCountBefore = -1;

	public static void doBefore(){
		assertNull("Forgot to call Threads.doAfter in another test?", Thread.getDefaultUncaughtExceptionHandler());
		assertTrue("Forgot to call Threads.doAfter in another test?", exceptions.isEmpty());
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {			
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				exceptions.add(new ThreadException(t, e));
				System.err.println("Exception in thread " + t.toString() + ": " + e.getMessage());
				e.printStackTrace(System.err);
			}
		});
		threadCountBefore = Thread.activeCount();
	}
	
	public static void doAfter(){
		Thread.setDefaultUncaughtExceptionHandler(null);
		int numThrown = exceptions.size();
		exceptions.clear();
		assertEquals("Unhandled exceptions were thrown in other threads", 0, numThrown);
		assertEquals("Left over threads from test", threadCountBefore, Thread.activeCount());
	}
}
