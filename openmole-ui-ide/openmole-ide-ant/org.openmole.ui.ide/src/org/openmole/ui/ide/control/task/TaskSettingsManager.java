/*
 *  Copyright (C) 2010 Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the Affero GNU General Public License as published by
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

import java.awt.Component;
import java.io.IOException;
import javax.swing.JEditorPane;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.text.CloneableEditorSupport;
import org.openide.util.Exceptions;
import org.openmole.ui.ide.control.TabManager;
import org.openmole.ui.ide.workflow.model.ICapsuleView;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public class TaskSettingsManager extends TabManager {

    private static TaskSettingsManager instance = null;


    @Override
    public void addTab(Object displayed) {
        ICapsuleView tcv = (ICapsuleView) displayed;
        JEditorPane editorPane = new JEditorPane();

         EditorKit kit = CloneableEditorSupport.getEditorKit("text/x-groovy");
        editorPane.setEditorKit(kit);
        FileObject fob;
        try {
            fob = FileUtil.createMemoryFileSystem().getRoot().createData("tmp",
                    "groovy");
            //fob = FileUtil.getConfigRoot().createData("tmp", "groovy");
            DataObject dob = DataObject.find(fob);
            editorPane.getDocument().putProperty(
                    Document.StreamDescriptionProperty,
                    dob);
            //DialogBinding.bindComponentToFile(fob, 0, 0, editorPane);
            editorPane.setText("package dummy;");
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

//        taskSettingMap.put(tcv, editorPane);
//        tabbedPane.add(tcv.getName(), taskSettingMap.get(tcv));
        addMapping(tcv, editorPane,tcv.getCapsuleModel().getTaskModel().getName());
    }


//    private void addTaskSettingTab(ICapsuleView tcv) {
//        //    taskSettingMap.put(tcv,new ContainerComposerBuilder().setSplitOrientation(JSplitPane.VERTICAL_SPLIT)
//    /*    taskSettingMap.put(tcv,new ContainerComposerBuilder().setSplitOrientation(JSplitPane.VERTICAL_SPLIT)
//        .addComponent(new IOContainer())
//        .addComponent(new IOContainer())
//        //                                                                                   .setSplitOrientation(JSplitPane.HORIZONTAL_SPLIT)
//        .addComponent(new IOContainer())
//        .addComponent(new IOContainer())
//        .addComponent(new IOContainer())
//        .addComponent(new IOContainer())
//        .build());*/
//
//        JEditorPane editorPane = new JEditorPane();
//
//         EditorKit kit = CloneableEditorSupport.getEditorKit("text/x-groovy");
//        editorPane.setEditorKit(kit);
//        FileObject fob;
//        try {
//            fob = FileUtil.createMemoryFileSystem().getRoot().createData("tmp",
//                    "groovy");
//            //fob = FileUtil.getConfigRoot().createData("tmp", "groovy");
//            DataObject dob = DataObject.find(fob);
//            editorPane.getDocument().putProperty(
//                    Document.StreamDescriptionProperty,
//                    dob);
//            //DialogBinding.bindComponentToFile(fob, 0, 0, editorPane);
//            editorPane.setText("package dummy;");
//        } catch (IOException ex) {
//            Exceptions.printStackTrace(ex);
//        }
//        taskSettingMap.put(tcv, editorPane);
//        tabbedPane.add(tcv.getName(), taskSettingMap.get(tcv));
//    }

    public static TaskSettingsManager getInstance() {
        if (instance == null) {
            instance = new TaskSettingsManager();
        }
        return instance;
    }

}
