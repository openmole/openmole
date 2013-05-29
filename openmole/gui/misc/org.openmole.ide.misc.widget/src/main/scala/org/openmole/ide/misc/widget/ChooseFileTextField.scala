/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.ide.misc.widget

import java.awt.Dimension
import scala.swing.FileChooser.SelectionMode._
import javax.swing.filechooser.FileNameExtensionFilter
import scala.swing.FileChooser
import scala.swing.FileChooser.Result.Approve
import scala.swing.TextField
import scala.swing.event.MousePressed
import scala.collection.JavaConversions._

class ChooseFileTextField(initialText: String, chooserTitle: String, chooserDescription: Option[String], selectionMode: Value, extensions: Option[String]) extends TextField {
  def this(iT: String, cT: String, cD: String, ex: String) = this(iT, cT, Some(cD), FilesOnly, Some(ex))
  def this(iT: String, cT: String) = this(iT, cT, None, FilesOnly, None)
  def this(iT: String) = this(iT, "Select a directory", None, DirectoriesOnly, None)

  text = initialText
  val fc = new FileChooser {
    if (chooserDescription.isDefined) fileFilter = new FileNameExtensionFilter(chooserDescription.get, extensions.get)
    fileSelectionMode = selectionMode
    title = chooserTitle
  }

  reactions += {
    case e: MousePressed ⇒
      if (e.clicks == 2) {
        if (fc.showDialog(this, "OK") == Approve) text = fc.selectedFile.getPath
        publish(new DialogClosedEvent(this))
      }
  }
  columns = 15
  listenTo(this.mouse.clicks)
}
