/*
 * Copyright (C) 2011 <mathieu.leclaire at openmole.org>
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

package org.openmole.ide.core.implementation.action

import java.io.File
import javax.swing.filechooser.FileNameExtensionFilter
import scala.swing.FileChooser.SelectionMode._
import org.openide.windows.WindowManager
import org.openmole.ide.core.implementation.control.Settings
import org.openmole.ide.core.implementation.serializer.GUISerializer
import scala.swing.FileChooser.Result.Approve
import scala.swing.Component
import scala.swing.FileChooser
import scala.swing.Label

object SaveXML {
  def save:Unit = SaveXML.save(Settings.currentProject.getOrElse(SaveXML.show))
  
  def save(title: String): Unit = {
    Settings.currentProject = Some(title)
    WindowManager.getDefault.getMainWindow.setTitle(title)
    GUISerializer.serialize(title)
    }
  
  def show : String = {
    val fc = new FileChooser {
      new FileNameExtensionFilter("Save", ".xml,.XML")
      fileSelectionMode = FilesOnly
      title ="Save OpenMOLE project"
    }
  
    var text = ""
    if (fc.showDialog(new Label,"OK") == Approve) text = fc.selectedFile.getPath
    if (new File(text).getParentFile.isDirectory) text= text.split('.')(0)+".xml"
    text
  }
}