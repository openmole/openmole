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

  def objective(value: PhenotypeContent, phenotype: Phenotype) = phenotype.value.take(value.objectives.size)
  def fromContext(context: Context, value: PhenotypeContent) = new Phenotype(value.objectives.map(o ⇒ context(o)).toArray)

}

class Phenotype(val value: Array[Any]) extends AnyVal

object PhenotypeContent {
  def apply(objectives: Seq[Objective[_]]): PhenotypeContent = new PhenotypeContent(objectives.map(Objective.prototype))
  def toVals(p: PhenotypeContent) = p.objectives
}

class PhenotypeContent(val objectives: Seq[Val[_]])