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
import org.openmole.ide.core.implementation.execution.{ ScenesManager, Settings }
import org.openmole.ide.core.implementation.serializer.{ MoleData, GUISerializer }
import scala.swing.FileChooser.Result._
import scala.swing.Label
import org.openmole.ide.core.implementation.dataproxy.Proxies

object SaveXML {

  def save(frame: GUIPanel, path: Option[File] = Settings.currentProject orElse SaveXML.show): Unit =
    path match {
      case Some(p) ⇒
        frame.title = "OpenMOLE - " + p.getCanonicalPath
        (new GUISerializer).serialize(p, Proxies.instance, ScenesManager.moleScenes.map(MoleData.fromScene))
        Settings.currentProject = path
      case None ⇒
    }

  def show: Option[File] = {
    val fc = DialogFactory.fileChooser("Save OpenMOLE project",
      "*.om",
      "om",
      Settings.currentPath)
    if (fc.showDialog(new Label, "OK") == Approve) {
      val f = new File(fc.selectedFile.getPath)
      if (f.getParentFile.isDirectory) {
        Settings.currentPath = Some(f.getParentFile)
        val saveAs =
          if (!f.getName.contains(".")) new File(fc.selectedFile.getPath + ".om")
          else f
        Some(saveAs)
      } else None
    } else None
  }
}
