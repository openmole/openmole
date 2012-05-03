/*
 * Copyright (C) 2011 mathieu
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

package org.openmole.ide.core.implementation.execution

import java.awt.Rectangle
import java.io.OutputStream
import scala.swing.TextArea

object TextAreaOutputStream {

  implicit def textAreadDecorator(textArea: TextArea) = new {

    def toStream = new TextAreaOutputStream(textArea)

  }
}

class TextAreaOutputStream(textArea: TextArea) extends OutputStream {
  override def flush = textArea.repaint

  override def write(b: Int) = textArea.append(new String(Array[Byte](b.asInstanceOf[Byte])))

  override def write(b: Array[Byte], off: Int, len: Int) = {
    textArea.append(new String(b, off, len))
    textArea.peer.scrollRectToVisible(new Rectangle(0, textArea.size.height - 2, 1, 1))
  }
}
