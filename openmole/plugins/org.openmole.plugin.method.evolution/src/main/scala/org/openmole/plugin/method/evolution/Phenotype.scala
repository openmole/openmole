package org.openmole.plugin.method.evolution

import org.openmole.core.dsl._
import `extension`._
import mgo.tools.CanBeNaN

object Phenotype:

  implicit def phenotypeCanBeNaN: CanBeNaN[Phenotype] = (t: Phenotype) ⇒
    def anyIsNaN(t: Any): Boolean =
      t match
        case x: Double ⇒ x.isNaN
        case x: Float  ⇒ x.isNaN
        case _         ⇒ false

    t.values.exists(anyIsNaN)


  def toContext(content: PhenotypeContent, phenotype: Phenotype) =
    (PhenotypeContent.toVals(content) zip phenotype.values).map { case (v, va) ⇒ Variable.unsecure(v, va) }

  def objectives(content: PhenotypeContent, phenotype: Phenotype) =
    val context: Context = toContext(content, phenotype)
    content.objectives.map(v ⇒ context.apply(v)).toArray

  def outputs(content: PhenotypeContent, phenotype: Phenotype) =
    val context: Context = toContext(content, phenotype)
    content.outputs.map(v ⇒ context.apply(v)).toArray

  def fromContext(context: Context, content: PhenotypeContent): Phenotype =
    val value = PhenotypeContent.toVals(content).map(o ⇒ context(o))
    Phenotype(IArray.from(value))

case class Phenotype(values: IArray[Any]) extends AnyVal

object PhenotypeContent:
  def toVals(p: PhenotypeContent) =
    (p.objectives ++ p.outputs).distinct



case class PhenotypeContent(objectives: Seq[Val[?]], outputs: Seq[Val[?]] = Seq())