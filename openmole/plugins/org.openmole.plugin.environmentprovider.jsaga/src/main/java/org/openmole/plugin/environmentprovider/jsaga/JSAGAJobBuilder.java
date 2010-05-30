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

package org.openmole.plugin.environmentprovider.jsaga;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;

import org.ogf.saga.error.AuthenticationFailedException;
import org.ogf.saga.error.AuthorizationFailedException;
import org.ogf.saga.error.BadParameterException;
import org.ogf.saga.error.DoesNotExistException;
import org.ogf.saga.error.IncorrectStateException;
import org.ogf.saga.error.NoSuccessException;
import org.ogf.saga.error.NotImplementedException;
import org.ogf.saga.error.PermissionDeniedException;
import org.ogf.saga.error.TimeoutException;
import org.ogf.saga.job.JobDescription;
import org.ogf.saga.job.JobFactory;
import org.openmole.core.implementation.execution.batch.BatchEnvironment;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.core.model.execution.batch.IRuntime;
import org.openmole.plugin.environmentprovider.jsaga.internal.Activator;

public class JSAGAJobBuilder {

    private static JSAGAJobBuilder instance = new JSAGAJobBuilder();
 
    private JSAGAJobBuilder() {
    }

    public static JSAGAJobBuilder GetInstance() {
        return instance;
    }
    JobDescription hello;

    public JobDescription getJobDescription(URI in, URI out, JSAGAEnvironment env, IRuntime runtime, File tmpScript) throws InternalProcessingError, InterruptedException {
        try {

            JobDescription description = JobFactory.createJobDescription();

            description.setAttribute(JobDescription.EXECUTABLE, "/bin/bash");

            StringBuilder args = new StringBuilder();
            args.append(in.toString());
            args.append(" ");
            args.append(out.toString());

            BufferedWriter writter = new BufferedWriter(new FileWriter(tmpScript));

            String argsSt = args.toString();

            //, env.getDescriptionFile().toURI().getSchemeSpecificPart() + ">" + env.getDescriptionFile().getName()

            writter.write(env.getLaunchingScript().getScript(argsSt, runtime, env));

            writter.close();

            description.setVectorAttribute(JobDescription.ARGUMENTS, new String[]{tmpScript.getName()});
            description.setAttribute(JobDescription.TOTALCPUTIME, new Integer(Activator.getWorkspace().getPreferenceAsDurationInS(JSAGAEnvironment.CPUTime)).toString());
            description.setAttribute(JobDescription.TOTALPHYSICALMEMORY, new Integer(Activator.getWorkspace().getPreferenceAsInt(JSAGAEnvironment.Memory)).toString());

            description.setVectorAttribute(JobDescription.FILETRANSFER, new String[]{tmpScript.toURI().getSchemeSpecificPart() + ">" + tmpScript.getName()});

          //  System.out.println(tmpScript.getPath() + " " + tmpScript.exists() + ";" + env.getDescriptionFile().getPath() + " " +  env.getDescriptionFile().exists());

            return description;

        } catch (IOException e) {
            throw new InternalProcessingError(e);
        } catch (NotImplementedException e) {
            throw new InternalProcessingError(e);
        } catch (AuthenticationFailedException e) {
            throw new InternalProcessingError(e);
        } catch (AuthorizationFailedException e) {
            throw new InternalProcessingError(e);
        } catch (PermissionDeniedException e) {
            throw new InternalProcessingError(e);
        } catch (IncorrectStateException e) {
            throw new InternalProcessingError(e);
        } catch (BadParameterException e) {
            throw new InternalProcessingError(e);
        } catch (DoesNotExistException e) {
            throw new InternalProcessingError(e);
        } catch (TimeoutException e) {
            throw new InternalProcessingError(e);
        } catch (NoSuccessException e) {
            throw new InternalProcessingError(e);
        }
    }

    public JobDescription getHelloWorld() throws InternalProcessingError {
        if (hello != null) {
            return hello;
        }

        synchronized (this) {

            if (hello == null) {
                try {

                    File helloFile = Activator.getWorkspace().newTmpFile("testhello", ".txt");
                    PrintStream str = new PrintStream(helloFile);

                    str.println("Hello");
                    str.close();

                    hello = JobFactory.createJobDescription();

                    hello.setAttribute(JobDescription.EXECUTABLE, "/bin/echo");
                    hello.setVectorAttribute(JobDescription.ARGUMENTS, new String[]{"Hello"});
                    hello.setVectorAttribute(JobDescription.FILETRANSFER, new String[]{helloFile.toURI().getSchemeSpecificPart() + ">" + helloFile.getName()});

                } catch (NotImplementedException e) {
                    new InternalProcessingError(e);
                } catch (NoSuccessException e) {
                    new InternalProcessingError(e);
                } catch (AuthenticationFailedException e) {
                    new InternalProcessingError(e);
                } catch (AuthorizationFailedException e) {
                    new InternalProcessingError(e);
                } catch (PermissionDeniedException e) {
                    new InternalProcessingError(e);
                } catch (IncorrectStateException e) {
                    new InternalProcessingError(e);
                } catch (BadParameterException e) {
                    new InternalProcessingError(e);
                } catch (DoesNotExistException e) {
                    new InternalProcessingError(e);
                } catch (TimeoutException e) {
                    new InternalProcessingError(e);
                } catch (IOException e) {
                    new InternalProcessingError(e);
                }
            }

            return hello;
        }
    }
}
