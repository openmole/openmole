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

package org.openmole.ui.console.internal.command;

import java.util.List;
import org.codehaus.groovy.tools.shell.CommandSupport;
import org.codehaus.groovy.tools.shell.Shell;
import org.openmole.commons.tools.structure.Duo;
import org.openmole.ui.console.internal.command.registry.Registry;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class Register extends UICommand {

  //  List<Object> registred

    public Register(Shell shell, String string, String string1) {
        super(shell, string, string1);
    }

    @Override
    public Object execute(List list) {
        Duo<Object, List<Object>> ags = getArgs(list);
        Registry.getInstance().register(ags.getLeft());
        return null;
    }

}
