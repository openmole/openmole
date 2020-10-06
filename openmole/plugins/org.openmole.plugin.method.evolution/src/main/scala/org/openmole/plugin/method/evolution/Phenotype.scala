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

  def toContext(content: PhenotypeContent, phenotype: Phenotype) =
    (PhenotypeContent.toVals(content) zip phenotype.value).map { case (v, va) ⇒ Variable.unsecure(v, va) }

  def objectives(content: PhenotypeContent, phenotype: Phenotype) = {
    val context: Context = toContext(content, phenotype)
    content.objectives.map(v ⇒ context.apply(v)).toArray
  }

  def outputs(content: PhenotypeContent, phenotype: Phenotype) = {
    val context: Context = toContext(content, phenotype)
    content.outputs.map(v ⇒ context.apply(v)).toArray
  }

  def fromContext(context: Context, content: PhenotypeContent) = {
    val value = PhenotypeContent.toVals(content).map(o ⇒ context(o))
    new Phenotype(value.toArray)
  }

}

class Phenotype(val value: Array[Any]) extends AnyVal

object PhenotypeContent {
  def apply(
    objectives: Seq[Objective[_]],
    outputs:    Seq[Val[_]]       = Seq()): PhenotypeContent =
    new PhenotypeContent(
      objectives.map(Objective.prototype),
      outputs)

  def toVals(p: PhenotypeContent) =
    (p.objectives ++ p.outputs).distinct

}

class PhenotypeContent(val objectives: Seq[Val[_]], val outputs: Seq[Val[_]])