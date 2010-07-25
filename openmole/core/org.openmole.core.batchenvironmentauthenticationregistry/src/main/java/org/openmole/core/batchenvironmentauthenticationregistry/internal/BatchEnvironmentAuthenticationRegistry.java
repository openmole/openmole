package org.openmole.core.batchenvironmentauthenticationregistry.internal;


import java.util.HashMap;
import java.util.Map;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.core.batchenvironmentauthenticationregistry.IBatchEnvironmentAuthenticationRegistry;
import org.openmole.core.model.execution.batch.IBatchEnvironmentDescription;
import org.openmole.core.model.execution.batch.IBatchEnvironmentAuthentication;

public class BatchEnvironmentAuthenticationRegistry implements IBatchEnvironmentAuthenticationRegistry {

    Map<IBatchEnvironmentDescription, IBatchEnvironmentAuthentication> info = new HashMap<IBatchEnvironmentDescription, IBatchEnvironmentAuthentication>();

    @Override
    public synchronized IBatchEnvironmentAuthentication getAuthentication(IBatchEnvironmentDescription desc) throws InternalProcessingError {
        return info.get(desc);
    }

    @Override
    public synchronized boolean isRegistred(IBatchEnvironmentDescription desc) {
        return info.containsKey(desc);
    }

    @Override
    public synchronized void createAuthenticationIfNeeded(IBatchEnvironmentDescription desc) throws InternalProcessingError {
        if(!isRegistred(desc)) {
            this.info.put(desc, desc.createBatchEnvironmentAuthentication());
        }
    }
}
