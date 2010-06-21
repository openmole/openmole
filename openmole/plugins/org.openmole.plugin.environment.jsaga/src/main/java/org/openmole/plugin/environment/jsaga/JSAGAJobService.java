/*
 *  Copyright (C) 2010 reuillon
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.plugin.environment.jsaga;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ogf.saga.error.AuthenticationFailedException;
import org.ogf.saga.error.AuthorizationFailedException;
import org.ogf.saga.error.BadParameterException;
import org.ogf.saga.error.IncorrectStateException;
import org.ogf.saga.error.NoSuccessException;
import org.ogf.saga.error.NotImplementedException;
import org.ogf.saga.error.PermissionDeniedException;
import org.ogf.saga.error.TimeoutException;
import org.ogf.saga.job.Job;
import org.ogf.saga.job.JobDescription;
import org.ogf.saga.job.JobFactory;
import org.ogf.saga.job.JobService;
import org.ogf.saga.task.State;
import org.ogf.saga.task.Task;
import org.ogf.saga.task.TaskMode;
import org.ogf.saga.url.URL;
import org.ogf.saga.url.URLFactory;
import org.openmole.core.batchservicecontrol.BatchJobServiceDescription;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.commons.aspect.caching.Cachable;
import org.openmole.core.implementation.execution.batch.BatchJobService;
import org.openmole.plugin.environment.jsaga.internal.Activator;
import org.openmole.core.model.execution.batch.IBatchJob;
import org.openmole.core.model.file.IURIFile;
import org.openmole.core.model.execution.batch.IRuntime;
import org.openmole.misc.workspace.ConfigurationLocation;

public class JSAGAJobService extends BatchJobService<IJSAGAJobDescription> {

    final static ConfigurationLocation CreationTimout = new ConfigurationLocation(JSAGAJobService.class.getSimpleName(), "CreationTimout");
    final static ConfigurationLocation TestJobDoneTimeOut = new ConfigurationLocation(JSAGAJobService.class.getSimpleName(), "TestJobDoneTimeOut");

    static {
        Activator.getWorkspace().addToConfigurations(CreationTimout, "PT2M");
        Activator.getWorkspace().addToConfigurations(CreationTimout, "PT30M");
    }
    URI jobServiceURI;
    JSAGAEnvironment environment;

    public JSAGAJobService(URI jobServiceURI, JSAGAEnvironment environment, int nbAccess) throws InternalProcessingError {
        super(environment.getDescription(), new BatchJobServiceDescription(jobServiceURI.toString()), nbAccess);
        this.jobServiceURI = jobServiceURI;
        this.environment = environment;
    }

    @Override
    public boolean test() {

        try {
            JobDescription hello = JSAGAJobBuilder.GetInstance().getHelloWorld();
            final Job job = getJobServiceCache().createJob(hello);

            job.run();

            float timeOut = Activator.getWorkspace().getPreferenceAsDurationInS(TestJobDoneTimeOut);
            if(!job.waitFor(timeOut)) return false;
            return job.getState() == State.DONE;
        } catch (IncorrectStateException e) {
            Logger.getLogger(JSAGAJobService.class.getName()).log(Level.WARNING, null, e);
            return false;
        } catch (InternalProcessingError e) {
            Logger.getLogger(JSAGAJobService.class.getName()).log(Level.WARNING, null, e);
            return false;
        } catch (NotImplementedException e) {
            Logger.getLogger(JSAGAJobService.class.getName()).log(Level.WARNING, null, e);
            return false;
        } catch (AuthenticationFailedException e) {
            Logger.getLogger(JSAGAJobService.class.getName()).log(Level.WARNING, null, e);
            return false;
        } catch (AuthorizationFailedException e) {
            Logger.getLogger(JSAGAJobService.class.getName()).log(Level.WARNING, null, e);
            return false;
        } catch (PermissionDeniedException e) {
            Logger.getLogger(JSAGAJobService.class.getName()).log(Level.WARNING, null, e);
            return false;
        } catch (BadParameterException e) {
            Logger.getLogger(JSAGAJobService.class.getName()).log(Level.WARNING, null, e);
            return false;
        } catch (TimeoutException e) {
            Logger.getLogger(JSAGAJobService.class.getName()).log(Level.WARNING, null, e);
            return false;
        } catch (NoSuccessException e) {
            Logger.getLogger(JSAGAJobService.class.getName()).log(Level.WARNING, null, e);
            return false;
        }
    }

    @Override
    public IBatchJob createBatchJob(IJSAGAJobDescription batchJobDescription) throws InternalProcessingError {

        IBatchJob ret;
        try {
            ret = new JSAGAJob(getJobServiceCache().createJob(batchJobDescription.getJobDescription()), this, batchJobDescription.getScript());
            return ret;
        } catch (NotImplementedException e) {
            throw new InternalProcessingError(e);
        } catch (AuthenticationFailedException e) {
            throw new InternalProcessingError(e);
        } catch (AuthorizationFailedException e) {
            throw new InternalProcessingError(e);
        } catch (PermissionDeniedException e) {
            throw new InternalProcessingError(e);
        } catch (BadParameterException e) {
            throw new InternalProcessingError(e);
        } catch (TimeoutException e) {
            throw new InternalProcessingError(e);
        } catch (NoSuccessException e) {
            throw new InternalProcessingError(e);
        } catch (InternalProcessingError e) {
            throw new InternalProcessingError(e);
        }
    }

    @Override
    public IBatchJob createBatchJob(IURIFile inputFile, IURIFile outputFile, IRuntime runtime) throws InternalProcessingError,
            UserBadDataError, InterruptedException {

        File script;
        try {
            script = Activator.getWorkspace().newTmpFile("script", ".sh");
        } catch (IOException e) {
            throw new InternalProcessingError(e);
        }

        JobDescription jobDescription = JSAGAJobBuilder.GetInstance().getJobDescription(inputFile.getLocation(), outputFile.getLocation(), environment, runtime, script);
        IJSAGAJobDescription JSAGAJobDescription = new JSAGAJobDescription(jobDescription, script);

        return createBatchJob(JSAGAJobDescription);
    }

    @Cachable
    private JobService getJobServiceCache() throws InternalProcessingError {
        //Logger.getLogger(JSAGAJobService.class.getName()).info("/!\\ CREATE JOB SERVICE..." + jobServiceURI.toString() + " "+ System.identityHashCode(this));

        final URL url;
        Task<?, JobService> task;
        try {
            url = URLFactory.createURL(jobServiceURI.toString());
            task = JobFactory.createJobService(TaskMode.ASYNC, Activator.getJSagaSessionService().getSession(), url);
        } catch (NotImplementedException e) {
            throw new InternalProcessingError(e);
        } catch (NoSuccessException e) {
            throw new InternalProcessingError(e);
        } catch (BadParameterException e) {
            throw new InternalProcessingError(e);
        }

        try {
            return task.get(Activator.getWorkspace().getPreferenceAsDurationInMs(CreationTimout), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new InternalProcessingError(e);
        } catch (ExecutionException e) {
            throw new InternalProcessingError(e);
        } catch (java.util.concurrent.TimeoutException e) {
            task.cancel(true);
            throw new InternalProcessingError(e);
        }

    }
}
