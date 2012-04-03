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

import javax.swing.JComponent
import javax.swing.JPanel
import scala.swing._

class MyPanel extends Panel{
  override def enabled_=(b : Boolean) : Unit = 
    contents.foreach{ c => c match {
        case x : MyPanel => 
          println("mypanel " + x.toString)
          x.enabled = b
        case null =>
        case _ => c.peer match {
            case x : JPanel => 
          println("jpanel " + x.toString)
              c.enabled = b
             // x.getComponents.foreach{_.setEnabled(b)}
            case x : JComponent => 
          println("jcomponent " + x.toString)
              x.setEnabled(b)
            case _ => 
          }
      }
    }
}