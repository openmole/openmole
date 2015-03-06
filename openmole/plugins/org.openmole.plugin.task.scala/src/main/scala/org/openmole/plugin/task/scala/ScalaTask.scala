/*
 * Copyright (C) 2010 Romain Reuillon
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
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.task.scala

import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._
import org.openmole.core.tools.script._
import org.openmole.plugin.task.jvm._

object ScalaTask {

  def apply(source: String)(implicit plugins: PluginSet = PluginSet.empty) =
    new ScalaTaskBuilder {
      def toTask = new ScalaTask(source) with Built
    }
}

abstract class ScalaTask(val source: String) extends JVMLanguageTask with ScalaWrappedCompilation {
  override def processCode(context: Context) = compiled(context).get.run(context)
}

