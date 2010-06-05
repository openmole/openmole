package org.openmole.misc.backgroundexecutor.internal;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.openmole.misc.executorservice.ExecutorType;

import org.openmole.misc.backgroundexecutor.IBackgroundExecution;

public class BackgroundExecution implements IBackgroundExecution {

    final Callable callable;
    Throwable exception = null;
    boolean finished = false;
    boolean started = false;
 
    Future future;

    public BackgroundExecution(Callable callable) {
        super();
        this.callable = callable;
    }

    @Override
    public synchronized void start(ExecutorType type) {
        if (isStarted()) {
            return;
        }
        future = Activator.getExecutorService().getExecutorService(type).submit(new Runnable() {

            @Override
            public void run() {
                Thread.currentThread().setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread thread, Throwable thrwbl) {
                       exception = thrwbl;
                    }
                });

                try {
                    callable.call();
                } catch (Throwable e) {
                    exception = e;
                } finally {
                    finished = true;
                }
            }
        });

        started = true;
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
    public synchronized void interrupt() {
        if(future != null) {
            future.cancel(true);
        }
    }

    @Override
    public synchronized boolean isSucessFullStartIfNecessaryExceptionIfFailed(ExecutorType type) throws InterruptedException, ExecutionException {
        if (hasFailed()) {
            Throwable t = getFailureCause();
            if(InterruptedException.class.isAssignableFrom(t.getClass())) {
                throw ((InterruptedException) t);
            } else {
                throw new ExecutionException(t);
            }
        }

        if (!isStarted()) {
            start(type);
        }

        return isSucessFull();
    }
}
