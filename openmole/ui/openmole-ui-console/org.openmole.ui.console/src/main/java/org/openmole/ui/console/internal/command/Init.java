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
package org.openmole.ui.console.internal.command;

import java.util.List;
import org.codehaus.groovy.tools.shell.CommandSupport;
import org.codehaus.groovy.tools.shell.Shell;
import org.openmole.core.model.execution.IEnvironment;
import org.openmole.commons.tools.service.HierarchicalRegistry;
import org.openmole.misc.workspace.IWorkspace;
import org.openmole.ui.console.internal.command.initializer.EnvironmentInitializer;
import org.openmole.ui.console.internal.command.initializer.IInitializer;
import org.openmole.ui.console.internal.command.initializer.WorkspaceInitializer;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class Init extends CommandSupport {

    private HierarchicalRegistry<IInitializer> initializers = new HierarchicalRegistry<IInitializer>();

    public Init(Shell shell, String string, String string1) {
        super(shell, string, string1);
        initializers.register(IWorkspace.class, new WorkspaceInitializer());
        initializers.register(IEnvironment.class, new EnvironmentInitializer());
    }

    @Override
    public Object execute(List list) {
        for (Object arg : list) {
            Object obj = shell.execute((String) arg);

            if(obj.getClass() == Class.class) {
                 for (IInitializer initializer : initializers.getClosestRegistred((Class)obj)) {
                    initializer.initialize(null,(Class) obj);
                 }
            } else {
                for (IInitializer initializer : initializers.getClosestRegistred(obj.getClass())) {
                    initializer.initialize(obj, obj.getClass());
                 }
            }

        }
        return null;
    }
}
