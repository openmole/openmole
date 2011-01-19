/*
 *  Copyright (C) 2010 mathieu
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
package org.openmole.ui.ide.serializer;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Iterator;
import org.openmole.ui.ide.control.MoleScenesManager;
import org.openmole.ui.ide.exception.MoleExceptionManagement;
import org.openmole.ui.ide.workflow.implementation.IEntityUI;
import org.openmole.ui.ide.workflow.implementation.MoleScene;
import org.openmole.ui.ide.workflow.implementation.PrototypeUI;
import org.openmole.ui.ide.workflow.implementation.PrototypesUI;
import org.openmole.ui.ide.workflow.implementation.TaskUI;
import org.openmole.ui.ide.workflow.implementation.TasksUI;
import org.openmole.ui.ide.workflow.model.IMoleScene;

/**
 *
 * @author mathieu
 */
public class GUISerializer {

    private static GUISerializer instance;
    XStream xstream = new XStream(new DomDriver());
    final Class moleSceneClass = MoleScene.class;
    final Class prototypeClass = PrototypeUI.class;
    final Class taskClass = TaskUI.class;

    public GUISerializer() {
        xstream = new XStream(new DomDriver());
        xstream.registerConverter(new MoleSceneConverter());
        xstream.registerConverter(new PrototypeConverter());
        xstream.registerConverter(new TaskConverter());
        xstream.alias("molescene", moleSceneClass);
        xstream.alias("prototype", prototypeClass);
        xstream.alias("task", taskClass);
    }

    public void serialize(String toFile) throws IOException {
        FileWriter writer = new FileWriter(new File(toFile));

        //root node
        ObjectOutputStream out = xstream.createObjectOutputStream(writer, "openmole");

        //prototypes
        for (Iterator<IEntityUI> itp = PrototypesUI.getInstance().getAll().iterator(); itp.hasNext();) {
            out.writeObject(itp.next());
        }

        //tasks
        for (Iterator<IEntityUI> itt = TasksUI.getInstance().getAll().iterator(); itt.hasNext();) {
            out.writeObject(itt.next());
        }

        //molescenes
        for (Iterator<IMoleScene> itms = MoleScenesManager.getInstance().getMoleScenes().iterator(); itms.hasNext();) {
            out.writeObject(itms.next());
        }

        out.close();
    }

    public void unserialize(String fromFile) throws FileNotFoundException, IOException, EOFException {

        FileReader reader = new FileReader(new File(fromFile));

        ObjectInputStream in = xstream.createObjectInputStream(reader);
        PrototypesUI.getInstance().clearAll();
        TasksUI.getInstance().clearAll();
        MoleScenesManager.getInstance().removeMoleScenes();
        
        Object readObject;
        for (int i = 0;; i++) {
            try {
                readObject = in.readObject();
                if (readObject.getClass().equals(prototypeClass)) {
                    PrototypesUI.getInstance().register((PrototypeUI) readObject);
                } else if (readObject.getClass().equals(taskClass)) {
                    TasksUI.getInstance().register((TaskUI) readObject);
                } else if (readObject.getClass().equals(moleSceneClass)) {
                    MoleScenesManager.getInstance().addMoleScene((IMoleScene) readObject);
                }
            } catch (ClassNotFoundException ex) {
                MoleExceptionManagement.showException("Error reading the xml file: "+ex.getMessage());
            }
        }
    }

    public static GUISerializer getInstance() {
        if (instance == null) {
            instance = new GUISerializer();
        }
        return instance;
    }
}
