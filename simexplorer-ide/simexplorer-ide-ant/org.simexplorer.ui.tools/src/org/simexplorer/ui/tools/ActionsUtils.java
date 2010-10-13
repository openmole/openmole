/*
 *
 *  Copyright (c) 2009, Cemagref
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License as
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
package org.simexplorer.ui.tools;

import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

public class ActionsUtils {

    /**
     * Build a File Chooser and initialize the current directory of the dialog
     * either to the dir passed as arg, either to the user home dir.
     * @param dialogTitle
     * @param currentDir could be null. If it is a file, the parent dir will be used.
     * @return
     */
    public static JFileChooser getJFileChooser(String dialogTitle, File currentDir) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle(dialogTitle);
        if (currentDir != null) {
            fc.setCurrentDirectory(currentDir);
        } else {
            // open the filechooser in the Home directory
            fc.setCurrentDirectory(new java.io.File(System.getProperty("user.home")));
        }
        return fc;
    }
    public static JFileChooser getJFileChooser(String dialogTitle, File currentDir, String fileExtensionDescription, String ... fileExtension) {
        JFileChooser fc = getJFileChooser(dialogTitle, currentDir);
     FileFilter filter = new FileNameExtensionFilter(fileExtensionDescription,fileExtension);
     fc.addChoosableFileFilter(filter);
        return fc;
    }
}
