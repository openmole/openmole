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
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.DomDriver;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.openide.util.Exceptions;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.implementation.mole.Mole;
import org.openmole.core.model.mole.IMole;
import org.openmole.ui.ide.exception.MoleExceptionManagement;
import org.openmole.ui.ide.workflow.implementation.MoleScene;
import org.openmole.ui.ide.workflow.model.IMoleScene;

/**
 *
 * @author mathieu
 */
public class Serializer {


    public static void serialize(MoleScene scene, String toFile) throws UserBadDataError {
        try {
            XStream xstream = new XStream(new DomDriver());
            DomMaker dommaker = new DomMaker(scene);
            xstream.registerConverter(dommaker);
            xstream.alias("mole", Mole.class);
            xstream.alias("capsule", org.openmole.core.implementation.capsule.Capsule.class);

            // dos = new DataOutputStream(new FileOutputStream(toFile));
            xstream.toXML(MoleMaker.process(scene), new FileOutputStream(toFile));
        } catch (FileNotFoundException ex) {
            MoleExceptionManagement.showException(ex);
        }

    }

    public static IMole unserialize(String fromFile) throws FileNotFoundException, IOException {
        XStream xstream = new XStream();
        IMole mole = (IMole) xstream.fromXML(new FileInputStream(fromFile));
        return mole;
    }
}
