package org.openmole.misc.backgroundexecutor;

public interface IBackgroundExecutor {
    IBackgroundExecution createBackgroundExecution(ITransferable transferable);
}
