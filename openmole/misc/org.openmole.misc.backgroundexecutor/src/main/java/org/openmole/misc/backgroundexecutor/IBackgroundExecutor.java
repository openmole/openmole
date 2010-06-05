package org.openmole.misc.backgroundexecutor;

import java.util.concurrent.Callable;

public interface IBackgroundExecutor {
    IBackgroundExecution createBackgroundExecution(Callable callable);
}
