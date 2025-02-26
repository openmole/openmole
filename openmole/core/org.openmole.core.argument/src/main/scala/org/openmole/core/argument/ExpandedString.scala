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

package org.openmole.core.argument

import java.io.InputStream

import org.openmole.core.context.{ Context, Val }
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.fileservice.FileService
import org.openmole.core.workspace.TmpDirectory
import org.openmole.tool.stream.{ StringInputStream, StringOutputStream }
import org.openmole.tool.random._

import scala.collection.mutable.ListBuffer
import scala.util.Try

/**
 * Methods to parse code strings
 */
object ExpandedString {

  implicit def fromStringToVariableExpansion(s: String): FromContext[String] = ExpandedString(s)
  implicit def fromTraversableOfStringToTraversableOfVariableExpansion(t: Iterable[String]): Iterable[FromContext[String]] = t.map(ExpandedString(_))
  implicit def fromFileToExpandedString(f: java.io.File): FromContext[String] = ExpandedString(f.getPath)

  def apply(s: String): FromContext[String] = apply(new StringInputStream(s))

  //  def expandValues(s: String, context: Context) =
  //    parse(new StringInputStream(s)).map {
  //      case UnexpandedElement(s) => s
  //      case ValueElement(v)      => v
  //      case CodeElement(code) =>
  //        context.variable[Any](code) match {
  //          case Some(v) => v.value
  //          case None    => throw new UserBadDataError(s"'$code' is not a value, cannot expands string '$s'")
  //        }
  //    }.mkString

  /**
   * Expand an input stream as an [[FromContext]]
   * @param is
   * @return
   */
  def apply(is: InputStream): FromContext[String] = {
    val expandedFC = parse(is).map(ExpansionElement.fromContext)

    FromContext { p =>
      import p._
      expandedFC.map(_.from(context)).mkString
    } withValidate { expandedFC.map(_.validate) }
  }

  def parse(is: InputStream) = {
    val expandedElements = ListBuffer[ExpansionElement]()

    val it = Iterator.continually(is.read).takeWhile(_ != -1)

    def nextToExpand(it: Iterator[Int]) = {
      var opened = 1
      val res = new StringBuffer
      while (it.hasNext && opened > 0) {
        val c = it.next
        c match {
          case '{' =>
            res.append(c.toChar); opened += 1
          case '}' =>
            opened -= 1; if (opened > 0) res.append(c.toChar)
          case _ => res.append(c.toChar)
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
        case '{' =>
          if (dollar) {
            expandedElements += UnexpandedElement(os.clear())
            val toExpand = nextToExpand(it)
            expandedElements += ExpansionElement(toExpand)
          }
          else os.write(c)
          dollar = false
        case '$' =>
          if (dollar) os.write('$')
          dollar = true
        case _ =>
          if (dollar) os.write('$')
          os.write(c)
          dollar = false
      }
    }
    if (dollar) os.write('$')

    expandedElements += UnexpandedElement(os.clear())
    expandedElements.toList
  }

  /**
   * An ExpandedElement distinguishes between value strings and code strings
   */
  object ExpansionElement {
    def apply(code: String): ExpansionElement =
      if (isValue(code)) ValueElement(code)
      else CodeElement(code)

    def isValue(code: String) =
      code.isEmpty || {
        Try(code.toDouble).toOption orElse
          Try(code.toLong).toOption orElse
          Try(code.toLowerCase.toBoolean).toOption match {
            case Some(v) => true
            case None    => false
          }
      }

    def fromContext(expansionElement: ExpansionElement) =
      expansionElement match {
        case e: UnexpandedElement => FromContext(_ => e.string)
        case e: ValueElement      => FromContext(_ => e.v)
        case e: CodeElement =>
          FromContext { p =>
            import p._
            context.variable(e.code) match {
              case Some(value) => value.value.toString
              case None        => e.proxy().from(context).toString
            }
          } withValidate {
            Validate { p =>
              import p._
              if (p.inputs.exists(_.name == e.code)) Seq()
              else e.proxy.validate(inputs)
            }
          }

      }
  }

  sealed trait ExpansionElement

  /**
   * An [[ExpansionElement]] which has not been expanded yet (a String)
   *
   * @param string
   */
  case class UnexpandedElement(string: String) extends ExpansionElement

  /**
   * A value element
   * @param v
   */
  case class ValueElement(v: String) extends ExpansionElement

  /**
   * A code element - the code is compiled only at each deserialization (@transient lazy val pattern for the proxy dynamically compiling here)
   * @param code
   */
  case class CodeElement(code: String) extends ExpansionElement {
    @transient lazy val proxy = ScalaCompilation.dynamic[Any](code)
  }

}

