package org.openmole.misc.caching;

import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.WeakHashMap;


public class SoftMethodCache {

    class ObjectContainer {
        Object obj;

        ObjectContainer(Object obj) {
            this.obj = obj;
        }

        Object getObject() {
            return obj;
        }
    }

    Map<Object, Map<String, SoftReference<ObjectContainer>>> cache = new WeakHashMap<Object, Map<String, SoftReference<ObjectContainer>>>();

    void putCachedMethodResult(Object object, final String method, Object result) {
        final Map<String, SoftReference<ObjectContainer>> methodMap = getMethodMap(object);
        methodMap.put(method, new SoftReference<ObjectContainer>(new ObjectContainer(result) {

            @Override
            protected void finalize() throws Throwable {
                super.finalize();
                methodMap.remove(method);
            }
 
        }));
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
            else return objectContainer.getObject();
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

    void clear(Object obj) {
        cache.remove(obj);
    }

    public int size() {
        return cache.size();
    }
}
