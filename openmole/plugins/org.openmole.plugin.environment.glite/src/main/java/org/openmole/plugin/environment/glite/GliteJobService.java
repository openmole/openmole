/*
 *  Copyright (C) 2010 reuillon
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
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

package org.openmole.plugin.environment.glite;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.model.execution.batch.IRuntime;
import org.openmole.core.model.file.IURIFile;
import org.openmole.misc.workspace.ConfigurationLocation;
import org.openmole.plugin.environment.glite.internal.Activator;
import org.openmole.plugin.environment.jsaga.JSAGAJob;
import org.openmole.plugin.environment.jsaga.JSAGAJobService;

/**
 *
 * @author reuillon
 */
public class GliteJobService extends JSAGAJobService<GliteEnvironment, GliteAuthentication> {

    final static String ConfigGroup = GliteJobService.class.getSimpleName();
    final static ConfigurationLocation LCGCPTimeOut = new ConfigurationLocation(ConfigGroup, "RuntimeCopyOnWNTimeOut");

    static {
        Activator.getWorkspace().addToConfigurations(LCGCPTimeOut, "PT2M");
    }
    
    
    public GliteJobService(URI jobServiceURI, GliteEnvironment environment, GliteAuthenticationKey authenticationKey, GliteAuthentication authentication, int nbAccess) throws InternalProcessingError, UserBadDataError, InterruptedException {
        super(jobServiceURI, environment, authenticationKey, authentication, nbAccess);
    }

    @Override
    protected JSAGAJob buildJob(String id) throws InternalProcessingError {
        return new GliteJob(id, this, getAuthentication().getProxyExpiresTime());
    }

    @Override
    protected void generateScriptString(String in, String out, IRuntime runtime, int memorySizeForRuntime, OutputStream os) throws IOException, InternalProcessingError {
        PrintStream writter = new PrintStream(os);
        
        writter.print("BASEPATH=$PWD;CUR=$PWD/ws$RANDOM;while test -e $CUR; do CUR=$PWD/ws$RANDOM;done;mkdir $CUR; export HOME=$CUR; cd $CUR; ");
        writter.print(mkLcgCpGunZipCmd(getEnvironment(), runtime.getRuntime().getLocationString(), "$PWD/openmole.tar.bz2"));
        writter.print(" tar -xjf openmole.tar.bz2 >/dev/null; rm -f openmole.tar.bz2; ");
        writter.print("mkdir envplugins; PLUGIN=0;");

        for(IURIFile plugin: runtime.getEnvironmentPlugins()) {
            writter.print(mkLcgCpGunZipCmd(getEnvironment(), plugin.getLocationString(), "$CUR/envplugins/plugin$PLUGIN.jar"));
            writter.print("PLUGIN=`expr $PLUGIN + 1`; ");
        }

        writter.print(mkLcgCpGunZipCmd(getEnvironment(), runtime.getEnvironmentAuthenticationFile().getLocationString(),"$CUR/authentication.xml"));

        writter.print("cd org.openmole.runtime-*; export PATH=$PWD/jre/bin:$PATH; /bin/sh run.sh ");
        writter.print(Integer.toString(memorySizeForRuntime));
        writter.print("m ");
        writter.print("-a $CUR/authentication.xml ");
        writter.print("-p $CUR/envplugins/ ");
        writter.print("-i ");
        writter.print(in);
        writter.print(" -o ");
        writter.print(out);
        writter.print(" -w $CUR ; cd .. ; rm -rf $CUR");

    }
    
    String mkLcgCpGunZipCmd(GliteEnvironment env, String from, String to) throws InternalProcessingError {
        StringBuilder builder = new StringBuilder();

        builder.append("lcg-cp --vo ");
        builder.append(env.getVOName());
        builder.append(" --checksum --connect-timeout ");
        builder.append(getTimeOut());
        builder.append(" --sendreceive-timeout ");
        builder.append(getTimeOut());
        builder.append(" --bdii-timeout ");
        builder.append(getTimeOut());
        builder.append(" --srm-timeout ");
        builder.append(getTimeOut());
        builder.append(" ");
        builder.append(from);
        builder.append(" file:");
        builder.append(to);
        builder.append(".gz; gunzip ");
        builder.append(to);
        builder.append(".gz;");

        String cp = builder.toString();
        return cp;
    }
    
    private String getTimeOut() throws InternalProcessingError {
        String timeOut = new Integer(Activator.getWorkspace().getPreferenceAsDurationInS(LCGCPTimeOut)).toString();
        return timeOut;
    }

    @Override
    protected JobDescription buildJobDescription(IRuntime runtime, File script, Map<String, String> attributes) throws InternalProcessingError, InterruptedException {
        try {
            JobDescription description = super.buildJobDescription(runtime, script, attributes);
            
            StringBuilder requirements = new StringBuilder();
            int i = 0;
            
            while(i < GliteAttributes.values().length - 1) {
                requirements.append(attributes.get(GliteAttributes.values()[i].value));
                requirements.append("&&");   
                i++;
            }
            
            requirements.append(attributes.get(GliteAttributes.values()[i].value));
            description.setVectorAttribute("ServiceAttributes", new String[]{"wms.requirements", requirements.toString()});

            return description;
        } catch (NotImplementedException ex) {
            throw new InternalProcessingError(ex);
        } catch (AuthenticationFailedException ex) {
            throw new InternalProcessingError(ex);
        } catch (AuthorizationFailedException ex) {
            throw new InternalProcessingError(ex);
        } catch (PermissionDeniedException ex) {
            throw new InternalProcessingError(ex);
        } catch (IncorrectStateException ex) {
            throw new InternalProcessingError(ex);
        } catch (BadParameterException ex) {
            throw new InternalProcessingError(ex);
        } catch (DoesNotExistException ex) {
            throw new InternalProcessingError(ex);
        } catch (TimeoutException ex) {
            throw new InternalProcessingError(ex);
        } catch (NoSuccessException ex) {
            throw new InternalProcessingError(ex);
        }
    }

    
    
}
