package net.liveshift.util;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

public class TimeOutHashMap<T1, T2> {
	
	private static final int CLEANUP_INTERVAL_PUTS = 20;
	
	final private HashMap<T1, WrappedValue> backingMap;
	private final long timeoutMillis;
	private int cleanupCounter;
	
	public class WrappedValue {
		long timeoutTime;
		final T2 value;
		
		public WrappedValue(T2 value) {
			this.timeoutTime = new Date().getTime() + timeoutMillis;
			this.value = value;
		}

		public T2 get() {
			this.timeoutTime = new Date().getTime() + timeoutMillis;
			return this.value;
		}
		
		public long getTimeToTimeout() {
			return new Date().getTime() - this.timeoutTime;
		}
	}
	
	public TimeOutHashMap(final long timeoutMillis) {
		this.backingMap = new HashMap<T1, WrappedValue>();
		this.timeoutMillis = timeoutMillis;
	}

	synchronized public void put(final T1 key, final T2 value) {
		
		if (this.cleanupCounter++ > CLEANUP_INTERVAL_PUTS) {
			
			cleanup();
		}
		
		this.backingMap.put(key, new WrappedValue(value));
	}
	synchronized public T2 get(final T1 key) {
		WrappedValue wrappedValue = this.backingMap.get(key);
		
		if (wrappedValue==null)
			return null;
		else if (wrappedValue.timeoutTime < new Date().getTime()) {
			this.backingMap.remove(key);
			return null;  //timed out
		}
		else
			return wrappedValue.get();
	}

	synchronized private void cleanup() {
		Iterator<Entry<T1, WrappedValue>> iter = this.backingMap.entrySet().iterator();
		
		long time = new Date().getTime();
		
		while (iter.hasNext()) {
			Entry<T1, WrappedValue> entry = iter.next();
			
			if (entry.getValue().timeoutTime < time)
				iter.remove();
		}
	}
	
	public Long getTimeToTimeOut(final T1 key) {
		WrappedValue wrappedValue = this.backingMap.get(key);
		long currentTime = new Date().getTime();
		
		if (wrappedValue==null)
			return null;
		else if (wrappedValue.timeoutTime < currentTime) {
			this.backingMap.remove(key);
			return null;  //timed out
		}
		else
			return currentTime - wrappedValue.timeoutTime;
	}
	
	public WrappedValue getWrappedValue(final T1 key) {
		WrappedValue wrappedValue = this.backingMap.get(key);
		long currentTime = new Date().getTime();
		
		if (wrappedValue==null)
			return null;
		else if (wrappedValue.timeoutTime < currentTime) {
			this.backingMap.remove(key);
			return null;  //timed out
		}
		else
			return wrappedValue;
	}
	
}
