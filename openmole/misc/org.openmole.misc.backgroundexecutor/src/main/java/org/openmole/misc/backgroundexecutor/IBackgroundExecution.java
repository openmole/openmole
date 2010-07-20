/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openmole.misc.backgroundexecutor;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.openmole.misc.executorservice.ExecutorType;

/**
 *
 * @author reuillon
 */
public interface IBackgroundExecution<T>  {

    public Future start(ExecutorType type);

    boolean isSucessFullExceptionIfFailed() throws ExecutionException;

    boolean isStarted();

    boolean hasFailed();

    boolean isFinished();

    Throwable getFailureCause();
    
    T getResult();

    boolean isSucessFull();

}
