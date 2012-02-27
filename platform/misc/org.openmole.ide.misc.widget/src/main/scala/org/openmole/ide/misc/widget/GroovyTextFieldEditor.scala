/*
 * Copyright (C) 2012 mathieu
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

package org.openmole.ide.misc.widget

import java.awt.Cursor
import javax.swing.ImageIcon
import org.openmole.ide.misc.image.ImageTool
import org.openmole.ide.misc.widget.dialog.DialogFactory
import scala.swing.Action

class GroovyTextFieldEditor(val title : String,
                            var editorText : String = "") extends LinkLabel("",new Action("") { def apply = {}}){                                              
  setIcon
  cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
  action = new Action(""){
    def apply = {
      editorText = DialogFactory.groovyEditor(title,editorText)
      setIcon
    }
  }
  
  private def setIcon = {
    editorText.isEmpty match {
      case true => icon = new ImageIcon(ImageTool.loadImage("img/edit_empty.png",20,20))
      case false => icon = new ImageIcon(ImageTool.loadImage("img/edit.png",20,20))
    }
    repaint
    revalidate
  }
}