/*
 * Copyright (C) 2011 Mathieu leclaire <mathieu.leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.plugin.task.groovy

import java.awt.Color
import org.openmole.ide.core.properties.ITaskFactoryUI
import org.openmole.ide.core.properties.PanelUI
import org.openmole.plugin.task.groovy.GroovyTask

class GroovyTaskFactoryUI extends ITaskFactoryUI {
  
  override def panel= new GroovyTaskPanelUI
  
  override def coreObject(p: PanelUI)= new GroovyTask("groovyTAsk")
  
  override def coreClass= classOf[GroovyTask]
  
  override def borderColor = new Color(61,104,130)
  
  override def backgroundColor = new Color(61,104,130,128)
}
