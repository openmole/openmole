package org.openmole.commons.aspect.caching;

import java.util.Map;
import java.util.TreeMap;
import java.util.WeakHashMap;

public class MethodCache {

//    final static Logger LOGGER = Logger.getLogger(MethodCache.class.getName());
    
    final Map<Object, Map<String, Object>> cache = new WeakHashMap<Object, Map<String, Object>>();

    void putCachedMethodResult(Object object, String method, Object result) {
        getMethodMap(object).put(method, result);
    }

    Object getCachedMethodResult(Object object, String method) {
        Map<String, Object> methodMap = cache.get(object);
        if (methodMap == null) {
            return null;
        }
        Object toReturn = methodMap.get(method);
        
//        LOGGER.log(Level.FINE, "Cached {0} size {1} returning {2}", new Object[]{method, size(), toReturn});
 
        return toReturn;
    }

    private Map<String, Object> getMethodMap(Object object) {
        Map<String, Object> methodMap = cache.get(object);
        if (methodMap != null) {
            return methodMap;
        }

        synchronized (cache) {
            methodMap = cache.get(object);
            if (methodMap == null) {
                methodMap = new TreeMap<String, Object>();
                cache.put(object, methodMap);
            }
            return methodMap;
        }
    }

    void clear(Object obj) {
        cache.remove(obj);
    }

    public int size() {
        return cache.size();
    }
}
