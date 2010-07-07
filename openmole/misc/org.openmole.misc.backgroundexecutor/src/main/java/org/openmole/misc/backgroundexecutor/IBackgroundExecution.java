/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openmole.misc.backgroundexecutor;

import java.util.concurrent.ExecutionException;
import org.openmole.misc.executorservice.ExecutorType;

/**
 *
 * @author reuillon
 */
public interface IBackgroundExecution<T> {

    public void start(ExecutorType type);

    boolean isSucessFullStartIfNecessaryExceptionIfFailed(ExecutorType type) throws ExecutionException;

    boolean isStarted();

    boolean hasFailed();

    boolean isFinished();

    Throwable getFailureCause();
    
    T getResult();

    boolean isSucessFull();

    void interrupt();
}
