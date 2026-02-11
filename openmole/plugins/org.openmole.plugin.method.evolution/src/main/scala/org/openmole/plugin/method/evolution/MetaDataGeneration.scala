package org.openmole.plugin.method.evolution

import org.openmole.core.argument.FromContext
import io.circe.*
import io.circe.generic.auto.*
import io.circe.parser.*
import io.circe.generic.semiauto.*
import org.openmole.core.exception.InternalProcessingError
import Genome.GenomeBound
import org.openmole.tool.types.TypeTool
import org.openmole.core.dsl.extension.*
import org.openmole.tool.logger.Prettifier

object MetadataGeneration:

  def genomeData(g: Genome) = g.map(boundData)

  def boundData(b: GenomeBound) =
    import EvolutionMetadata.*

    b match
      case b: GenomeBound.ScalarDouble             => GenomeBoundData.DoubleBound(ValData(Genome.resultVariable(b.v)), low = b.low, high = b.high, intervalType = GenomeBoundData.Continuous)
      case b: GenomeBound.ScalarInt                => GenomeBoundData.IntBound(ValData(Genome.resultVariable(b.v)), low = b.low, high = b.high, intervalType = GenomeBoundData.Discrete)
      case b: GenomeBound.ContinuousInt            => GenomeBoundData.IntBound(ValData(Genome.resultVariable(b.v)), low = b.low, high = b.high, intervalType = GenomeBoundData.Continuous)
      case b: GenomeBound.SequenceOfDouble         => GenomeBoundData.DoubleSequenceBound(ValData(Genome.resultVariable(b.v)), low = b.low, high = b.high, intervalType = GenomeBoundData.Continuous)
      case b: GenomeBound.SequenceOfContinuousInt  => GenomeBoundData.IntSequenceBound(ValData(Genome.resultVariable(b.v)), low = b.low, high = b.high, intervalType = GenomeBoundData.Continuous)
      case b: GenomeBound.SequenceOfInt            => GenomeBoundData.IntSequenceBound(ValData(Genome.resultVariable(b.v)), low = b.low, high = b.high, intervalType = GenomeBoundData.Discrete)
      case b: GenomeBound.Enumeration[?]           => GenomeBoundData.Enumeration(ValData(Genome.resultVariable(b.v)), b.values.map(Prettifier.prettify(_)))
      case b: GenomeBound.SequenceOfEnumeration[?] => GenomeBoundData.Enumeration(ValData(Genome.resultVariable(b.v)), b.values.map(Prettifier.prettify(_)))

  def objectivesData(o: Objectives) =
    Objectives.toSeq(o).map(objectiveData)
  
  def objectiveData(o: Objective) =
    EvolutionMetadata.Objective(Objective.resultPrototype(o).name, o.delta, o.negative, o.noisy)
  
  def fromString(s: String): EvolutionMetadata =
    decode[EvolutionMetadata](s) match
      case Left(e)  => throw new InternalProcessingError(s"Error parsing metadata $s", e)
      case Right(m) => m

  def grid(p: Seq[PSE.PatternAxe]) =
    (Objectives.resultPrototypes(p.map(_.p)) zip p) map { case (p, pa) => EvolutionMetadata.PSE.GridAxe(p.name, pa.scale) }


  def density(d: PPSE.Density): EvolutionMetadata.PPSE.Density =
    d match
      case x: PPSE.Density.IndependentJoint => EvolutionMetadata.PPSE.Density.IndependentJoint(x.density.map(density))
      case x: PPSE.Density.GaussianDensity => EvolutionMetadata.PPSE.Density.GaussianDensity(ValData(x.v), mean = x.mean, sd = x.sd)
      case x: PPSE.Density.BetaDensity => EvolutionMetadata.PPSE.Density.BetaDensity(ValData(x.v), alpha = x.alpha, beta = x.beta)
