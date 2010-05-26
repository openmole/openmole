/*
 *  Copyright (C) 2010 Romain Reuillon <romain.reuillon at openmole.org>
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

package org.openmole.misc.workspace.internal;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.misc.workspace.IPasswordProvider;
import org.openmole.misc.workspace.IPasswordProvider;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class StaticPasswordProvider implements IPasswordProvider {

    final String password;

    public StaticPasswordProvider(String password) {
        this.password = password;
    }


    @Override
    public String getPassword() throws InternalProcessingError {
        return password;
    }

}
