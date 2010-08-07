package org.openmole.core.authenticationregistry;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.model.execution.batch.IBatchServiceAuthentication;
import org.openmole.core.model.execution.batch.IBatchServiceAuthenticationKey;

public interface IAuthenticationRegistry {
        boolean isRegistred(IBatchServiceAuthenticationKey authenticationKey);
        <AUTH extends IBatchServiceAuthentication> void initAndRegisterIfNotAllreadyIs(IBatchServiceAuthenticationKey<? extends AUTH> key, AUTH authentication) throws InternalProcessingError, UserBadDataError, InterruptedException;
        <AUTH extends IBatchServiceAuthentication> AUTH getRegistred(IBatchServiceAuthenticationKey<? extends AUTH> key);

}
