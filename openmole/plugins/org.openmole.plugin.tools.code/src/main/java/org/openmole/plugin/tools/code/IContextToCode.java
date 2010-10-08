/*
 *  Copyright (C) 2010 Romain Reuillon <romain.reuillon at openmole.org>
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

package org.openmole.plugin.tools.code;

import java.io.File;
import org.openmole.core.model.data.IData;

import org.openmole.core.model.data.IVariable;
import org.openmole.core.model.execution.IProgress;
import org.openmole.core.model.job.IContext;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public interface IContextToCode {
    void setCode(ISourceCode sourceCode) throws UserBadDataError, InternalProcessingError;
    String getCode();
    Object execute(IContext global, IContext context) throws UserBadDataError, InternalProcessingError;
    Object execute(IContext global, IContext context, Iterable<IVariable> tmpVariables) throws UserBadDataError, InternalProcessingError;
    Object execute(IContext global, IContext context, Iterable<IVariable> tmpVariables, Iterable<File> libs) throws UserBadDataError, InternalProcessingError;
    Object execute(IContext global, IContext context, IProgress progress, Iterable<IData<?>> output, Iterable<File> libs) throws UserBadDataError, InternalProcessingError;
    Object execute(IContext global, IContext context, Iterable<IVariable> tmpVariables, IProgress progress, Iterable<IData<?>> output, Iterable<File> libs) throws UserBadDataError, InternalProcessingError;

}
