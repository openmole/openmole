package org.openmole.core.execution.jsaga.session;

import org.ogf.saga.context.Context;
import org.ogf.saga.session.Session;
import org.openmole.misc.exception.InternalProcessingError;

public interface IJSagaSessionService {
	void addContext(Context context) throws InternalProcessingError;
	Session getSession() throws InternalProcessingError;
	Context createContext() throws InternalProcessingError;
}
