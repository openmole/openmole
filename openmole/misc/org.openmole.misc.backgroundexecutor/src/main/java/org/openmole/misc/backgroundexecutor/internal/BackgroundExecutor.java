package org.openmole.misc.backgroundexecutor.internal;


import java.util.concurrent.Callable;
import org.openmole.misc.backgroundexecutor.IBackgroundExecutor;
import org.openmole.misc.backgroundexecutor.IBackgroundExecution;

public class BackgroundExecutor implements IBackgroundExecutor {


    @Override
    public IBackgroundExecution createBackgroundExecution(Callable callable) {
        return new BackgroundExecution(callable);
    }

   
}
