package org.openmole.plugin.method.evolution

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.tool.types.ToDouble
object DeltaTask {

  def apply(objective: Delta*)(implicit name: sourcecode.Name, definitionScope: DefinitionScope) =
    Task("DeltaTask") { p ⇒
      import p._
      context ++ objective.map {
        case DeltaDouble(v, o) ⇒ Variable(v, math.abs(context(v) - o))
        case DeltaInt(v, o)    ⇒ Variable(v, math.abs(context(v) - o))
        case DeltaLong(v, o)   ⇒ Variable(v, math.abs(context(v) - o))
      }
    } set (
      (inputs, outputs) ++= objective.map(Delta.v)
    )

  sealed trait Delta
  case class DeltaDouble(v: Val[Double], objective: Double) extends Delta
  case class DeltaInt(v: Val[Int], objective: Int) extends Delta
  case class DeltaLong(v: Val[Long], objective: Long) extends Delta

  object Delta {
    implicit def fromTupleDouble[T](t: (Val[Double], T))(implicit toDouble: ToDouble[T]): DeltaDouble = DeltaDouble(t._1, toDouble(t._2))
    implicit def fromTupleInt(t: (Val[Int], Int)): DeltaInt = DeltaInt(t._1, t._2)
    implicit def fromTupleLong(t: (Val[Long], Long)): DeltaLong = DeltaLong(t._1, t._2)

    def v(delta: Delta) =
      delta match {
        case DeltaDouble(v, _) ⇒ v
        case DeltaInt(v, _)    ⇒ v
        case DeltaLong(v, _)   ⇒ v
      }

  }

}

object Delta {
  import org.openmole.core.setter.DefinitionScope

  def apply(dsl: DSL, objective: DeltaTask.Delta*)(implicit definitionScope: DefinitionScope) =
    dsl -- DeltaTask(objective *)

}
