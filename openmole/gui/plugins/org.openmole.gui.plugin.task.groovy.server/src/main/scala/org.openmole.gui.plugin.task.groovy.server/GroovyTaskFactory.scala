package org.openmole.gui.plugin.task.groovy.server

import org.openmole.gui.ext.data.Factory
import org.openmole.gui.plugin.task.groovy.ext.GroovyTaskData
import org.openmole.core.workflow.task.PluginSet
import org.openmole.plugin.task.groovy.GroovyTask
import scala.util.Try

/*
 * Copyright (C) 25/09/14 // mathieu.leclaire@openmole.org
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

class GroovyTaskFactory(val data: GroovyTaskData) extends Factory {

  def coreObject(implicit plugins: PluginSet): Try[Any] =
    Try { GroovyTask(data.code)(plugins) //set { _ setName data.name }
     }
}
