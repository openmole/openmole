package org.openmole.commons.aspect.caching;

import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openmole.commons.tools.service.LockRepository;

public aspect CachingAspect {

	MethodCache methodCache = new MethodCache();
	SoftMethodCache softMethodCache = new SoftMethodCache();
	WeakLockRepository lockRepo = new WeakLockRepository();
        LockRepository<Object> objectLockRepository = new LockRepository<Object>();

	Object around(): execution(* *(..)) && @annotation(org.openmole.commons.aspect.caching.Cachable) {
		
		Object object = thisJoinPoint.getThis();
		String method = thisJoinPointStaticPart.getSignature().toString();

		Object ret = methodCache.getCachedMethodResult(object, method);
		
		if(ret !=  null) return ret;

		Lock lock = lockRepo.getLockFor(object, method);

		lock.lock();
		try {		
			objectLockRepository.lock(object);

                        try{
                            ret = methodCache.getCachedMethodResult(object, method);
                            if(ret == null) {
				ret = proceed();
				methodCache.putCachedMethodResult(object, method, ret);
                            }
                            return ret;
                        } finally {
                            objectLockRepository.unlock(object);
                        }
			
		} finally {
			lock.unlock();
		}
	}

	Object around(): execution(* *(..)) && @annotation(org.openmole.commons.aspect.caching.SoftCachable) {
		Object object = thisJoinPoint.getThis();
		String method = thisJoinPointStaticPart.getSignature().toString();

		
		Object ret = softMethodCache.getCachedMethodResult(object, method);
		
		//Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.INFO, "Getting soft cached res "+ ret);
		
                //System.out.println("SoftCache " + method);

		if(ret != null) return ret;

		Lock lock = lockRepo.getLockFor(object, method);

		lock.lock();
		try {
                        objectLockRepository.lock(object);

                        try {
                            ret = softMethodCache.getCachedMethodResult(object, method);
                            if(ret == null) {
				ret = proceed();
				softMethodCache.putCachedMethodResult(object, method, ret);
                            }
                            return ret;
                        } finally {
                            objectLockRepository.unlock(object);
                        }

		} finally {
			lock.unlock();
		}
	}


        before() : execution(* *(..)) && @annotation(org.openmole.commons.aspect.caching.ChangeState) {
             Object object = thisJoinPoint.getThis();
             objectLockRepository.lock(object);

              try {
                methodCache.clear(object);
                softMethodCache.clear(object);
             } finally {
		objectLockRepository.unlock(object);
             }
  	}
}
