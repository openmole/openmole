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

import java.util.Iterator;
import java.util.List;
import org.codehaus.groovy.tools.shell.CommandSupport;
import org.codehaus.groovy.tools.shell.Shell;
import scala.Tuple2;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public abstract class UICommand extends CommandSupport  {

    public UICommand(Shell shell, String string, String string1) {
        super(shell, string, string1);
    }

    protected Tuple2<Object, String[]> getArgs(List<String> objs) {
        Iterator<String> it = objs.iterator();
        Object obj = shell.execute(it.next());

        String[] args = new String[objs.size() - 1];
        int i = 0;

        while(it.hasNext()) {
            args[i++] = it.next();
        }

        return new Tuple2<Object, String[]>(obj, args);
    }

}
