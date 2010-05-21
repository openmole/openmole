package org.openmole.misc.backgroundexecutor.internal;


import org.openmole.misc.backgroundexecutor.ITransferable;
import org.openmole.misc.backgroundexecutor.IBackgroundExecutor;
import org.openmole.misc.backgroundexecutor.IBackgroundExecution;

public class BackgroundExecutor implements IBackgroundExecutor {


    @Override
    public IBackgroundExecution createBackgroundExecution(ITransferable transferable) {
        return new BackgroundExecution(transferable);
    }

   
}
