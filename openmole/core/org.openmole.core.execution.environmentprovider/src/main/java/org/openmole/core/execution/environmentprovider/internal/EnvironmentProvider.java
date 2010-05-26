package org.openmole.core.execution.environmentprovider.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.core.execution.environmentprovider.IEnvironmentProvider;
import org.openmole.core.workflow.model.execution.IEnvironment;
import org.openmole.core.workflow.model.execution.IEnvironmentDescription;

public class EnvironmentProvider implements IEnvironmentProvider {

    Map environements;

    @Override
    public <ENV extends IEnvironment, DESC extends IEnvironmentDescription<? extends ENV>>  ENV getEnvironment(DESC description) throws InternalProcessingError {

        Map<DESC, ENV> envs = this.<ENV, DESC>getEnvironments();

        synchronized (envs) {
            if (!isDescriptionRegistred(description)) {
                    ENV env = description.createEnvironment();
                    envs.put(description, env);
                    return env;
                
            }
            return envs.get(description);
        }


    }

    private synchronized <ENV extends IEnvironment, DESC extends IEnvironmentDescription<? extends ENV>>  Map<DESC, ENV> getEnvironments() {
        if (environements == null) {
            environements = new HashMap<DESC, ENV>();
        }

        return environements;
    }

    @Override
    public synchronized boolean isDescriptionRegistred(IEnvironmentDescription desc) {
        return getEnvironments().containsKey(desc);
    }
}
