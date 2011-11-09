/*
 *  Copyright (C) 2010 reuillon
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 * 
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.plugin.task.netlogo5;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.nlogo.api.CompilerException;
import org.nlogo.api.LogoException;
import org.nlogo.headless.HeadlessWorkspace;
import org.nlogo.agent.World;
import org.nlogo.agent.Observer;
import org.openmole.misc.exception.InternalProcessingError;
import org.openmole.misc.exception.UserBadDataError;
import org.openmole.plugin.task.netlogo.NetLogo;
import org.openmole.plugin.task.netlogo.NetLogoFactory;
import org.openmole.plugin.task.netlogo.NetLogoTask;
import scala.collection.JavaConversions;

/**
 *
 * @author reuillon
 */
public class NetLogo5Task extends NetLogoTask {


    public NetLogo5Task(String name,
            File workspace,
            String scriptName,
            Iterable<String> launchingCommands) throws UserBadDataError, InternalProcessingError {
        super(name, workspace, scriptName, JavaConversions.iterableAsScalaIterable(launchingCommands));
    }

    public NetLogo5Task(String name,
            String workspace,
            String scriptName,
            Iterable<String> launchingCommands) throws UserBadDataError, InternalProcessingError {
        super(name, workspace, scriptName, JavaConversions.iterableAsScalaIterable(launchingCommands));
    }
    
    public NetLogo5Task(String name,
            File script,
            Iterable<String> launchingCommands) throws UserBadDataError, InternalProcessingError {
        super(name, script, JavaConversions.iterableAsScalaIterable(launchingCommands));
    }
    
    public NetLogo5Task(String name,
            String script,
            Iterable<String> launchingCommands) throws UserBadDataError, InternalProcessingError {
        super(name, script, JavaConversions.iterableAsScalaIterable(launchingCommands));
    }
    
    @Override 
    public NetLogoFactory netLogoFactory() {
        return new NetLogoFactory() {
            public NetLogo apply() {
                return new NetLogo() {
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
                    
                    public void dispose() throws Exception {
                        workspace.dispose();
                    }
                    
                    @Override
                    public List globals() {                
                        World world = workspace.world();
                        Observer observer = world.observer();
                        List nlGlobalList = new ArrayList(world.getVariablesArraySize(observer));
                        for(int i=0;i<nlGlobalList.size();i++){
                            nlGlobalList.add(world.observerOwnsNameAt(i));
                        }
                        return nlGlobalList;
                    }
                };
            }
        };
    }
}
