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

package org.openmole.core.implementation.tools

import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.InputStream
import org.openmole.misc.tools.script.GroovyProxy
import org.openmole.core.model.data.Context
import org.openmole.core.model.data.Variable
import scala.math.BigDecimal
import util.Try
import org.openmole.misc.tools.io.{ StringBuilderOutputStream, StringInputStream }
import org.openmole.misc.exception._

object VariableExpansion {

  def expandBigDecimal(context: Context, s: String): BigDecimal =
    BigDecimal(apply(context, s))

  def expandDouble(context: Context, s: String): Double =
    apply(context, s).toDouble

  def expandInt(context: Context, s: String): Int =
    apply(context, s).toInt

  def apply(context: Context, s: String): String =
    apply(context, Iterable.empty, s)

  def apply(context: Context, tmpVariable: Iterable[Variable[_]], s: String): String =
    expandDataInternal(context, tmpVariable, s)

  private def expandDataInternal(context: Context, tmpVariable: Iterable[Variable[_]], s: String): String = {
    val os = new StringBuilder()
    expandBufferData(context ++ tmpVariable, new StringInputStream(s), new StringBuilderOutputStream(os))
    os.toString
  }

  private def getVarName(str: String): String = {
    str.substring(1, str.length - 1)
  }

  protected def expandOneData(allVariables: Context, variableExpression: String): String =
    if (variableExpression.isEmpty) variableExpression
    else allVariables.variable(variableExpression).map((_: Variable[Any]).value) orElse
      Try(variableExpression.toDouble).toOption orElse
      Try(variableExpression.toLong).toOption orElse
      Try(variableExpression.toLowerCase.toBoolean).toOption match {
        case Some(value) ⇒ value.toString
        case None ⇒
          val shell = new GroovyProxy(variableExpression, Iterable.empty) with GroovyContextAdapter
          shell.execute(allVariables).toString
      }

  def expandBufferData(context: Context, is: InputStream, os: OutputStream) = {
    //val isreader = new InputStreamReader(is, "UTF-8")
    val oswriter = new OutputStreamWriter(os)

    try {
      val it = Iterator.continually(is.read).takeWhile(_ != -1)

      def nextToExpand(it: Iterator[Int]) = {
        var opened = 1
        val res = new StringBuffer
        while (it.hasNext && opened > 0) {
          val c = it.next
          c match {
            case '{' ⇒ res.append(c.toChar); opened += 1
            case '}' ⇒ opened -= 1; if (opened > 0) res.append(c.toChar)
            case _   ⇒ res.append(c.toChar)
          }
        }
        if (opened != 0) throw new UserBadDataError("Malformed ${expr} expression, unmatched opened {")
        res.toString
      }

      var dollar = false

      while (it.hasNext) {
        val c = it.next
        c match {
          case '{' ⇒
            if (dollar) {
              val toExpand = nextToExpand(it)
              oswriter.write(expandOneData(context, toExpand))
            }
            else oswriter.write(c)
            dollar = false
          case '$' ⇒
            if (dollar) oswriter.write('$')
            dollar = true
          case _ ⇒
            if (dollar) oswriter.write('$')
            oswriter.write(c)
            dollar = false
        }
      }
      if (dollar) oswriter.write('$')
    }
    finally oswriter.close
  }

  implicit def stringExpansionDecorator(s: String) = new {
    def expand(context: Context) = apply(context, s)
  }

}
