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
import org.codehaus.groovy.tools.shell.Shell;
import org.openmole.core.model.execution.IEnvironment;
import org.openmole.core.model.mole.IMoleExecution;
import org.openmole.commons.tools.service.HierarchicalRegistry;
import org.openmole.commons.tools.structure.Duo;
import org.openmole.ui.console.internal.command.registry.Registry;
import org.openmole.ui.console.internal.command.viewer.EnvironmentViewer;
import org.openmole.ui.console.internal.command.viewer.IViewer;
import org.openmole.ui.console.internal.command.viewer.MoleExecutionViewer;
import org.openmole.ui.console.internal.command.viewer.RegistryViewer;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class Print extends UICommand {

    private HierarchicalRegistry<IViewer> viewers = new HierarchicalRegistry<IViewer>();

    public Print(Shell shell, String string, String string1) {
        super(shell, string, string1);
        viewers.register(IEnvironment.class, new EnvironmentViewer());
        viewers.register(IMoleExecution.class, new MoleExecutionViewer());
        viewers.register(Registry.class, new RegistryViewer());
    }

    @Override
    public Object execute(List list) {

        if (list.isEmpty()) {
            return null;
        }

        Duo<Object, List<Object>> args = getArgs(list);

        for (IViewer viewer : viewers.getClosestRegistred(args.getLeft().getClass())) {
            viewer.view(args.getLeft(), args.getRight());
        }

        return null;
    }
}
