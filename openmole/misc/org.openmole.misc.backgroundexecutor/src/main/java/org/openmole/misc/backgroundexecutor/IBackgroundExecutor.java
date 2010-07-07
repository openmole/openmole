package org.openmole.misc.backgroundexecutor;

import java.util.concurrent.Callable;

public interface IBackgroundExecutor {
    <T> IBackgroundExecution<T> createBackgroundExecution(Callable<T> callable);
}
