package org.openmole.core

import reflect.macros.Context
import scala.language.experimental.macros
import org.openmole.core.model.data.Prototype
import org.openmole.misc.macros.SimpleMacros
import scala.reflect.Manifest.classType

/**
 * Created with IntelliJ IDEA.
 * User: luft
 * Date: 6/23/13
 * Time: 5:04 PM
 */
package object convenience {

  def prototype[T]: Prototype[T] = macro prototypeImpl[T]

  def prototypeImpl[T: c.WeakTypeTag](c: Context): c.Expr[Prototype[T]] = {
    import c.universe._

    val n = SimpleMacros.getValName(c)

    val x: Expr[String] = c.Expr[String](Literal(Constant(evidence$1.toString())))

    val expr: Expr[Manifest[T]] = reify { classType(Class.forName(x.splice)) }

    reify {
      new Prototype[T] {
        /**
         * Get the type of the prototype.
         *
         * @return the type of the prototype
         */
        def `type`: Manifest[T] = expr.splice

        /**
         * Get the name of the prototype.
         *
         * @return the name of the prototype
         */
        def name: String = n.splice
      }
    }
  }

}
