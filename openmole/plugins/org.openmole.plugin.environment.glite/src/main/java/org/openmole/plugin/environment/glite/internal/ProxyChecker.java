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

import java.util.logging.Level;
import java.util.logging.Logger;
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
import org.openmole.plugin.environment.glite.GliteAuthentication;

public class ProxyChecker implements IUpdatable {

    GliteAuthentication checkedEnv;
    Context ctx;

    public ProxyChecker(GliteAuthentication checkedEnv, Context ctx) {
        super();
        this.checkedEnv = checkedEnv;
        this.ctx = ctx;
    }

    public void setCtx(Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public boolean update() throws InterruptedException {
        try {
            checkedEnv.initContext(ctx);
            ctx.getAttribute(Context.USERID);
        } catch (NotImplementedException ex) {
            Logger.getLogger(ProxyChecker.class.getName()).log(Level.SEVERE, "Error while renewing the proxy.", ex);
        } catch (AuthenticationFailedException ex) {
            Logger.getLogger(ProxyChecker.class.getName()).log(Level.SEVERE, "Error while renewing the proxy.", ex);
        } catch (AuthorizationFailedException ex) {
            Logger.getLogger(ProxyChecker.class.getName()).log(Level.SEVERE, "Error while renewing the proxy.", ex);
        } catch (PermissionDeniedException ex) {
            Logger.getLogger(ProxyChecker.class.getName()).log(Level.SEVERE, "Error while renewing the proxy.", ex);
        } catch (IncorrectStateException ex) {
            Logger.getLogger(ProxyChecker.class.getName()).log(Level.SEVERE, "Error while renewing the proxy.", ex);
        } catch (DoesNotExistException ex) {
            Logger.getLogger(ProxyChecker.class.getName()).log(Level.SEVERE, "Error while renewing the proxy.", ex);
        } catch (TimeoutException ex) {
            Logger.getLogger(ProxyChecker.class.getName()).log(Level.SEVERE, "Error while renewing the proxy.", ex);
        } catch (NoSuccessException ex) {
            Logger.getLogger(ProxyChecker.class.getName()).log(Level.SEVERE, "Error while renewing the proxy.", ex);
        } catch (InternalProcessingError ex) {
            Logger.getLogger(ProxyChecker.class.getName()).log(Level.SEVERE, "Error while renewing the proxy.", ex);
        } catch (UserBadDataError ex) {
            Logger.getLogger(ProxyChecker.class.getName()).log(Level.SEVERE, "Error while renewing the proxy.", ex);
        }
        
        return true;
    }
    

}
