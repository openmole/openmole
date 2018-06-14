/*
 * Copyright (C) 2011 Romain Reuillon
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
package org.openmole.plugin.tool.netlogo5;

import org.nlogo.agent.Observer;
import org.nlogo.agent.World;
import org.nlogo.api.LogoException;
import org.nlogo.api.LogoList;
import org.nlogo.api.LogoListBuilder;
import org.nlogo.headless.HeadlessWorkspace;
import org.nlogo.nvm.Procedure;
import org.openmole.plugin.tool.netlogo.NetLogo;

import java.util.LinkedList;
import java.util.Map;

/**
 * @author Romain Reuillon
 */
public class NetLogo5 implements NetLogo {

    protected HeadlessWorkspace workspace = null;

    private HeadlessWorkspace getWorkspace() {
        if(workspace == null) {
            workspace = HeadlessWorkspace.newInstance();
        }
        return workspace;
    }

    @Override
    public void open(String script) throws Exception {
        getWorkspace().open(script);
    }

    @Override
    public void command(String cmd) throws Exception {
        getWorkspace().command(cmd);
    }

    @Override
    public boolean isNetLogoException(Throwable e) {
        return LogoException.class.isAssignableFrom(e.getClass());
    }

    @Override
    public Object report(String variable) throws Exception {
        return getWorkspace().report(variable);
    }

    @Override
    public void setGlobal(String variable, Object value) throws Exception {
        if(value instanceof Object[]){
            workspace.world().setObserverVariableByName(variable,arrayToList((Object[]) value));
        }
        else{
            workspace.world().setObserverVariableByName(variable,value);
        }
    }

    @Override
    public void dispose() throws Exception {
        getWorkspace().dispose();
    }

    @Override
    public String[] globals() {
        World world = getWorkspace().world();
        Observer observer = world.observer();
        String nlGlobalList[] = new String[world.getVariablesArraySize(observer)];
        for (int i = 0; i < nlGlobalList.length; i++) {
            nlGlobalList[i] = world.observerOwnsNameAt(i);
        }
        return nlGlobalList;
    }

    public String[] reporters() {
        LinkedList<String> reporters = new LinkedList<String>();
        for (Map.Entry<String, Procedure> e : getWorkspace().getProcedures().entrySet()) {
            if (e.getValue().tyype == Procedure.Type.REPORTER) reporters.add(e.getKey());
        }
        return reporters.toArray(new String[0]);
    }

    @Override
    public ClassLoader getNetLogoClassLoader() {
        return HeadlessWorkspace.class.getClassLoader();
    }

    /**
     * Converts an iterable to a LogoList
     * @param array
     * @return
     */
    public static LogoList arrayToList(Object[] array){
        LogoListBuilder list = new LogoListBuilder();
        for(Object o:array){
            if(o instanceof Object[]){list.add(arrayToList((Object[]) o));}
            else{list.add(o);}
        }
        return(list.toLogoList());
    }

}
