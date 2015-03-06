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
import org.openmole.core.workflow.data.{ Variable, DataSet, Context }
import org.openmole.core.workflow.task._
import org.openmole.plugin.task.jvm.JVMLanguageTask
import org.openmole.plugin.task.scala._

object ModelFamilyTask {

  def apply(modelFamily: ModelFamily) = new TaskBuilder {
    modelFamily.family.compiled
    modelFamily.attributes.foreach { a ⇒ addInput(a.prototype) }
    addInput(modelFamily.modelIdPrototype)
    modelFamily.objectives.foreach { addOutput(_) }
    override def toTask: Task = new ModelFamilyTask(modelFamily) with Built
  }

}

abstract class ModelFamilyTask(val modelFamily: ModelFamily) extends Task { t ⇒
  override def process(context: Context): Context =
    context ++ modelFamily.run(context)
}
