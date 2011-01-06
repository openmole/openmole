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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import org.openmole.core.model.mole.IMole;
import org.openmole.ui.ide.workflow.implementation.MoleSceneManager;
import org.openmole.ui.ide.workflow.implementation.PrototypeUI;

/**
 *
 * @author mathieu
 */
public class GUISerializer {

    private static GUISerializer instance;
    XStream xstream = new XStream(new DomDriver());

    public GUISerializer() {
        xstream = new XStream(new DomDriver());
        xstream.registerConverter(new MoleSceneConverter());
        xstream.alias("openmole", OpenMole.class);
        xstream.alias("prototype", PrototypeUI.class);
        xstream.alias("capsule", org.openmole.core.implementation.capsule.Capsule.class);
        xstream.alias("islot", org.openmole.core.implementation.transition.Slot.class);
        xstream.alias("oslot", org.openmole.core.implementation.transition.Slot.class);
    }

    public void serialize(String toFile) throws IOException {
        FileOutputStream fos = new FileOutputStream(toFile);
        xstream.toXML(new OpenMole(), fos);
        fos.close();
    }

    public void unserialize(String fromFile) throws FileNotFoundException, IOException {
        FileInputStream fos = new FileInputStream(fromFile);
        xstream.fromXML(fos);
        fos.close();
    }

    public static GUISerializer getInstance() {
        if (instance == null) {
            instance = new GUISerializer();
        }
        return instance;
    }

    public class OpenMole{}
}
