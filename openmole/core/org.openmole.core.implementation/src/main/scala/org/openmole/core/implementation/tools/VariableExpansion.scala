/*
 * Copyright (C) 2010 reuillon
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
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.InputStream
import org.openmole.misc.tools.io.FileUtil
import org.openmole.misc.tools.groovy.GroovyProxy
import org.openmole.core.implementation.data.Context
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IVariable
import scala.math.BigDecimal
import scala.util.control.Breaks._

object VariableExpansion {

  private val patternBegin = '{'
  private val patternEnd = '}'
  private val eval = "$" + patternBegin
    
  def expandBigDecimalData(context: IContext, s: String): BigDecimal = {
    BigDecimal(expandData(context, s))
  }

  def expandDoubleData(context: IContext, s: String): Double = {
    expandData(context, s).toDouble
  }

  def expandIntegerData(context: IContext, s: String): Int = {
    expandData(context, s).toInt
  }

  def expandData(context: IContext, s: String): String = {
    expandData(context, Iterable.empty, s)
  }

  def expandData(context: IContext, tmpVariable: Iterable[IVariable[_]], s: String): String = {
    expandDataInernal(context, tmpVariable, s)
  }

  private def expandDataInernal(context: IContext, tmpVariable: Iterable[IVariable[_]], s: String): String = {
    var ret = s
    val allVariables = new Context
    allVariables ++= context
    allVariables ++= tmpVariable
    var cur = 0
    
    breakable { do {
        val beginIndex = ret.indexOf(eval)
        if(beginIndex == -1) break
        var cur = beginIndex + 2
        var curLevel = 0
      
        breakable { while (cur < ret.length) {
            val curChar = ret.charAt(cur)

            if (curChar == patternEnd) {
              if(curLevel == 0) break
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
      } while (true) }


    ret
  }

  def expandData(replace: Map[String, String], tmpVariable: Iterable[IVariable[_]], s: String): String = {
    expandDataInternal(replace, tmpVariable, s)
  }

  def expandDataInternal(replace: Map[String, String], tmpVariable: Iterable[IVariable[_]], s: String): String = {
    var ret = s
    var beginIndex = -1
    var endIndex = -1
    
    breakable { do {
        beginIndex = ret.indexOf(patternBegin)
        endIndex = ret.indexOf(patternEnd)

        if (beginIndex == -1 || endIndex == -1) break
        
        val toReplace = ret.substring(beginIndex, endIndex + 1);

        val varName = getVarName(toReplace);

        ret = replace.get(varName) match {
          case Some(toInsert) => ret.substring(0, beginIndex) + toInsert + ret.substring(endIndex + 1)
          case None =>ret.substring(0, beginIndex) + ret.substring(endIndex + 1)
        }
      
      } while (true) }

    ret
  }

  private def getVarName(str: String): String = {
    str.substring(1, str.length - 1)
  }

  protected def expandOneData(allVariables: IContext, variableExpression: String): String = {
    allVariables.variable(variableExpression) match {
      case Some(variable) => variable.value.toString
      case None => 
        val shell = new GroovyProxy(variableExpression, Iterable.empty) with GroovyContextAdapter
        shell.execute(allVariables).toString
    }
  }
  
  
  def expandBufferData(context: IContext,is: InputStream,os: OutputStream)= {
    val isreader = new InputStreamReader(is, "UTF-8");
    val oswriter = new OutputStreamWriter(os)
    
    var openbrace = 0
    var closebrace = 0
    var n = 0
    var expandTime = false
    var esbuilder = new StringBuffer(UTF16.valueOf('$')).append(UTF16.valueOf('{'))
    
    def appendChar(c: Int)= esbuilder.append(UTF16.valueOf(n))
   
    try{
      while({n = isreader.read; n} != -1) {
        n match {
          case '{' => {
              openbrace+= 1
              expandTime= true
              if (openbrace > 1) appendChar(n)
              else esbuilder = new StringBuffer(UTF16.valueOf('$')).append(UTF16.valueOf('{'))
            }
          case '}' => {
              closebrace+= 1
              appendChar(n)
              if (openbrace == closebrace) {
                oswriter.write(expandData(context,esbuilder.toString))
                expandTime= false
                openbrace= 0
                closebrace= 0
              }
            }
          case _ => {
              if (expandTime)
                appendChar(n)
              else if (n !='$')
                oswriter.write(UTF16.valueOf(n))
            }
        }    
      }
    }
    finally {
      oswriter.close()
    }
  }
  
}
