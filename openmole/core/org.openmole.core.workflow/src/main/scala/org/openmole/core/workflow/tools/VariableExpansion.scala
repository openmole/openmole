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
import org.openmole.core.tools.script.{ GroovyProxyPool, GroovyProxy }
import org.openmole.core.workflow.data.{ Context, Variable }
import org.openmole.tool.stream.{ StringBuilderOutputStream, StringInputStream }
import org.openmole.core.workflow.data._

import scala.collection.mutable.ListBuffer
import scala.util.Try

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
    val os = new StringBuilderOutputStream()

    while (it.hasNext) {
      val c = it.next
      c match {
        case '{' ⇒
          if (dollar) {
            expandedElements += UnexpandedElement(os.builder.toString())
            os.builder.clear()
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
    expandedElements += UnexpandedElement(os.builder.toString())
    Expansion(expandedElements)
  }

  case class Expansion(elements: Seq[ExpansionElement]) {
    def expand(context: Context) = elements.map(_.expand(context)).mkString
  }

  trait ExpansionElement {
    def expand(context: Context): String
  }

  case class UnexpandedElement(string: String) extends ExpansionElement {
    def expand(context: Context): String = string
  }
  case class ExpandedElement(code: String) extends ExpansionElement {
    @transient lazy val proxy = GroovyProxyPool(code)
    def expand(context: Context): String =
      if (code.isEmpty) code
      else context.variable(code).map((_: Variable[Any]).value) orElse
        Try(code.toDouble).toOption orElse
        Try(code.toLong).toOption orElse
        Try(code.toLowerCase.toBoolean).toOption match {
          case Some(value) ⇒ value.toString
          case None        ⇒ proxy(context.toBinding).toString
        }
  }

}
