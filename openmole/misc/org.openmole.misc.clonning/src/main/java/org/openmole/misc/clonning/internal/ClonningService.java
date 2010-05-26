package org.openmole.misc.clonning.internal;

import org.openmole.misc.clonning.*;

import com.rits.cloning.Cloner;
import org.openmole.commons.exception.InternalProcessingError;

public class ClonningService implements IClonningService {

    private Cloner cloner;

    public ClonningService() {
        cloner = new Cloner();
    }

    // return an isolated version of the top context cloned
   /* public synchronized IContext cloneJobContext(IContext context, ITicket ticket, String name) throws InternalProcessingError {
    //	IContext context = job.getContext();
    IContext parent = context.getParent();
    //	ITicket ticket = job.getTicket();
    //	String name = job.getTask().getName();

    context.isolate(ticket);
    //IContext clone = (IContext) xstream.fromXML(xstream.toXML(context));

    IContext clone = cloner.deepClone(context);
    
    parent.addChild(name, context, ticket);

    return clone;
    }*/
    @Override
    public synchronized Object clone(Object object) throws InternalProcessingError {
        if (object.getClass().isPrimitive()) {
            return object;
        }

        try {
            return cloner.deepClone(object);
        } catch(StackOverflowError error) {
            throw new InternalProcessingError("Unable to clonne object, it is to big for the library.");
        }
     }
}
