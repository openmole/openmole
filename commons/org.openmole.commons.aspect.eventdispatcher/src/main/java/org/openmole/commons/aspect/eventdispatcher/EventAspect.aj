package org.openmole.commons.aspect.eventdispatcher;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.openmole.commons.aspect.eventdispatcher.internal.Activator;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import java.lang.InterruptedException;
import java.lang.reflect.Method;
import org.aspectj.lang.reflect.MethodSignature;
import java.lang.annotation.Annotation;

public aspect EventAspect {

	after() throws InternalProcessingError, UserBadDataError : execution(* *(..)) && @annotation(org.openmole.commons.aspect.eventdispatcher.ObjectModified) {
		
		Object object = thisJoinPoint.getThis();

                Method method = ((MethodSignature) thisJoinPointStaticPart.getSignature()).getMethod();
                ObjectModified annotation = method.getAnnotation(ObjectModified.class);

                Activator.getEventDispatcher().objectChanged(object,annotation.type(), thisJoinPoint.getArgs());
	}

        after() : execution(*.new(..)) && @annotation(org.openmole.commons.aspect.eventdispatcher.ObjectConstructed) {

		// construct genericIdentifier
		Object object = thisJoinPoint.getThis();
		//Object ret = proceed();

               // Method method = ((MethodSignature) thisJoinPointStaticPart.getSignature()).getMethod();
               // ObjectModified annotation = method.getAnnotation(ObjectModified.class);

               // Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.INFO,"ctr: " + thisJoinPointStaticPart.getSignature().toString());

                Activator.getEventDispatcher().objectConstructed(object);
		//Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.INFO,object.toString() + " "+ method);

		//return ret;
	}
}
