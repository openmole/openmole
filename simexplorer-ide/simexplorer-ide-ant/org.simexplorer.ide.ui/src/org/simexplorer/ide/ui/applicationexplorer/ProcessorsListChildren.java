/*
 *
 *  Copyright © 2007, 2008, Cemagref
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation; either version 3 of
 *  the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this program; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston,
 *  MA  02110-1301  USA
 */
package org.simexplorer.ide.ui.applicationexplorer;

import javax.swing.event.ChangeListener;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.simexplorer.ui.ide.workflow.model.TasksList;
import org.openmole.core.implementation.task.Task;

public class ProcessorsListChildren extends Children.Keys<Object> {

    protected TasksList processorsList;
    private ChangeListener listener;


    public ProcessorsListChildren(TasksList processorsList) {
        this.processorsList = processorsList;
        setKeys(processorsList.getChildren());
    }


    /*@Override
    protected void addNotify() {
        List li = new LinkedList();
        li.addAll(processorsList);
        if (null != processorsList.getLibraries()) {
            li.addAll(processorsList.getLibraries());
        }
        setKeys(li);
    }*/

    @Override
    protected Node[] createNodes(Object o) {
        if (o instanceof TasksList) {
            return new Node[]{new ProcessorsListNode<TasksList>((TasksList) o)};
        } else if (o instanceof Task) {
            return new Node[]{new ItemProcessorNode((Task) o, this)};
        }
        throw new UnsupportedOperationException("Not handled node type: " + o.getClass());
    }

    public int indexOf(Task processor) {
        return processorsList.indexOf(processor);
    }

    public int size() {
        return processorsList.size();
    }

    public void moveUp(Task processor) {
        int index = processorsList.indexOf(processor);
        processorsList.remove(processor);
        processorsList.add(index-1, processor);
        this.setKeys(processorsList.getChildren());
    }

    public void moveDown(Task processor) {
        int index = processorsList.indexOf(processor);
        processorsList.remove(processor);
        processorsList.add(index+1, processor);
        this.setKeys(processorsList.getChildren());
    }

    public void add(Task processor) {
        processorsList.add(processor);
        this.setKeys(processorsList.getChildren());
    }

    public void removeProcessor(Task processor) {
        processorsList.remove(processor);
        this.setKeys(processorsList.getChildren());
    }

    protected void removeNotify() {
   /*     if (listener != null) {
            PropertiesNotifier.removeChangeListener(listener);
            listener = null;
        }
        setKeys(Collections.EMPTY_SET);
    */}

    private void refreshList() {
/*        List keys = new ArrayList();
        Properties p = System.getProperties();
        Enumeration e = p.propertyNames();
        while (e.hasMoreElements()) keys.add(e.nextElement());
        Collections.sort(keys);
        setKeys(keys);
  */  }


}
