package org.openmole.plugin.method.evolution

import org.openmole.plugin.method.evolution.data._
import org.openmole.core.expansion.FromContext
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.generic.semiauto._
//import io.circe.generic.extras.Configuration
import org.openmole.core.exception.InternalProcessingError
import org.openmole.core.tools.io.Prettifier._
import Genome.GenomeBound
import org.openmole.plugin.hook.omr.*
import org.openmole.tool.types.TypeTool

object MetadataGeneration {

  def genomeData(g: Genome) = g.map(boundData)

  def boundData(b: GenomeBound) = {
    import EvolutionMetadata._
    import org.openmole.plugin.tool.methoddata._

    b match {
      case b: GenomeBound.ScalarDouble             ⇒ GenomeBoundData.DoubleBound(ValData(b.v), low = b.low, high = b.high, intervalType = GenomeBoundData.Continuous)
      case b: GenomeBound.ScalarInt                ⇒ GenomeBoundData.IntBound(ValData(b.v), low = b.low, high = b.high, intervalType = GenomeBoundData.Discrete)
      case b: GenomeBound.ContinuousInt            ⇒ GenomeBoundData.IntBound(ValData(b.v), low = b.low, high = b.high, intervalType = GenomeBoundData.Continuous)
      case b: GenomeBound.SequenceOfDouble         ⇒ GenomeBoundData.DoubleSequenceBound(ValData(b.v), low = b.low, high = b.high, intervalType = GenomeBoundData.Continuous)
      case b: GenomeBound.SequenceOfInt            ⇒ GenomeBoundData.IntSequenceBound(ValData(b.v), low = b.low, high = b.high, intervalType = GenomeBoundData.Discrete)
      case b: GenomeBound.Enumeration[_]           ⇒ GenomeBoundData.Enumeration(ValData(b.v), b.values.map(_.prettify()))
      case b: GenomeBound.SequenceOfEnumeration[_] ⇒ GenomeBoundData.Enumeration(ValData(b.v), b.values.map(_.prettify()))
    }
  }

  def objectiveData(o: Objective) =
    EvolutionMetadata.Objective(Objective.resultPrototype(o).name, o.delta, o.negative, o.noisy)

  def fromString(s: String): EvolutionMetadata =
    decode[EvolutionMetadata](s) match {
      case Left(e)  ⇒ throw new InternalProcessingError(s"Error parsing metadata $s", e)
      case Right(m) ⇒ m
    }

  def grid(p: Seq[PSE.PatternAxe]) =
    (Objectives.resultPrototypes(p.map(_.p)) zip p) map { case (p, pa) ⇒ EvolutionMetadata.PSE.GridAxe(p.name, pa.scale) }

}
