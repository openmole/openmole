/*
 * Copyright (C) 2011 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.plugin.task.netlogo4;

import java.util.ArrayList;
import java.util.List;
import org.nlogo.agent.World;
import org.nlogo.agent.Observer;
import org.nlogo.headless.HeadlessWorkspace;
import org.openmole.plugin.task.netlogo.NetLogo;

/**
 *
 * @author reuillon
 */
public class NetLogo4 implements NetLogo {

    private HeadlessWorkspace workspace = HeadlessWorkspace.newInstance();

    @Override
    public void open(String script) throws Exception {
        workspace.open(script);
    }

    @Override
    public void command(String cmd) throws Exception {
        workspace.command(cmd);
    }

    @Override
    public Object report(String variable) throws Exception {
        return workspace.report(variable);
    }
    
    @Override
    public void dispose() throws Exception {
        workspace.dispose();
    }

    @Override
    public String[] globals() {
        World world = workspace.world();
        Observer observer = world.observer();
        String nlGlobalList[] = new String[world.getVariablesArraySize(observer)];
        for (int i = 0; i < nlGlobalList.length; i++) {
            nlGlobalList[i] = world.observerOwnsNameAt(i);
        }
        return nlGlobalList;
    }
}
