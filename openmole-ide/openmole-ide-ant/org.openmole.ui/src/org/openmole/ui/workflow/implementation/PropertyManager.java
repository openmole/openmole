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
package org.openmole.ui.workflow.implementation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import org.openmole.ui.exception.MoleExceptionManagement;
import org.openmole.ui.palette.Category;
import org.openmole.ui.palette.Category.CategoryName;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public class PropertyManager {

    public static final String NAME = "name";
    public static final String IMPL = "impl";
    public static final String BG_COLOR = "bg-color";
    public static final String BORDER_COLOR = "border-color";
    public static final String BG_IMG = "bg-img";
    public static final String THUMB_IMG = "thumb-img";

    public static void readProperties(CategoryName cat) {

        File actual = new File("src/resources/" + Category.toString(cat) + "/");
        for (File f : actual.listFiles()) {
            Properties props = read(f.getPath());
            try {
                Preferences.getInstance().registerProperties(cat,
                                                             Class.forName(f.getName()),
                                                             props);
                
                Preferences.getInstance().register(cat,
 		                                             Class.forName(f.getName()),
 		                                             props);
            } catch (ClassNotFoundException ex) {
                MoleExceptionManagement.showException(ex);
            }
        }
    }

    public static Properties read(String path) {
        try {
            Properties props = new Properties();
            FileReader reader = new FileReader(path);
            props.load(reader);
            reader.close();
            return props;
        } catch (FileNotFoundException ex) {
            MoleExceptionManagement.showException(ex);
        } catch (IOException ex) {
            MoleExceptionManagement.showException(ex);
        }
        return null;
    }
}
