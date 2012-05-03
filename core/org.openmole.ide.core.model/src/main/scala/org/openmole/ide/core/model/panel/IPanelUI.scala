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

package org.openmole.ide.core.model.panel

import javax.swing.JPanel

trait IPanelUI {
  def peer: JPanel

  //  def enabled(b : Boolean) : Unit = 
  //    peer.getComponents.foreach{ c => c match {
  //        case x : MyPanel => 
  //          println("IPA mypanel")
  //          x.enabled = b
  //        case x : JComponent => 
  //          println("IPA jcomponent")
  //          x.setEnabled(b)
  //        case _ => 
  //      }
  //    }
}