/*
 * Copyright (C) 2011 <mathieu.leclaire at openmole.org>
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
package org.openmole.ide.plugin.task.netlogo

import java.awt.Dimension
import javax.swing.filechooser.FileNameExtensionFilter
import scala.swing.BoxPanel
import scala.swing.FileChooser
import scala.swing.FileChooser.Result.Approve
import scala.swing.Orientation
import scala.swing.TextField
import scala.swing.event.FocusGained
import scala.swing.FileChooser.SelectionMode.Value

class FakeTextField(fc: FileChooser, initialText: String) extends TextField {
  def this(filter: FileNameExtensionFilter, chooserTitle: String,t: String) = this(new FileChooser {
      fileFilter = filter
      title = chooserTitle},t)
  def this(chooserTitle: String,t: String,sm: Value) = this(new FileChooser{
      title = chooserTitle
      fileSelectionMode = sm},t)
  
  maximumSize = new Dimension(150,30)
    reactions += {
      case FocusGained(peer,_,false) =>
        focusable = false
        if (fc.showDialog(this,"OK") == Approve) text = fc.selectedFile.getPath
        focusable = true
    }
    text = initialText
    columns = 15
  listenTo(this)
}
