package org.openmole.core.execution.jsaga.session.internal;

import org.ogf.saga.context.Context;
import org.ogf.saga.context.ContextFactory;
import org.ogf.saga.error.IncorrectStateException;
import org.ogf.saga.error.NoSuccessException;
import org.ogf.saga.error.NotImplementedException;
import org.ogf.saga.error.TimeoutException;
import org.ogf.saga.session.Session;
import org.ogf.saga.session.SessionFactory;
import org.openmole.misc.exception.InternalProcessingError;
import org.openmole.core.execution.jsaga.session.IJSagaSessionService;


public class JSagaSessionService implements IJSagaSessionService {
	
	Session session;
	
	@Override
	public void addContext(Context context) throws InternalProcessingError {
		
		try {
			getSession().addContext(context);
		} catch (NotImplementedException e) {
			throw new InternalProcessingError(e);
		} 
		
	}

	@Override
	public Context createContext() throws InternalProcessingError {
		try {
			return ContextFactory.createContext();
		} catch (NotImplementedException e) {
			throw new InternalProcessingError(e);
		} catch (IncorrectStateException e) {
			throw new InternalProcessingError(e);
		} catch (TimeoutException e) {
			throw new InternalProcessingError(e);
		} catch (NoSuccessException e) {
			throw new InternalProcessingError(e);
		}
	}

	@Override
	public Session getSession() throws InternalProcessingError {
		if(session != null) return session;
		
		synchronized (this) {
			if(session == null) {
				try {
					session = SessionFactory.createSession(false);
				} catch (NotImplementedException e) {
					throw new InternalProcessingError(e);
				} catch (NoSuccessException e) {
					throw new InternalProcessingError(e);
				}
			}
			return session;
		}
		// TODO Auto-generated method stub
	}
	


}
