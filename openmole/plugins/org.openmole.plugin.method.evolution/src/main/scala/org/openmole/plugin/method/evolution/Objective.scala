package org.openmole.plugin.method.evolution

import org.openmole.core.context.{ Context, Val }
import org.openmole.tool.types.ToDouble

object Objective {
  implicit def valToObjective[T](v: Val[T])(implicit td: ToDouble[T]) = Objective(v, context ⇒ td(context(v)))
  def index(obj: Objectives, v: Val[_]) = obj.indexWhere(_.prototype == v) match {
    case -1 ⇒ None
    case x  ⇒ Some(x)
  }
}

case class Objective(prototype: Val[_], fromContext: Context ⇒ Double)

