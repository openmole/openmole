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

package org.openmole.ide.misc.widget.dialog

import java.awt.Dimension
import org.openmole.ide.misc.widget.GroovyEditor
import org.openide.DialogDescriptor
import org.openide.DialogDisplayer
import org.openide.NotifyDescriptor

object DialogFactory {

  def groovyEditor(title: String,
                   content: String): String = {
    val editor = new GroovyEditor
    editor.preferredSize = new Dimension(150, 150)
    editor.editor.text = content
    if (DialogDisplayer.getDefault.notify(new DialogDescriptor(editor.peer, title)).equals(NotifyDescriptor.OK_OPTION))
      editor.editor.text
    else content
  }
}
