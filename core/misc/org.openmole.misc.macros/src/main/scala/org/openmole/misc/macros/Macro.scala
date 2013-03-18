package org.openmole.misc.macros

import reflect.macros.Context
import scala.language.experimental.macros


/**
 * Created with IntelliJ IDEA.
 * User: luft
 * Date: 3/7/13
 * Time: 2:25 PM
 * To change this template use File | Settings | File Templates.
 */
object SimpleMacros {

  def nomImpl(c: Context): c.Expr[String] = {
    import c.universe._
    val regex = """.*(val|var) ([a-zA-Z][a-zA-Z0-9]*) = .*""".r

    val lineInfo = c.enclosingPosition

    val ret = lineInfo.lineContent match {
      case regex(_,name) => name
      case _ => throw new IllegalArgumentException(s"There is no variable defined on line ${lineInfo.line} of file ${lineInfo.source}: ${lineInfo.lineContent}")
    }
    
    c.literal(ret)
  }

  def !! :String = macro nomImpl
}
