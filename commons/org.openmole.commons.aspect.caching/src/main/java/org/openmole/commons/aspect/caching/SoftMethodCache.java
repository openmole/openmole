package org.openmole.commons.aspect.caching;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import org.apache.commons.collections15.map.ReferenceMap;

public class SoftMethodCache {

    final Map<Object, Map<String, Object>> cache = Collections.synchronizedMap(new WeakHashMap<Object, Map<String, Object>>());
  
    void putCachedMethodResult(Object object, String method, Object result) {
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
                methodMap = Collections.synchronizedMap(new ReferenceMap<String, Object>(ReferenceMap.SOFT, ReferenceMap.SOFT));
                cache.put(object, methodMap);
            }
            return methodMap;
        }
    }

    public int size() {
        return cache.size();
    }

    void clear(Object obj) {
        cache.remove(obj);
    }
}
