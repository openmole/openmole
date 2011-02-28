/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openmole.core.authenticationregistry.ssh;

import java.util.Collection;
import org.openmole.misc.exception.InternalProcessingError;
import org.openmole.misc.exception.UserBadDataError;
import org.openmole.misc.workspace.ConfigurationLocation;
import org.openmole.core.authenticationregistry.jsaga.JSAGAExecutionEnvironment;
import org.openmole.core.authenticationregistry.jsaga.model.IJSAGAJobService;
import org.openmole.core.authenticationregistry.jsaga.model.IJSAGALaunchingScript;
import org.openmole.core.authenticationregistry.ssh.internal.Activator;
import org.openmole.core.model.execution.EnvironmentConfiguration;
import org.openmole.core.model.execution.batch.IBatchStorage;

/**
 *
 * @author reuillon
 */
public class SSHEnvironment extends JSAGAExecutionEnvironment<SSHEnvironmentDescription> {

    final static String ConfigGroup = SSHEnvironment.class.getSimpleName();
    final static ConfigurationLocation DefaultNbSpot = new ConfigurationLocation(ConfigGroup, "DefaultNbSpot");

    static {
        Activator.getWorkspace().addToConfigurations(DefaultNbSpot, "4");
    }


    Integer nbSpots;

    public SSHEnvironment(SSHEnvironmentDescription description) throws InternalProcessingError {
        super(description);
        nbSpots = Activator.getWorkspace().getPreferenceAsInt(DefaultNbSpot);
    }

    public void setNbSpots(Integer nbSpots) {
        this.nbSpots = nbSpots;
    }

  

    @Override
    protected Collection<IJSAGAJobService> allJobServices() throws InternalProcessingError, UserBadDataError {



        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected Collection<IBatchStorage> allStorages() throws InternalProcessingError, UserBadDataError {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setConfigurationMode(EnvironmentConfiguration configuration) throws InternalProcessingError {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public IJSAGALaunchingScript<?> getLaunchingScript() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void initializeAccess() throws UserBadDataError, InternalProcessingError, InterruptedException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
