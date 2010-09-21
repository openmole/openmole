/*
 *  Copyright (C) 2010 reuillon
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the Affero GNU General Public License as published by
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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.joda.time.format.ISOPeriodFormat;

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
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.model.execution.batch.IRuntime;
import org.openmole.plugin.environment.jsaga.internal.Activator;
import static org.openmole.plugin.environment.jsaga.JSAGAAttributes.*;

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

            OutputStream os = new BufferedOutputStream(new FileOutputStream(tmpScript));
            try {
                env.getLaunchingScript().generateScript(in.toString(), out.toString(), runtime, env.getMemorySizeForRuntime(), os);
            } finally {
                os.close();
            }
            
            description.setVectorAttribute(JobDescription.ARGUMENTS, new String[]{tmpScript.getName()});
            description.setVectorAttribute(JobDescription.FILETRANSFER, new String[]{tmpScript.toURI().toURL() /*getSchemeSpecificPart()*/ + ">" + tmpScript.getName()});
      
            for(Map.Entry<String, String> entry: env.getAttributes().entrySet()) {
                final String value;
                
                if(entry.getKey().equals(CPU_TIME)) {
                     value = new Integer(ISOPeriodFormat.standard().parsePeriod(entry.getValue()).toStandardSeconds().getSeconds()).toString();
                } else value = entry.getValue();
                  
                description.setAttribute(entry.getKey(), value);
            }
                
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

    public JobDescription getHelloWorld() throws InternalProcessingError, UserBadDataError {
        if (hello != null) {
            return hello;
        }

        synchronized (this) {

            if (hello == null) {
                try {

                    File helloFile = Activator.getWorkspace().newFile("testhello", ".txt");
                    PrintStream str = new PrintStream(helloFile);

                    str.println("Hello");
                    str.close();

                    hello = JobFactory.createJobDescription();

                    hello.setAttribute(JobDescription.EXECUTABLE, "/bin/echo");
                    hello.setVectorAttribute(JobDescription.ARGUMENTS, new String[]{"Hello"});
                    hello.setVectorAttribute(JobDescription.FILETRANSFER, new String[]{helloFile.toURI().toURL() /*getSchemeSpecificPart()*/ + ">" + helloFile.getName()});

                } catch (NotImplementedException e) {
                    throw new InternalProcessingError(e);
                } catch (NoSuccessException e) {
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
                } catch (IOException e) {
                    throw new InternalProcessingError(e);
                }
            }

            return hello;
        }
    }
}
