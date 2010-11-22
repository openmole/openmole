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

package org.openmole.ui.console.internal.command.initializer;

import java.io.IOException;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.misc.workspace.IWorkspace;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class WorkspaceInitializer implements IInitializer<IWorkspace> {

    @Override
    public void initialize(IWorkspace object, Class c) {
        //System.out.println("Enter your password:");
        //BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        try {
            String password = new jline.ConsoleReader().readLine("Enter your password:", new Character('*'));
            object.password_$eq(password);
        } catch (IOException e) {
            throw new Error(e);
        }
    }

}
