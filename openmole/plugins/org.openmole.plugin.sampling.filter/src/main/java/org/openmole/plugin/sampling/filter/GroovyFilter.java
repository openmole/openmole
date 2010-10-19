/*
 *
 *  Copyright (c) 2008, Cemagref
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation; either version 3 of
 *  the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this program; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston,
 *  MA  02110-1301  USA
 */
package org.openmole.plugin.sampling.filter;

import groovy.lang.Binding;
import java.util.Collections;
import org.openmole.core.implementation.data.Prototype;
import org.openmole.core.model.data.IPrototype;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.commons.tools.groovy.GroovyProxy;

import org.openmole.commons.aspect.caching.SoftCachable;
import org.openmole.core.model.sampling.IValues;
import org.openmole.plugin.tools.code.ISourceCode;

public class GroovyFilter implements IFilter {

    final static private IPrototype<IValues> factorValuesPrototype = new Prototype<IValues>("factorValues", IValues.class);

    private String script;

    public GroovyFilter(ISourceCode code) throws UserBadDataError, InternalProcessingError {
        script = code.getCode();
    }

    @SoftCachable
    public GroovyProxy getCodeEditor() throws InternalProcessingError, UserBadDataError {
        GroovyProxy groovyProxy = new GroovyProxy();
        groovyProxy.compile(script, Collections.EMPTY_LIST);
        return groovyProxy;
    }

    public String getScript() {
        return script;
    }


    @Override
    public boolean accept(IValues factorsValues) throws UserBadDataError, InternalProcessingError {
        Binding binding = new Binding();
        binding.setVariable(factorValuesPrototype.getName(), factorsValues);
        return (java.lang.Boolean) getCodeEditor().execute(binding);
    }
}
