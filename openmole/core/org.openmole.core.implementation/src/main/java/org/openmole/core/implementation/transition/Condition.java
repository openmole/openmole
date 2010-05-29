/*
 *  Copyright (C) 2010 Romain Reuillon
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

package org.openmole.core.implementation.transition;


import groovy.lang.Binding;
import java.util.Collections;
import org.openmole.core.implementation.tools.GroovyShellProxyAdapter;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;

import org.openmole.core.workflow.model.job.IContext;
import org.openmole.core.workflow.model.transition.ICondition;
import org.openmole.commons.aspect.caching.ChangeState;
import org.openmole.commons.aspect.caching.SoftCachable;
import org.openmole.commons.tools.groovy.GroovyProxy;


public class Condition implements ICondition {

 	String code = "true";
	
	public Condition() {
		super();
	}
	
	public Condition(String code) {
		super();
		setCode(code);
	}

	@Override
	public boolean evaluate(IContext context) throws UserBadDataError, InternalProcessingError {
            final GroovyShellProxyAdapter groovyShellProxyAdapter = getGroovyShellProxyAdapter();
            
            synchronized(groovyShellProxyAdapter) {
                return (Boolean) groovyShellProxyAdapter.execute(context);
            }           
	}

        @ChangeState
	public void setCode(String code) {
		this.code = code;
	}


        @SoftCachable
	private GroovyShellProxyAdapter getGroovyShellProxyAdapter() throws InternalProcessingError, UserBadDataError {
            GroovyShellProxyAdapter ret = new GroovyShellProxyAdapter(new GroovyProxy());
            ret.compile(code, Collections.EMPTY_LIST);
            return ret;
        }


}
