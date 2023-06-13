package org.openmole.plugin.method.evolution

import io.circe.derivation
import org.openmole.core.omr.*

case class SaveOption(
  frequency: Option[Long],
  last:      Boolean)

object EvolutionMetadata:

  import io.circe.*

  given Codec[EvolutionMetadata] = Codec.AsObject.derivedConfigured
  given MethodMetaData[EvolutionMetadata] = MethodMetaData[EvolutionMetadata](_ ⇒ EvolutionMetadata.method)

  enum GenomeBoundData:
    case IntBound(value: ValData, low: Int, high: Int, intervalType: GenomeBoundData.IntervalType)
    case DoubleBound(value: ValData, low: Double, high: Double, intervalType: GenomeBoundData.IntervalType)
    case IntSequenceBound(value: ValData, low: Seq[Int], high: Seq[Int], intervalType: GenomeBoundData.IntervalType)
    case DoubleSequenceBound(value: ValData, low: Seq[Double], high: Seq[Double], intervalType: GenomeBoundData.IntervalType)
    case Enumeration(value: ValData, values: Seq[String])

  object GenomeBoundData:
    given Codec[GenomeBoundData] = Codec.AsObject.derivedConfigured

    enum IntervalType:
      case Continuous, Discrete

    export IntervalType.{Continuous, Discrete}

    def name(data: GenomeBoundData) =
      data match
        case d: DoubleBound         ⇒ d.value.name
        case d: IntBound            ⇒ d.value.name
        case d: DoubleSequenceBound ⇒ d.value.name
        case d: IntSequenceBound    ⇒ d.value.name
        case d: Enumeration         ⇒ d.value.name

  object Objective:
    given Codec[Objective] = Codec.AsObject.derivedConfigured

  case class Objective(
    name:     String,
    delta:    Option[Double],
    negative: Boolean,
    noisy:    Boolean)

  def method = "evolution"
  case object none extends EvolutionMetadata

  object PSE:
    type Grid = Seq[PSE.GridAxe]

    object GridAxe:
      given Codec[GridAxe] = Codec.AsObject.derivedConfigured

    case class GridAxe(objective: String, grid: Seq[Double])

  case class PSE(
    genome:     Seq[GenomeBoundData],
    objective:  Seq[Objective],
    grid:       PSE.Grid,
    generation: Long,
    saveOption: SaveOption) extends EvolutionMetadata

  case class StochasticPSE(
    genome:     Seq[GenomeBoundData],
    objective:  Seq[Objective],
    grid:       PSE.Grid,
    sample:     Int,
    generation: Long,
    saveOption: SaveOption) extends EvolutionMetadata

  object Profile

  case class Profile(
    genome:     Seq[GenomeBoundData],
    objective:  Seq[Objective],
    generation: Long,
    saveOption: SaveOption) extends EvolutionMetadata

  case class NSGA2(
    genome:         Seq[GenomeBoundData],
    objective:      Seq[Objective],
    populationSize: Int,
    generation:     Long,
    saveOption:     SaveOption) extends EvolutionMetadata

  case class StochasticNSGA2(
    genome:         Seq[GenomeBoundData],
    objective:      Seq[Objective],
    populationSize: Int,
    sample:         Int,
    generation:     Long,
    saveOption:     SaveOption) extends EvolutionMetadata


sealed trait EvolutionMetadata

object AnalysisData:

  type GenomeData = String
  type ObjectiveData = String

  sealed trait Convergence
  sealed trait Generation

  object NSGA2:
    case class Generation(generation: Long, genome: Vector[Vector[GenomeData]], objective: Vector[Objective]) extends AnalysisData.Generation
    case class Objective(objectives: Vector[Double])
    case class Convergence(nadir: Option[Vector[Double]], generations: Vector[GenerationConvergence]) extends AnalysisData.Convergence
    case class GenerationConvergence(generation: Long, hypervolume: Option[Double], minimums: Option[Vector[Double]])

  object StochasticNSGA2:
    case class Generation(generation: Long, genome: Vector[Vector[GenomeData]], objective: Vector[Objective]) extends AnalysisData.Generation
    case class Objective(objectives: Vector[Double], samples: Int)
    case class Convergence(nadir: Option[Vector[Double]], generations: Vector[GenerationConvergence]) extends AnalysisData.Convergence
    case class GenerationConvergence(generation: Long, hypervolume: Option[Double], minimums: Option[Vector[Double]])



