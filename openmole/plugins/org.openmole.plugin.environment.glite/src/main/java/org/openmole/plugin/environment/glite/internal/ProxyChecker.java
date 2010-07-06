/*
 *  Copyright (C) 2010 reuillon
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
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

package org.openmole.plugin.environment.glite.internal;

import org.ogf.saga.context.Context;
import org.ogf.saga.error.AuthenticationFailedException;
import org.ogf.saga.error.AuthorizationFailedException;
import org.ogf.saga.error.DoesNotExistException;
import org.ogf.saga.error.IncorrectStateException;
import org.ogf.saga.error.NoSuccessException;
import org.ogf.saga.error.NotImplementedException;
import org.ogf.saga.error.PermissionDeniedException;
import org.ogf.saga.error.TimeoutException;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.misc.updater.IUpdatable;
import org.openmole.plugin.environment.glite.GliteEnvironmentAuthentication;

public class ProxyChecker implements IUpdatable {

    GliteEnvironmentAuthentication checkedEnv;
    Context ctx;

    public ProxyChecker(GliteEnvironmentAuthentication checkedEnv, Context ctx) {
        super();
        this.checkedEnv = checkedEnv;
        this.ctx = ctx;
    }

    public void setCtx(Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public void update() throws InternalProcessingError, UserBadDataError, InterruptedException {
        try {
            checkedEnv.initContext(ctx);
            ctx.getAttribute(Context.USERID);

           // Logger.getLogger(ProxyChecker.class.getName()).log(Level.INFO,ctx.getAttribute(Context.LIFETIME));
        } catch (NotImplementedException ex) {
            throw new InternalProcessingError(ex);
        } catch (AuthenticationFailedException ex) {
            throw new InternalProcessingError(ex);
        } catch (AuthorizationFailedException ex) {
            throw new InternalProcessingError(ex);
        } catch (PermissionDeniedException ex) {
            throw new InternalProcessingError(ex);
        } catch (IncorrectStateException ex) {
            throw new InternalProcessingError(ex);
        } catch (DoesNotExistException ex) {
            throw new InternalProcessingError(ex);
        } catch (TimeoutException ex) {
            throw new InternalProcessingError(ex);
        } catch (NoSuccessException ex) {
            throw new InternalProcessingError(ex);
        }
    }
}
