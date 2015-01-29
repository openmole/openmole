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

  case class Attribute(prototype: Prototype[_], name: String, min: Double, max: Double)

  def apply(model: Class[_])(implicit plugins: PluginSet) = new ModelFamilyBuilder(model)

}

import ModelFamily._

object ModelFamilyBuilder {
  implicit def modelFamilyBuilderToModelFamily(builder: ModelFamilyBuilder) = builder.toModelFamily
}

class ModelFamilyBuilder(val model: Class[_])(implicit val plugins: PluginSet) extends Builder { builder ⇒
  private val _attributes = ListBuffer[Attribute]()

  def addAttribute(prototype: Prototype[_], name: String, min: Double, max: Double) = {
    _attributes += Attribute(prototype, name, min, max)
    this
  }

  def addAttribute(prototype: Prototype[_], min: Double, max: Double) = {
    _attributes += Attribute(prototype, prototype.name, min, max)
    this
  }

  private var _modelPrototype = Prototype[AnyRef]("model")
  def setModelPrototype(prototype: Prototype[AnyRef]) = {
    _modelPrototype = prototype
    this
  }

  private val _libraries = ListBuffer[File]()
  def addLibrary(library: File*) = {
    _libraries ++= library
    this
  }

  private val _traits = ListBuffer[Class[_]]()
  def addTrait(t: Class[_]*) = {
    _traits ++= t
    this
  }

  private var _modelIdPrototype = Prototype[Int]("modelId")
  def setModelIdPrototype(prototype: Prototype[Int]) = {
    _modelIdPrototype = prototype
    this
  }

  def toModelFamily = new ModelFamily {
    override def model = builder.model
    override def attributes = _attributes.toList
    override def libraries = _libraries
    override def traits = _traits
    override def modelPrototype = _modelPrototype
    override def plugins = builder.plugins
    override def modelIdPrototype = _modelIdPrototype
  }
}

trait ModelFamily <: Plugins { family ⇒
  def model: Class[_]
  def traits: Seq[Class[_]]
  def attributes: Seq[Attribute]
  def libraries: Seq[File]
  def modelPrototype: Prototype[AnyRef]
  def modelIdPrototype: Prototype[Int]

  def traitsCombinations: Seq[Seq[Class[_]]] = (0 to traits.size).flatMap(traits.combinations)
  def attributesString =
    attributes.map {
      case Attribute(p, name, _, _) ⇒ s"def ${p.name}: ${p.`type`} = ${ScalaCompilation.inputObject}.${name}"
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
        compilation.compiled(attributes.map(_.prototype))
    }

  def workingModels = compilations.collect { case Success(m) ⇒ m }

}
