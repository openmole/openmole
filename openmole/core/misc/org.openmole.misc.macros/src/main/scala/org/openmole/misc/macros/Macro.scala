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

  def getValName(c: Context): c.Expr[String] = {
    def enclosingTrees(c: Context): Seq[c.Tree] =
      c.asInstanceOf[reflect.macros.runtime.Context].callsiteTyper.context.enclosingContextChain.map(_.tree.asInstanceOf[c.Tree])
    def invalidEnclosingTree(s: String): String = ???

    import c.universe.{ Apply ⇒ ApplyTree, _ }

    val methodName = c.macroApplication.symbol.name

    def processName(n: Name): String = n.decoded.trim // trim is not strictly correct, but macros don't expose the API necessary

    def enclosingVal(trees: List[c.Tree]): String =
      {
        trees match {
          case vd @ ValDef(_, name, _, _) :: ts ⇒ processName(name)
          case (_: ApplyTree | _: Select | _: TypeApply) :: xs ⇒
            println(trees)
            enclosingVal(xs)
          // lazy val x: X = <methodName> has this form for some reason (only when the explicit type is present, though)
          case Block(_, _) :: DefDef(mods, name, _, _, _, _) :: xs if mods.hasFlag(Flag.LAZY) ⇒ processName(name)
          case _ ⇒
            c.error(c.enclosingPosition, invalidEnclosingTree(methodName.decoded))
            "<error>"
        }
      }
    c.Expr[String](Literal(Constant(enclosingVal(enclosingTrees(c).toList))))
  }

  def !! : String = macro getValName
}
