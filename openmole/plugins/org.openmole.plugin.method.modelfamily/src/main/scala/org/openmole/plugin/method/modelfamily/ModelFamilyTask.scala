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

import fr.iscpif.family.{ Combination, TypedValue, ModelFamily }
import org.openmole.core.workflow.builder.TaskBuilder
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._
import org.openmole.plugin.task.jvm.JVMLanguageTask
import org.openmole.plugin.task.scala._

import scala.util.Try
import fr.iscpif.family.{ ModelFamily ⇒ FModelFamily, Combination, TypedValue }

object ModelFamilyTask {

  def apply(modelFamily: ModelFamily) = new TaskBuilder {
    modelFamily.inputs.foreach { i ⇒ addInput(i) }
    modelFamily.attributes.foreach { a ⇒ addInput(a.prototype) }
    addInput(modelFamily.modelIdPrototype)

    modelFamily.objectives.foreach { o ⇒ addOutput(o) }
    modelFamily.outputs.foreach { o ⇒ addOutput(o) }
    def toTask = new ModelFamilyTask(modelFamily) with Built
  }
}

import ModelFamily._

abstract class ModelFamilyTask(val modelFamily: ModelFamily) extends Task { t ⇒

  implicit def prototypeToTypeValue[T](p: Prototype[T]) = new TypedValue(p.name, p.`type`.manifest)
  implicit def typedValueToPrototype[T](p: TypedValue) = Prototype(p.name)(PrototypeType(p.`type`))

  @transient lazy val family = new FModelFamily {
    def imports: Seq[String] = modelFamily.imports
    def inputs = modelFamily.inputs.toSeq.map(d ⇒ d: TypedValue)
    def outputs = modelFamily.outputs.toSeq.map(d ⇒ d: TypedValue)
    def attributes = modelFamily.attributes.map(d ⇒ d.prototype: TypedValue)
    def combination: Combination[Class[_]] = modelFamily.combination
    def compile(code: String): Try[Any] = compilation.compile(code)
    def compilation = new ScalaCompilation {
      def libraries: Seq[File] = modelFamily.libraries
      def usedClasses: Seq[Class[_]] = modelFamily.usedClasses
    }
    def source(traits: String, attributes: String) = {
      val context =
        Context(
          Variable(traitsVariable, traits),
          Variable(attributesVariable, attributes)
        )
      modelFamily.source.from(context)
    }

  }

  family.compiled.get

  def run(context: Context): Context = {
    lazy val rng = Task.buildRNG(context)
    val values = family.allInputs.map((a: TypedValue) ⇒ context(a.name))
    val modelId = context(modelFamily.modelIdPrototype)
    val map = family.run(modelId, values: _*)(rng).get
    family.outputs.map(o ⇒ Variable.unsecure(o, map(o.name)))
  }

  override def process(context: Context): Context =
    context + run(context)
}
