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
import org.openmole.core.workflow.task.{ Task, PluginSet }
import org.openmole.core.workflow.tools.{ FromContext, ExpandedString }
import org.openmole.plugin.method.evolution.Scalar
import org.openmole.plugin.task.scala._

import scala.collection.BitSet
import scala.collection.mutable.ListBuffer
import scala.util.{ Try, Success }
import fr.iscpif.family.{ ModelFamily ⇒ FModelFamily, Combination }

object ModelFamily {

  case class Attribute(prototype: Prototype[Double], name: String, min: FromContext[Double], max: FromContext[Double]) {
    def toInput = Scalar(prototype, min, max)
  }

  def apply(source: ExpandedString, combination: Combination[Class[_]])(implicit plugins: PluginSet) = new ModelFamilyBuilder(source, combination)

  def traitsVariable = Prototype[String]("traits")
  def attributesVariable = Prototype[String]("attributes")

}

import ModelFamily._

object ModelFamilyBuilder {
  implicit def modelFamilyBuilderToModelFamily(builder: ModelFamilyBuilder) = builder.toModelFamily
}

class ModelFamilyBuilder(val source: ExpandedString, val combination: Combination[Class[_]])(implicit val plugins: PluginSet) extends Builder with ScalaBuilder { builder ⇒

  addClassUse(combination.components: _*)

  private val _attributes = ListBuffer[Attribute]()

  def addAttribute(prototype: Prototype[Double], name: String, min: FromContext[Double], max: FromContext[Double]) = {
    _attributes += Attribute(prototype, name, min, max)
    this
  }

  def addAttribute(prototype: Prototype[Double], min: FromContext[Double], max: FromContext[Double]) = {
    _attributes += Attribute(prototype, prototype.name, min, max)
    this
  }

  private var _modelIdPrototype = Prototype[Int]("modelId")
  def setModelIdPrototype(prototype: Prototype[Int]) = {
    _modelIdPrototype = prototype
    this
  }

  private var _objectives = ListBuffer[Prototype[Double]]()
  def addObjective(prototype: Prototype[Double]*) = {
    _objectives ++= prototype
    this
  }

  def toModelFamily =
    new ModelFamily with super[ScalaBuilder].Built { mf ⇒
      def source = builder.source
      def attributes = _attributes.toList
      def combination = builder.combination
      def modelIdPrototype = _modelIdPrototype
      def objectives = _objectives.toList
    }
}

trait ModelFamily <: Plugins { f ⇒

  @transient lazy val family = new FModelFamily {
    def imports: Seq[String] = f.imports
    def outputs: Seq[String] = f.objectives.map(_.name)
    def attributes: Seq[String] = f.attributes.map(_.prototype.name)
    def combination: Combination[Class[_]] = f.combination
    def compile(code: String): Try[Any] = compilation.compile(code)
    def compilation = new ScalaCompilation {
      def libraries: Seq[File] = f.libraries
      def usedClasses: Seq[Class[_]] = f.usedClasses
    }
    def source(traits: String, attributes: String) = {
      val context =
        Context(
          Variable(traitsVariable, traits),
          Variable(attributesVariable, attributes)
        )
      f.source.from(context)
    }
  }

  def imports: Seq[String]
  def source: ExpandedString
  def traits: Seq[Class[_]] = combination.components
  def attributes: Seq[Attribute]
  def objectives: Seq[Prototype[Double]]
  def libraries: Seq[File]
  def modelIdPrototype: Prototype[Int]
  def usedClasses: Seq[Class[_]]
  def combination: Combination[Class[_]]
  def traitsCombinations = combination.combinations
  lazy val size = traitsCombinations.size

  def run(context: Context) = {
    implicit lazy val rng = Task.buildRNG(context)
    val attributeValues = attributes.map(a ⇒ context(a.prototype))
    val modelId = context(modelIdPrototype)
    val map = family.run(modelId, attributeValues: _*)(rng).get
    objectives.map(o ⇒ Variable.unsecure(o, map(o.name)))
  }

}
