/*
 * Copyright (C) 2015 Romain Reuillon
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.method.modelfamily

import java.io.File

import org.openmole.core.workflow.builder.TaskBuilder
import org.openmole.core.workflow.data.{ DataSet, Context }
import org.openmole.core.workflow.task._
import org.openmole.plugin.task.code.JVMLanguageTask
import org.openmole.plugin.task.scala._

import scala.tools.ant.ScalaTask

object ModelFamilyTask {

  def apply(modelFamily: ModelFamily)(implicit plugins: PluginSet = PluginSet.empty) = new ScalaTaskBuilder {
    modelFamily.attributes.map { a ⇒ addInput(a.prototype) }
    override def toTask: Task = new ModelFamilyTask(modelFamily) with Built
  }

}

abstract class ModelFamilyTask(val modelFamily: ModelFamily) extends JVMLanguageTask { t ⇒

  override def processCode(context: Context): Context = {
    modelFamily.compilations.foreach(println)
    println(modelFamily.workingModels.size)
    context
  }

}
