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

import org.openmole.core.serializer.plugin.Plugins
import org.openmole.core.workflow.builder.Builder
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task.PluginSet
import org.openmole.plugin.task.scala._

import scala.collection.mutable.ListBuffer
import scala.util.Success

object ModelFamily {

  def apply(model: Class[_])(implicit plugins: PluginSet) = new ModelFamilyBuilder(model)

}

object ModelFamilyBuilder {
  implicit def modelFamilyBuilderToModelFamily(builder: ModelFamilyBuilder) = builder.toModelFamily
}

class ModelFamilyBuilder(val model: Class[_])(implicit val plugins: PluginSet) extends Builder { builder ⇒
  private val _attributes = ListBuffer[(Prototype[_], String)]()
  def addAttribute(prototype: Prototype[_], name: String) = _attributes += prototype -> name
  def addAttribute(prototypes: Prototype[_]*) = _attributes ++= prototypes.map(p ⇒ p -> p.name)

  private var _modelPrototype = Prototype[AnyRef]("model")
  def setModelPrototype(prototype: Prototype[AnyRef]) = _modelPrototype = prototype

  private val _libraries = ListBuffer[File]()
  def addLibrary(library: File*) = _libraries ++= library

  private val _traits = ListBuffer[Class[_]]()
  def addTrait(t: Class[_]*) = _traits ++= t

  def toModelFamily = new ModelFamily {
    override def model: Class[_] = builder.model
    override def attributes: Seq[(Prototype[_], String)] = _attributes.toList
    override def libraries: Seq[File] = _libraries
    override def traits: Seq[Class[_]] = _traits
    override def modelPrototype: Prototype[AnyRef] = _modelPrototype
    override def plugins = builder.plugins
  }
}

trait ModelFamily <: Plugins { family ⇒
  def model: Class[_]
  def traits: Seq[Class[_]]
  def attributes: Seq[(Prototype[_], String)]
  def libraries: Seq[File]
  def modelPrototype: Prototype[AnyRef]

  def traitsCombinations: Seq[Seq[Class[_]]] = (0 to traits.size).flatMap(traits.combinations)
  def attributesString =
    attributes.map {
      case (p, name) ⇒ s"def ${p.name}: ${p.`type`} = ${ScalaCompilation.inputObject}.${name}"
    }

  def codes = {
    traitsCombinations.map {
      ts ⇒
        def traitsString = ts.map(t ⇒ s"with ${t.getName}").mkString(" ")

        s"""
           |lazy val ${modelPrototype.name} = new ${model.getName} $traitsString {
           |${attributesString.map("  " + _).mkString("\n")}
           |}
         """.stripMargin
    }
  }

  @transient lazy val compilations =
    codes.map {
      code ⇒
        val compilation =
          new ScalaCompilation {
            override def imports: Seq[String] = Seq.empty
            override def source: String = code
            override def outputs: DataSet = DataSet(modelPrototype)
            override def libraries: Seq[File] = family.libraries
            override def usedClasses: Seq[Class[_]] = traits ++ Seq(model)
          }
        compilation.compiled(attributes.map(_._1))
    }

  def workingModels = compilations.collect { case Success(m) ⇒ m }

}
