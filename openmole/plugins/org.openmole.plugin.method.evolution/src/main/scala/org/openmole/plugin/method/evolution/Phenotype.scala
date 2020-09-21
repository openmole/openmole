package org.openmole.plugin.method.evolution

import org.openmole.core.dsl._
import `extension`._
import mgo.tools.CanBeNaN

object Phenotype {

  implicit def phenotypeCanBeNaN: CanBeNaN[Phenotype] = new CanBeNaN[Phenotype] {
    override def isNaN(t: Phenotype): Boolean = {
      def anyIsNaN(t: Any): Boolean = t match {
        case x: Double ⇒ x.isNaN
        case x: Float  ⇒ x.isNaN
        case _         ⇒ false
      }
      t.value.exists(anyIsNaN)
    }
  }

  def objective(objectives: Seq[Objective[_]], phenotype: Phenotype) = phenotype.value.take(objectives.size)

  def fromContext(context: Context, objectives: Seq[Val[_]]) = new Phenotype(objectives.map(o ⇒ context(o)).toArray)
}

class Phenotype(val value: Array[Any]) extends AnyVal
