package org.openmole.commons.aspect.caching;

import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openmole.commons.tools.service.LockRepository;

public aspect CachingAspect {

	MethodCache methodCache = new MethodCache();
	WeakLockRepository lockRepo = new WeakLockRepository();
        LockRepository<Object> objectLockRepository = new LockRepository<Object>();

	Object around(): execution(* *(..)) && @annotation(org.openmole.commons.aspect.caching.Cachable) {
		Object object = thisJoinPoint.getTarget();
		String method = thisJoinPointStaticPart.getSignature().toString();
               
		Object ret = methodCache.cachedMethodResult(object, method);
		
		if(ret !=  null) return ret;

		Lock lock = lockRepo.lockFor(object, method);

		lock.lock();
		try {		
			objectLockRepository.lock(object);

                        try{
                            ret = methodCache.cachedMethodResult(object, method);
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

        before() : execution(* *(..)) && @annotation(org.openmole.commons.aspect.caching.ChangeState) {
             Object object = thisJoinPoint.getTarget();
             objectLockRepository.lock(object);

              try {
                methodCache.clear(object);
             } finally {
		objectLockRepository.unlock(object);
             }
  	}
}
