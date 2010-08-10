package org.openmole.commons.aspect.caching;

import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;


public class SoftMethodCache {
    
    final static Logger LOGGER = Logger.getLogger(SoftMethodCache.class.getName());
    
    class ObjectContainer {
        
        final String key;
        final Object obj;

        ObjectContainer(String key, Object obj) {
            this.key = key;
            this.obj = obj;
        }

        @Override
        protected void finalize() throws Throwable {
            super.finalize();
            LOGGER.log(Level.FINE, "Remove cache for {0}", key);
            synchronized (cache) {
                cache.remove(key);                
            }
        }
        
    }

    final Map<Object, Map<String, SoftReference<ObjectContainer>>> cache = new WeakHashMap<Object, Map<String, SoftReference<ObjectContainer>>>();

   
    void putCachedMethodResult(Object object, String method, Object result) {
        LOGGER.log(Level.FINE, "Softcache size {0}", size());
        
        final Map<String, SoftReference<ObjectContainer>> methodMap = getMethodMap(object);
        methodMap.put(method, new SoftReference<ObjectContainer>(new ObjectContainer(method, result)));
    }

    Object getCachedMethodResult(Object object, String method) {
        Map<String, SoftReference<ObjectContainer>> methodMap = cache.get(object);
        if(methodMap == null) return null;

        SoftReference<ObjectContainer> ref = methodMap.get(method);
        if (ref == null) {
            return null;
        } else {
            ObjectContainer objectContainer = ref.get();
            if(objectContainer == null) return null;
            else return objectContainer.obj;
        }
    }

    private Map<String, SoftReference<ObjectContainer>> getMethodMap(Object object) {
        Map<String, SoftReference<ObjectContainer>> methodMap = cache.get(object);
        if (methodMap != null) {
            return methodMap;
        }

        synchronized (cache) {
            methodMap = cache.get(object);
            if (methodMap == null) {
                methodMap = Collections.synchronizedMap(new TreeMap<String, SoftReference<ObjectContainer>>());
                cache.put(object, methodMap);
            }
            return methodMap;
        }
    }

    public int size() {
        return cache.size();
    }
}
