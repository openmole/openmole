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
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import org.openmole.core.model.mole.IMole;
import org.openmole.ui.ide.exception.MoleExceptionManagement;

/**
 *
 * @author mathieu
 */
public class Serializer {

    public static void serialize(IMole mole, String toFile) {
        DataOutputStream dos = null;
        try {
            XStream xstream = new XStream();
            dos = new DataOutputStream(new FileOutputStream(toFile));
            dos.writeUTF(xstream.toXML(mole));
        } catch (IOException ex) {
            MoleExceptionManagement.showException(ex);
        } finally {
            try {
                dos.close();
            } catch (IOException ex) {
                MoleExceptionManagement.showException(ex);
            }
        }
    }

    public static IMole unserialize(String fromFile) {
        XStream xstream = new XStream();
        return (IMole) xstream.fromXML(fromFile);
    }
}
