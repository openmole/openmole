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
import org.openmole.core.workflow.tools.{ FromContext, ExpandedString }
import org.openmole.plugin.method.evolution.Scalar
import org.openmole.plugin.task.scala._

import scala.collection.BitSet
import scala.collection.mutable.ListBuffer
import scala.util.Success

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
    new ModelFamily with super[ScalaBuilder].Built {
      override def source = builder.source
      override def attributes = _attributes.toList
      override def combination = builder.combination
      override def modelIdPrototype = _modelIdPrototype
      override def objectives = _objectives.toList
    }
}

object Combination {
  def empty[T] = new Combination[T] {
    def combinations = Seq.empty
    def components = Seq.empty
  }
}

sealed trait Combination[T] {
  def combinations: Seq[Seq[T]]
  def components: Seq[T]
}

case class AnyOf[T](components: T*) extends Combination[T] {
  def combinations = (0 to components.size).flatMap(components.combinations)
}

case class OneOf[T](components: T*) extends Combination[T] {
  def combinations = components.map(t ⇒ Seq(t))
}

case class AllToAll[T](cs: Combination[T]*) extends Combination[T] {
  def components = cs.flatMap(_.components).distinct

  def combinations =
    cs.map(_.combinations).reduceOption((ts1, ts2) ⇒ combine(ts1, ts2)).getOrElse(Seq.empty)

  def combine[A](it1: Seq[Seq[A]], it2: Seq[Seq[A]]): Seq[Seq[A]] =
    for (v1 ← it1; v2 ← it2) yield v1 ++ v2
}

trait ModelFamily <: Plugins { family ⇒

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
  def attributesStrings =
    attributes.map {
      case Attribute(p, name, _, _) ⇒ s"def ${p.name}: ${p.`type`} = ${ScalaCompilation.inputObject}.${name}"
    }

  def traitsString = traitsCombinations.map { ts ⇒ ts.map(t ⇒ s"with ${t.getName}").mkString(" ") }

  def codes = {
    traitsString.map {
      ts ⇒
        val context =
          Context(
            Variable(traitsVariable, ts),
            Variable(attributesVariable, attributesStrings.map("  " + _).mkString("\n"))
          )
        source.from(context)
    }
  }

  @transient lazy val compilation = {
    val compilation =
      new ScalaCompilation {

        def codeMatch =
          codes.zipWithIndex.map {
            case (code, i) ⇒
              s"""
                 |  case $i =>
                 |    $code
                 |    $outputMap
              """.stripMargin
          }.mkString("\n")

        def code =
          s"""
            |${modelIdPrototype.name} match {
            |  $codeMatch
            |  case x => throw new org.openmole.misc.exception.InternalProcessingError("Model id " + x + " too large")
            |}
          """.stripMargin

        override def wrapOutput = false

        override def imports: Seq[String] = family.imports
        override def source: String = code
        override def outputs: DataSet = objectives
        override def libraries: Seq[File] = family.libraries
        override def usedClasses: Seq[Class[_]] = family.usedClasses
      }
    compilation.compiled(Seq(modelIdPrototype) ++ attributes.map(_.prototype)).get
  }

}
