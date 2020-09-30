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

  def objective(content: PhenotypeContent, phenotype: Phenotype) = phenotype.value.take(content.objectives.size)

  def values(content: PhenotypeContent, phenotype: Phenotype) = phenotype.value.drop(content.objectives.size).take(content.values.size)
  def valuesContext(content: PhenotypeContent, phenotype: Phenotype) = Context((content.values zip values(content, phenotype)).map { case (v, va) ⇒ Variable.unsecure(v, va) }: _*)

  def fromContext(context: Context, content: PhenotypeContent) = {
    val value = content.objectives.map(o ⇒ context(o)) ++ content.values.map(n ⇒ context(n))
    new Phenotype(value.toArray)
  }

}

class Phenotype(val value: Array[Any]) extends AnyVal

object PhenotypeContent {
  def apply(
    objectives: Seq[Objective[_]],
    values:     Seq[Val[_]]       = Seq()): PhenotypeContent =
    new PhenotypeContent(
      objectives.map(Objective.prototype),
      values)

  def toVals(p: PhenotypeContent) =
    p.objectives ++ p.values

}

class PhenotypeContent(val objectives: Seq[Val[_]], val values: Seq[Val[_]])