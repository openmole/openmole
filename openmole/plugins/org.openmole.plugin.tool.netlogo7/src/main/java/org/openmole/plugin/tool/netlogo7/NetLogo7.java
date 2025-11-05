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
package org.openmole.plugin.tool.netlogo7;

import org.nlogo.agent.Observer;
import org.nlogo.agent.World;
import org.nlogo.api.LogoException;
import org.nlogo.api.LogoListBuilder;
import org.nlogo.core.LogoList;
import org.nlogo.headless.HeadlessWorkspace;
import org.nlogo.nvm.Procedure;
import org.openmole.plugin.tool.netlogo.NetLogo;
import scala.collection.JavaConverters;

import java.util.AbstractCollection;
import java.util.LinkedList;

/**
 * @author Romain Reuillon
 */
public class NetLogo7 implements NetLogo {

    protected HeadlessWorkspace workspace = null;

    private HeadlessWorkspace getWorkspace() {
        if(workspace == null) {
            // If enabled NetLogo create an http connection that sometime hangs forever, see LibraryInfoDownloader
            System.setProperty("netlogo.libraries.disabled", "true");
            workspace = HeadlessWorkspace.newInstance();
        }
        return workspace;
    }

    @Override
    public void open(String script, boolean switch3d) throws Exception {
        // FIXME this is only a temporary fix - running simultaneously 3d and 2d models will fail anyway
        if (switch3d && script.endsWith("3d")) System.setProperty("org.nlogo.is3d", "true");
        else System.setProperty("org.nlogo.is3d", "false");
        getWorkspace().open(script, false);
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
        Object result = getWorkspace().report(variable);
        if(result instanceof LogoList){
            return listToCollection((LogoList) result);
        }else {
            return result;
        }
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
        for (scala.Tuple2<String, Procedure> e : JavaConverters.asJavaCollectionConverter(getWorkspace().procedures()).asJavaCollection()) {
            if (e._2().isReporter()) reporters.add(e._1());
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
    private static LogoList arrayToList(Object[] array){
        LogoListBuilder list = new LogoListBuilder();
        for(Object o:array){
            if(o instanceof Object[]){list.add(arrayToList((Object[]) o));}
            else{list.add(o);}
        }
        return(list.toLogoList());
    }


    private static AbstractCollection listToCollection(LogoList list){
        AbstractCollection collection = new LinkedList();
        for(Object o:list.toJava()){
            if(o instanceof LogoList){collection.add(listToCollection((LogoList) o));}
            else{collection.add(o);}
        }
        return(collection);
    }



}
