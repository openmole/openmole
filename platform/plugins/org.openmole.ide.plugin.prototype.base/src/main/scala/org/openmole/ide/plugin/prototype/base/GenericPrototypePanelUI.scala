/*
 * Copyright (C) 2011 <mathieu.leclaire at openmole.org>
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

package org.openmole.ide.plugin.prototype.base


import org.openmole.ide.core.model.panel.IPrototypePanelUI
import org.openmole.ide.misc.widget.MigPanel
import scala.swing.CheckBox
import scala.swing.Label
import scala.swing.TextField
import scala.swing.event.ButtonClicked

abstract class GenericPrototypePanelUI[T](d: Int) extends MigPanel("") with IPrototypePanelUI[T]{
  val dimTextField= new TextField(if(d>0) d.toString else "",2)
  val cb = new CheckBox("Array"){reactions+= {case ButtonClicked(cb) =>
        dimTextField.enabled = selected}
  }
    
  contents+= cb
  contents += dimTextField
  if (d<=0) cb.selected = false
 
  def dim= dimTextField.text 
}
