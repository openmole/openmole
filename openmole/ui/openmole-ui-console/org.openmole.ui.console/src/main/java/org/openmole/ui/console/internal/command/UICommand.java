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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.codehaus.groovy.tools.shell.CommandSupport;
import org.codehaus.groovy.tools.shell.Shell;
import org.openmole.commons.tools.structure.Duo;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public abstract class UICommand extends CommandSupport  {

    public UICommand(Shell shell, String string, String string1) {
        super(shell, string, string1);
    }

    protected Duo<Object, List<Object>> getArgs(List<Object> objs) {
        Iterator<Object> it = objs.iterator();

        Object obj = shell.execute((String) it.next());
        List<Object> args = new ArrayList<Object>(objs.size() - 1);

        while(it.hasNext()) {
            args.add(shell.execute((String) it.next()));
        }

        return new Duo<Object, List<Object>>(obj, args);
    }

}
