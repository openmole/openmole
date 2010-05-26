/*
 *  Copyright (C) 2010 Cemagref
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

package org.simexplorer.openmole.plugin.task.sensitivitytask;

import java.util.Iterator;
import org.openmole.core.workflow.implementation.domain.Domain;
import org.openmole.core.workflow.model.job.IContext;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;

/**
 *
 * @author Nicolas Dumoulin <nicolas.dumoulin@cemagref.fr>
 */
public class RFunctionDomain<T extends Object> extends Domain<T> {

    private String function;
    private String[] arguments;

    public RFunctionDomain(String function, String... arguments) {
        this.function = function;
        this.arguments = arguments;
    }

    public String[] getArguments() {
        return arguments;
    }

    public String getFunction() {
        return function;
    }

    @Override
    public Iterator<T> iterator(IContext context) throws UserBadDataError, InternalProcessingError {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
