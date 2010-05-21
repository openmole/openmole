package org.openmole.misc.eventdispatcher;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.openmole.misc.eventdispatcher.internal.Activator;
import org.openmole.misc.exception.InternalProcessingError;
import org.openmole.misc.exception.UserBadDataError;
import java.lang.InterruptedException;
import java.lang.reflect.Method;
import org.aspectj.lang.reflect.MethodSignature;
import java.lang.annotation.Annotation;

public aspect EventAspect {

	after() throws InternalProcessingError, UserBadDataError : execution(* *(..)) && @annotation(org.openmole.misc.eventdispatcher.ObjectModified) {
		
		Object object = thisJoinPoint.getThis();

                Method method = ((MethodSignature) thisJoinPoint.getSignature()).getMethod();
                ObjectModified annotation = method.getAnnotation(ObjectModified.class);

                Activator.getEventDispatcher().objectChanged(object,annotation.type(), thisJoinPoint.getArgs());
	}

        after() : execution(*.new(..)) && @annotation(org.openmole.misc.eventdispatcher.ObjectConstructed) {

		// construct genericIdentifier
		Object object = thisJoinPoint.getThis();
		//Object ret = proceed();

               // Method method = ((MethodSignature) thisJoinPoint.getSignature()).getMethod();
               // ObjectModified annotation = method.getAnnotation(ObjectModified.class);

               // Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.INFO,"ctr: " + thisJoinPoint.getSignature().toString());

                Activator.getEventDispatcher().objectConstructed(object);
		//Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.INFO,object.toString() + " "+ method);

		//return ret;
	}
}
