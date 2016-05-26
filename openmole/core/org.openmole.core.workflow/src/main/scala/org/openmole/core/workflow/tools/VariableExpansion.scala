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

package org.openmole.core.workflow.tools

import java.io.{ InputStream, OutputStream, OutputStreamWriter }

import org.openmole.core.exception.UserBadDataError
import org.openmole.tool.stream.{ StringInputStream, StringOutputStream }
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.tools.VariableExpansion.Expansion

import scala.collection.mutable.ListBuffer
import scala.util.{ Failure, Success, Try }

object VariableExpansion {

  def apply(s: String): Expansion = apply(new StringInputStream(s))

  def apply(is: InputStream): Expansion = {
    val expandedElements = ListBuffer[ExpansionElement]()

    val it = Iterator.continually(is.read).takeWhile(_ != -1)

    def nextToExpand(it: Iterator[Int]) = {
      var opened = 1
      val res = new StringBuffer
      while (it.hasNext && opened > 0) {
        val c = it.next
        c match {
          case '{' ⇒
            res.append(c.toChar); opened += 1
          case '}' ⇒
            opened -= 1; if (opened > 0) res.append(c.toChar)
          case _ ⇒ res.append(c.toChar)
        }
      }
      if (opened != 0) throw new UserBadDataError("Malformed ${expr} expression, unmatched opened {")
      res.toString
    }

    var dollar = false
    val os = new StringOutputStream()

    while (it.hasNext) {
      val c = it.next
      c match {
        case '{' ⇒
          if (dollar) {
            expandedElements += UnexpandedElement(os.clear())
            val toExpand = nextToExpand(it)
            expandedElements += ExpandedElement(toExpand)
          }
          else os.write(c)
          dollar = false
        case '$' ⇒
          if (dollar) os.write('$')
          dollar = true
        case _ ⇒
          if (dollar) os.write('$')
          os.write(c)
          dollar = false
      }
    }
    if (dollar) os.write('$')
    expandedElements += UnexpandedElement(os.clear())
    ElementsExpansion(expandedElements)
  }

  trait Expansion extends FromContext[String] {
    def expand(context: ⇒ Context)(implicit rng: RandomProvider): String
    def validate(inputs: Seq[Prototype[_]]): Seq[Throwable]
    def from(context: ⇒ Context)(implicit rng: RandomProvider) = expand(context)
  }

  case class ElementsExpansion(elements: Seq[ExpansionElement]) extends Expansion {
    def expand(context: ⇒ Context)(implicit rng: RandomProvider) = elements.map(_.expand(context)).mkString
    def validate(inputs: Seq[Prototype[_]]): Seq[Throwable] = elements.flatMap(_.validate(inputs))
  }

  trait ExpansionElement {
    def expand(context: ⇒ Context)(implicit rng: RandomProvider): String
    def validate(inputs: Seq[Prototype[_]]): Seq[Throwable]
  }

  case class UnexpandedElement(string: String) extends ExpansionElement {
    def expand(context: ⇒ Context)(implicit rng: RandomProvider): String = string
    def validate(inputs: Seq[Prototype[_]]): Seq[Throwable] = Seq.empty
  }

  object ExpandedElement {
    def apply(code: String): ExpansionElement = {
      if (code.isEmpty) ValueElement(code)
      else
        Try(code.toDouble).toOption orElse
          Try(code.toLong).toOption orElse
          Try(code.toLowerCase.toBoolean).toOption match {
            case Some(v) ⇒ ValueElement(code)
            case None    ⇒ CodeElement(code)
          }
    }
  }

  case class ValueElement(v: String) extends ExpansionElement {
    def expand(context: ⇒ Context)(implicit rng: RandomProvider): String = v
    def validate(inputs: Seq[Prototype[_]]): Seq[Throwable] = Seq.empty
  }

  case class CodeElement(code: String) extends ExpansionElement {
    @transient lazy val proxy = ScalaWrappedCompilation.dynamic[Any](code)
    def expand(context: ⇒ Context)(implicit rng: RandomProvider): String = {
      context.variable(code) match {
        case Some(value) ⇒ value.value.toString
        case None        ⇒ proxy().from(context).toString
      }
    }
    def validate(inputs: Seq[Prototype[_]]): Seq[Throwable] = {
      implicit def m = manifest[Any]
      if (inputs.exists(_.name == code)) Seq.empty
      else
        Try[Any](ScalaWrappedCompilation.static[Any](code, inputs)) match {
          case Success(_) ⇒ Seq.empty
          case Failure(t) ⇒ Seq(t)
        }
    }
  }

}

object ExpandedString {

  implicit def fromStringToExpandedString(s: String) = ExpandedString(s)
  implicit def fromStringToExpandedStringOption(s: String) = Some[ExpandedString](s)
  implicit def fromTraversableOfStringToTraversableOfExpandedString[T <: Traversable[String]](t: T) = t.map(ExpandedString(_))
  implicit def fromFileToExpandedString(f: java.io.File) = ExpandedString(f.getPath)

  def apply(s: String) =
    new ExpandedString {
      override def string = s
    }
}

trait ExpandedString <: Expansion {
  @transient lazy val expansion = VariableExpansion(string)
  def +(s: ExpandedString): ExpandedString = string + s.string
  def string: String
  def expand(context: ⇒ Context)(implicit rng: RandomProvider) = expansion.expand(context)
  def validate(inputs: Seq[Prototype[_]]) = expansion.validate(inputs)
}
