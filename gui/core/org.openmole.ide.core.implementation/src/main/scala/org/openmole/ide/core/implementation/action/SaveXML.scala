/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
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
import scala.swing.FileChooser.SelectionMode._
import org.openmole.ide.core.implementation.dialog.DialogFactory
import org.openmole.ide.core.implementation.dialog.GUIPanel
import org.openmole.ide.core.implementation.execution.Settings
import org.openmole.ide.core.implementation.serializer.GUISerializer
import scala.swing.FileChooser.Result._
import scala.swing.Label

object SaveXML {
  def save(frame: GUIPanel): Unit = SaveXML.save(frame, Settings.currentProject.getOrElse(SaveXML.show.getOrElse("")))

  def save(frame: GUIPanel,
           title: String): Unit = {
    if (title != "") {
      Settings.currentProject = Some(title)
      frame.title = "OpenMOLE - " + title
      (new GUISerializer).serialize(title)
    } else Settings.currentProject = None
  }

  def show: Option[String] = {
    val fc = DialogFactory.fileChooser("Save OpenMOLE project",
      "*.om",
      "om")

    var text: Option[String] = None
    if (fc.showDialog(new Label, "OK") == Approve) {
      text = Some(fc.selectedFile.getPath)
      if (new File(text.get).getParentFile.isDirectory) text = Some(text.get.split('.')(0) + ".om")
    }
    text
  }
}
