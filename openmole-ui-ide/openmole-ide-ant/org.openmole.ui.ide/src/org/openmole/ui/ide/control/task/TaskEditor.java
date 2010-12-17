/*
 * Copyright (C) 2010 mathieu leclaire <mathieu.leclaire@openmole.org>
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
package org.openmole.ui.ide.control.task;

import java.io.File;
import java.io.IOException;
import javax.swing.JEditorPane;
import javax.swing.text.EditorKit;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.text.CloneableEditorSupport;
import org.openide.util.Exceptions;
import org.openide.windows.TopComponent;
import javax.swing.text.Document;
import org.netbeans.api.editor.DialogBinding;

/**
 *
 * @author mathieu leclaire <mathieu.leclaire@openmole.org>
 */
public class TaskEditor extends JEditorPane {

    public TaskEditor() {
        EditorKit kit = CloneableEditorSupport.getEditorKit("text/x-groovy");
        setEditorKit(kit);
        FileObject fob;
        try {
            //Create a file in memory:
            fob = FileUtil.createMemoryFileSystem().getRoot().createData("tmp", "groovy");
            DataObject dob = DataObject.find(fob);
            getDocument().putProperty(
                    Document.StreamDescriptionProperty,
                    dob);
            DialogBinding.bindComponentToFile(fob, 0, 0, this);
            //Provide some default content:
//            setText("def scores = [80, 90, 70]\n\n\"\"\"Maximum: ${scores.max()}\n"
//                    + "Minimum: ${scores.min()}\"\"\"");
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        //add(editorPane, java.awt.BorderLayout.CENTER);
    }
}
