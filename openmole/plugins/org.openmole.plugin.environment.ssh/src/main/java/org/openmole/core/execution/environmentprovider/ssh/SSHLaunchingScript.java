package org.openmole.core.batchenvironmentauthenticationregistry.ssh;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.core.batchenvironmentauthenticationregistry.jsaga.model.IJSAGALaunchingScript;
import org.openmole.core.model.execution.batch.IRuntime;

public class SSHLaunchingScript implements IJSAGALaunchingScript<SSHEnvironment>{

	public SSHLaunchingScript() {
		super();
	}	

	@Override
	public String getScript(String args, IRuntime runtime, SSHEnvironment env) throws InternalProcessingError {
		        throw new UnsupportedOperationException("Not supported yet.");
	}

      
}
