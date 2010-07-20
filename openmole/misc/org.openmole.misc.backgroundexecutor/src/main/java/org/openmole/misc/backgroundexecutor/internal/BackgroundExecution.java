package org.openmole.misc.backgroundexecutor.internal;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.openmole.misc.executorservice.ExecutorType;

import org.openmole.misc.backgroundexecutor.IBackgroundExecution;

public class BackgroundExecution<T> implements IBackgroundExecution<T> {

    Callable<T> callable;
    
    Throwable exception = null;
    boolean finished = false;
    boolean started = false;
 
    T result;

    public BackgroundExecution(Callable<T> callable) {
        super();
        this.callable = callable;
    }

    @Override
    public synchronized Future start(ExecutorType type) {
        if (isStarted()) {
            throw new IllegalStateException("Background execution allready started");
        }
        Future future = Activator.getExecutorService().getExecutorService(type).submit(new Runnable() {

            @Override
            public void run() {
                Thread.currentThread().setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread thread, Throwable thrwbl) {
                       exception = thrwbl;
                       finished = true;
                    }
                });

                try {
                    result = callable.call();
                } catch (Throwable e) {
                    exception = e;
                } finally {
                    finished = true;
                }
            }
        });

        started = true;
        return future;
    }

    @Override
    public synchronized boolean isStarted() {
        return started;
    }

    @Override
    public synchronized boolean hasFailed() {
        return exception != null;
    }

    @Override
    public synchronized Throwable getFailureCause() {
        return exception;
    }

    @Override
    public synchronized boolean isSucessFull() {
        return finished && !hasFailed();
    }

    @Override
    public synchronized boolean isFinished() {
        return finished;
    }

    @Override
    public synchronized boolean isSucessFullExceptionIfFailed() throws ExecutionException {
        if (hasFailed()) {
            Throwable t = getFailureCause();
            throw new ExecutionException(t);
        }
        return isSucessFull();
    }

    @Override
    public T getResult() {
        return result;
    }
}
