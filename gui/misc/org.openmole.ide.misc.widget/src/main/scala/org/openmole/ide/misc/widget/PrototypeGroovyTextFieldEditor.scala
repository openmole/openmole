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
import org.openmole.ide.misc.tools.image.Images._
import org.openmole.ide.misc.widget.dialog.DialogFactory
import scala.swing.Action
import org.openmole.core.model.data._
import org.openmole.ide.misc.tools.check.TypeCheck

class PrototypeGroovyTextFieldEditor(val title: String,
                                     prototype: Prototype[_],
                                     var editorText: String = "") extends LinkLabel("", new Action("") { def apply = {} }) {
  setIcon(editorText)
  cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
  action = new Action("") {
    def apply = {
      editorText = DialogFactory.groovyEditor(title, editorText)
      setIcon(editorText)
    }
  }

  private def setIcon(code: String) = {
    icon = code.isEmpty match {
      case true ⇒ EDIT_EMPTY
      case false ⇒
        val (msg, obj) = TypeCheck.apply(code, prototype)
        tooltip = msg
        obj match {
          case Some(x) ⇒ EDIT
          case None ⇒ EDIT_ERROR
        }
    }
    repaint
    revalidate
  }
}