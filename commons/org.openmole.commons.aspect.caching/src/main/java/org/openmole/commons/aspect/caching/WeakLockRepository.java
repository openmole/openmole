package org.openmole.commons.aspect.caching;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class WeakLockRepository {
	Map<Object, Map<String, Lock>> locks  = new WeakHashMap<Object, Map<String,Lock>>();
	
	Lock getLockFor(Object obj, String method) {
		Map<String, Lock> lockMap = getLockMap(obj);
		
		Lock lock = lockMap.get(method);
		if(lock != null) return lock;
		
		synchronized (locks) {
			lock = lockMap.get(method);
			if(lock == null) {
				lock = new ReentrantLock();
				lockMap.put(method, lock);
			}
			return lock;
		}
	}

	private Map<String, Lock> getLockMap(Object obj) {
		Map<String, Lock> lockMap = locks.get(obj);
		if(lockMap != null) return lockMap;
		
		synchronized (locks) {
			lockMap = locks.get(obj);
			
			if(lockMap == null) {
				lockMap = new HashMap<String, Lock>();
				locks.put(obj, lockMap);
			}
			
			return lockMap;
		}
		
	}
	
}
