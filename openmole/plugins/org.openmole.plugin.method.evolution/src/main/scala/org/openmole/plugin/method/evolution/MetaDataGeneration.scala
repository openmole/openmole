package org.openmole.plugin.method.evolution

import org.openmole.plugin.method.evolution.data._
import org.openmole.core.expansion.FromContext
import io.circe._
import io.circe.generic.extras.auto._
import io.circe.parser._
import io.circe.generic.extras.semiauto._
import io.circe.generic.extras.Configuration
import org.openmole.core.exception.InternalProcessingError
import org.openmole.core.tools.io.Prettifier._
import Genome.GenomeBound
import org.openmole.plugin.hook.omr.MethodData

object MetadataGeneration {

  def genomeData(g: Genome) = g.map(boundData)

  def boundData(b: GenomeBound) = {
    import EvolutionMetadata._

    b match {
      case b: GenomeBound.ScalarDouble             ⇒ GenomeBoundData.DoubleBound(b.v.name, b.low, b.high)
      case b: GenomeBound.ScalarInt                ⇒ GenomeBoundData.IntBound(b.v.name, b.low, b.high)
      case b: GenomeBound.SequenceOfDouble         ⇒ GenomeBoundData.DoubleSequenceBound(b.v.name, b.low, b.high)
      case b: GenomeBound.SequenceOfInt            ⇒ GenomeBoundData.IntSequenceBound(b.v.name, b.low, b.high)
      case b: GenomeBound.Enumeration[_]           ⇒ GenomeBoundData.Enumeration(b.v.name, b.values.map(_.prettify()))
      case b: GenomeBound.SequenceOfEnumeration[_] ⇒ GenomeBoundData.Enumeration(b.v.name, b.values.map(_.prettify()))
    }
  }

  def exactObjectiveData(o: ExactObjective[_]) =
    EvolutionMetadata.ExactObjective(o.as.getOrElse(o.prototype.name), o.delta, o.negative)

  def noisyObjectiveData(o: NoisyObjective[_]) =
    EvolutionMetadata.NoisyObjective(o.as.getOrElse(o.prototype.name), o.delta, o.negative)

  def fromString(s: String): EvolutionMetadata =
    decode[EvolutionMetadata](s) match {
      case Left(e)  ⇒ throw new InternalProcessingError(s"Error parsing metadata $s", e)
      case Right(m) ⇒ m
    }

  def grid(p: Seq[PSE.PatternAxe]) =
    (Objectives.resultPrototypes(p.map(_.p)) zip p) map { case (p, pa) ⇒ EvolutionMetadata.PSE.GridAxe(p.name, pa.scale) }

}
