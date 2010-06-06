package org.openmole.core.batchenvironmentauthenticationregistry;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.core.model.execution.batch.IBatchEnvironmentDescription;
import org.openmole.core.model.execution.batch.IBatchEnvironmentAuthentication;

public interface IBatchEnvironmentAuthenticationRegistry {
	IBatchEnvironmentAuthentication getAuthentication(IBatchEnvironmentDescription desc) throws InternalProcessingError;
        boolean isRegistred(IBatchEnvironmentDescription desc);
        void createAuthenticationIfNeeded(IBatchEnvironmentDescription desc) throws InternalProcessingError;
}
