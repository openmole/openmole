package org.openmole.commons.aspect.caching;

import java.lang.ref.ReferenceQueue;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.collections15.map.ReferenceMap;

public class SoftMethodCache {

    final static Logger LOGGER = Logger.getLogger(SoftMethodCache.class.getName());
    final Map<Object, Map<String, Object>> cache = new WeakHashMap<Object, Map<String, Object>>();
    final ReferenceQueue referenceQueue = new ReferenceQueue();

    public SoftMethodCache() {
        Executors.newSingleThreadExecutor().submit(new Runnable() {

            @Override
            public void run() {
            }
        });
    }

    void putCachedMethodResult(Object object, String method, Object result) {
        LOGGER.log(Level.FINE, "Softcache size {0}", size());

        final Map<String, Object> methodMap = getMethodMap(object);
        methodMap.put(method, result);
    }

    Object getCachedMethodResult(Object object, String method) {
        Map<String, Object> methodMap = cache.get(object);
        if (methodMap == null) {
            return null;
        }

        return methodMap.get(method);
    }

    private Map<String, Object> getMethodMap(Object object) {
        Map<String, Object> methodMap = cache.get(object);
        if (methodMap != null) {
            return methodMap;
        }

        synchronized (cache) {
            methodMap = cache.get(object);
            if (methodMap == null) {
                methodMap = Collections.synchronizedMap(new ReferenceMap<String, Object>(ReferenceMap.HARD, ReferenceMap.SOFT));
                cache.put(object, methodMap);
            }
            return methodMap;
        }
    }

    public int size() {
        return cache.size();
    }
}
