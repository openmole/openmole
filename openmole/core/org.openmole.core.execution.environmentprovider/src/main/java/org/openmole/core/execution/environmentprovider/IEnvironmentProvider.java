package org.openmole.core.execution.environmentprovider;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.core.workflow.model.execution.IEnvironment;
import org.openmole.core.workflow.model.execution.IEnvironmentDescription;

public interface IEnvironmentProvider {
	<ENV extends IEnvironment, DESC extends IEnvironmentDescription<? extends ENV>> ENV getEnvironment(DESC desc) throws InternalProcessingError;
        boolean isDescriptionRegistred(IEnvironmentDescription desc);
}
