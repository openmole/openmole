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

package org.openmole.core.expansion

import java.io.InputStream

import org.openmole.core.context.{ Context, Val }
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.fileservice.FileService
import org.openmole.core.workspace.NewFile
import org.openmole.tool.stream.{ StringInputStream, StringOutputStream }
import org.openmole.tool.random._

import scala.collection.mutable.ListBuffer
import scala.util.Try

/**
 * Methods to parse code strings
 */
object ExpandedString {

  implicit def fromStringToVariableExpansion(s: String) = ExpandedString(s)
  implicit def fromTraversableOfStringToTraversableOfVariableExpansion[T <: Traversable[String]](t: T) = t.map(ExpandedString(_))
  implicit def fromFileToExpandedString(f: java.io.File) = ExpandedString(f.getPath)

  def apply(s: String): FromContext[String] = apply(new StringInputStream(s))

  def apply(is: InputStream): FromContext[String] = {
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
    Expansion(expandedElements)
  }

  case class Expansion(elements: Seq[ExpandedString.ExpansionElement]) extends FromContext[String] {
    def from(context: ⇒ Context)(implicit rng: RandomProvider, newFile: NewFile, fileService: FileService) = elements.map(_.from(context)).mkString
    def validate(inputs: Seq[Val[_]])(implicit newFile: NewFile, fileService: FileService): Seq[Throwable] = elements.flatMap(_.validate(inputs))
  }

  type ExpansionElement = FromContext[String]

  case class UnexpandedElement(string: String) extends ExpansionElement {
    def from(context: ⇒ Context)(implicit rng: RandomProvider, newFile: NewFile, fileService: FileService): String = string
    def validate(inputs: Seq[Val[_]])(implicit newFile: NewFile, fileService: FileService): Seq[Throwable] = Seq.empty
  }

  /**
   * An ExpandedElement distinguishes between value strings and code strings
   */
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
    def from(context: ⇒ Context)(implicit rng: RandomProvider, newFile: NewFile, fileService: FileService): String = v
    def validate(inputs: Seq[Val[_]])(implicit newFile: NewFile, fileService: FileService): Seq[Throwable] = Seq.empty
  }

  case class CodeElement(code: String) extends ExpansionElement {
    @transient lazy val proxy = ScalaCompilation.dynamic[Any](code)
    def from(context: ⇒ Context)(implicit rng: RandomProvider, newFile: NewFile, fileService: FileService): String = {
      context.variable(code) match {
        case Some(value) ⇒ value.value.toString
        case None        ⇒ proxy().from(context).toString
      }
    }
    def validate(inputs: Seq[Val[_]])(implicit newFile: NewFile, fileService: FileService): Seq[Throwable] =
      if (inputs.exists(_.name == code)) Seq.empty
      else proxy.validate(inputs).toSeq
  }

}

