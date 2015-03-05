/*
 * Copyright (C) 2014 Romain Reuillon
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

package org.openmole.plugin.task.scala

import org.openmole.core.workflow.task.PluginSet
import org.openmole.plugin.task.jvm.{ JVMLanguageBuilder, JVMLanguageTaskBuilder }

import scala.collection.mutable.ListBuffer

trait ScalaBuilder <: JVMLanguageBuilder { builder ⇒

  val usedClasses = ListBuffer[Class[_]]()

  def addClassUse(c: Class[_]*): this.type = {
    usedClasses ++= c
    this
  }

  addImport("org.openmole.plugin.task.jvm.JVMLanguageTask.newRNG")
  addImport("org.openmole.plugin.task.jvm.JVMLanguageTask.newFile")
  addImport("org.openmole.plugin.task.jvm.JVMLanguageTask.newDir")

  trait Built <: super.Built {
    def usedClasses = builder.usedClasses.toList
  }
}

abstract class ScalaTaskBuilder(implicit plugins: PluginSet) extends JVMLanguageTaskBuilder with ScalaBuilder { builder ⇒
  trait Built <: super[JVMLanguageTaskBuilder].Built with super[ScalaBuilder].Built
}