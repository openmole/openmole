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
package org.openmole.core.jsagasession.internal;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.ogf.saga.context.Context;
import org.ogf.saga.context.ContextFactory;
import org.ogf.saga.error.IncorrectStateException;
import org.ogf.saga.error.NoSuccessException;
import org.ogf.saga.error.NotImplementedException;
import org.ogf.saga.error.TimeoutException;
import org.ogf.saga.session.Session;
import org.ogf.saga.session.SessionFactory;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.core.jsagasession.IJSagaSessionService;

public class JSagaSessionService implements IJSagaSessionService {

    Session session;

    @Override
    public void addContext(Context context) throws InternalProcessingError {
        try {
            getSession().addContext(context);
        } catch (Exception e) {
            throw new InternalProcessingError(e);
        }
    }

    @Override
    public Context createContext() throws InternalProcessingError {
        try {
            return ContextFactory.createContext();
        } catch (Exception e) {
            throw new InternalProcessingError(e);
        }
    }

    @Override
    public Session getSession() throws InternalProcessingError {
        if (session != null) {
            return session;
        }

        synchronized (this) {
            if (session == null) {
                try {
                    session = SessionFactory.createSession(false);
                } catch (Exception e) {
                    throw new InternalProcessingError(e);
                }
            }
            return session;
        }
    }
}
