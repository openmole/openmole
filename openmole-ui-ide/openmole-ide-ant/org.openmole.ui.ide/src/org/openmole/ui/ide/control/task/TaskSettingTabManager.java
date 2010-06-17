/*
 *  Copyright (C) 2010 Mathieu Leclaire <mathieu.leclaire@openmole.fr>
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

package org.openmole.ui.ide.control.task;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.Map;
import java.util.WeakHashMap;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import org.openmole.ui.ide.workflow.model.ITaskCapsuleView;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public class TaskSettingTabManager {
    private static TaskSettingTabManager instance = null;
    private Map<ITaskCapsuleView,Component> taskSettingMap = new WeakHashMap<ITaskCapsuleView,Component>();
    private JTabbedPane tabbedPane;

    public void setTabbedPane(JTabbedPane tabbedPane) {
        this.tabbedPane = tabbedPane;
    }

    public void display(ITaskCapsuleView tcv){
        if (!taskSettingMap.containsKey(tcv)){
            addTaskSettingTab(tcv);
        }
        tabbedPane.setSelectedComponent(taskSettingMap.get(tcv));
    }

    private void addTaskSettingTab(ITaskCapsuleView tcv){
        taskSettingMap.put(tcv,new ContainerComposer( new IOContainer(),
                                                      new IOContainer())) ;
        System.out.println("ADD " + taskSettingMap.get(tcv));
        tabbedPane.add(tcv.getName(),taskSettingMap.get(tcv));

    }

  /*  public Component getComponent(TaskCapsuleViewUI tcv){
        return taskSettingMap.get(tcv);
    }*/

    public static TaskSettingTabManager getInstance() {
        if (instance == null) {
            instance = new TaskSettingTabManager();
        }
        return instance;
    }

}
