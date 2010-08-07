package org.simexplorer.core.workflow.model.libraries;

import org.openmole.core.model.job.IContext;



public abstract class Platform {
    // TODO : find a better design for automatically branch the inmplementation corresponding to the platform type
    public enum Type {
        R, Java, Dakota, Binary
    };
    public Type type;

    public abstract void load(Library library);

    public abstract String getInvokeCode(MethodInstance method);

    public abstract Object invoke(IContext context, MethodInstance method);

    public abstract void close(Library library);

    public abstract void install();  
}
