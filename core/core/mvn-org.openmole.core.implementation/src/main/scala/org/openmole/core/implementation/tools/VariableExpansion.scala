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

import com.ibm.icu.text.UTF16
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.InputStream
import org.openmole.misc.tools.script.GroovyProxy
import org.openmole.core.model.data.Context
import org.openmole.core.model.data.Variable
import scala.math.BigDecimal
import scala.util.control.Breaks._
import util.Try

object VariableExpansion {

  private val patternBegin = '{'
  private val patternEnd = '}'
  private val eval = "$" + patternBegin

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
    var ret = s
    val allVariables = context ++ tmpVariable
    var cur = 0

    breakable {
      do {
        val beginIndex = ret.indexOf(eval)
        if (beginIndex == -1) break
        var cur = beginIndex + 2
        var curLevel = 0

        breakable {
          while (cur < ret.length) {
            val curChar = ret.charAt(cur)

            if (curChar == patternEnd) {
              if (curLevel == 0) break
              curLevel -= 1
            } else if (curChar == patternBegin) {
              curLevel += 1
            }
            cur += 1
          }
        }

        if (cur < ret.length) {
          val toInsert = expandOneData(allVariables, getVarName(ret.substring(beginIndex + 1, cur + 1)))
          ret = ret.substring(0, beginIndex) + toInsert + ret.substring(cur + 1)
        } else break
      } while (true)
    }

    ret
  }

  def expandData(replace: Map[String, String], tmpVariable: Iterable[Variable[_]], s: String): String = {
    expandDataInternal(replace, tmpVariable, s)
  }

  def expandDataInternal(replace: Map[String, String], tmpVariable: Iterable[Variable[_]], s: String): String = {
    var ret = s
    var beginIndex = -1
    var endIndex = -1

    breakable {
      do {
        beginIndex = ret.indexOf(patternBegin)
        endIndex = ret.indexOf(patternEnd)

        if (beginIndex == -1 || endIndex == -1) break

        val toReplace = ret.substring(beginIndex, endIndex + 1)

        val varName = getVarName(toReplace);

        ret = replace.get(varName) match {
          case Some(toInsert) ⇒ ret.substring(0, beginIndex) + toInsert + ret.substring(endIndex + 1)
          case None ⇒ ret.substring(0, beginIndex) + ret.substring(endIndex + 1)
        }
      } while (true)
    }

    ret
  }

  private def getVarName(str: String): String = {
    str.substring(1, str.length - 1)
  }

  protected def expandOneData(allVariables: Context, variableExpression: String): String = {
    allVariables.variable(variableExpression).map((_: Variable[Any]).value) orElse
      Try(variableExpression.toDouble).toOption orElse
      Try(variableExpression.toLong).toOption orElse
      Try(variableExpression.toLowerCase.toBoolean).toOption match {
        case Some(value) ⇒ value.toString
        case None ⇒
          val shell = new GroovyProxy(variableExpression, Iterable.empty) with GroovyContextAdapter
          shell.execute(allVariables).toString
      }
  }

  def expandBufferData(context: Context, is: InputStream, os: OutputStream) = {
    val isreader = new InputStreamReader(is, "UTF-8")
    val oswriter = new OutputStreamWriter(os)

    var openbrace = 0
    var closebrace = 0
    var expandTime = false
    var esbuilder = new StringBuffer(UTF16.valueOf('$')).append(UTF16.valueOf('{'))

    def appendChar(c: Int) = esbuilder.append(UTF16.valueOf(c))

    try {
      Iterator.continually(isreader.read).takeWhile(_ != -1).foreach {
        n ⇒
          n match {
            case '{' ⇒
              openbrace += 1
              expandTime = true
              if (openbrace > 1) appendChar(n)
              else esbuilder = new StringBuffer(UTF16.valueOf('$')).append(UTF16.valueOf('{'))
            case '}' ⇒
              closebrace += 1
              appendChar(n)
              if (openbrace == closebrace) {
                oswriter.write(apply(context, esbuilder.toString))
                expandTime = false
                openbrace = 0
                closebrace = 0
              }
            case _ ⇒
              if (expandTime) appendChar(n)
              else if (n != '$') oswriter.write(UTF16.valueOf(n))
          }
      }
    } finally oswriter.close
  }

  implicit def stringExpansionDecorator(s: String) = new {
    def expand(context: Context) = apply(context, s)
  }

}
