package org.openmole.plugin.method.evolution.data

case class SaveOption(
  frequency: Option[Long],
  last:      Boolean)

object EvolutionMetadata {

  import io.circe._
  import io.circe.generic.extras.auto._
  import io.circe.parser._
  import io.circe.generic.extras.semiauto._
  import org.openmole.plugin.hook.omr._

  implicit def methodData = MethodData[EvolutionMetadata](_ ⇒ EvolutionMetadata.method)
  implicit def evolutionMetadataEncoder: Encoder[EvolutionMetadata] = deriveConfiguredEncoder[EvolutionMetadata]
  implicit def evolutionMetadataDecoder: Decoder[EvolutionMetadata] = deriveConfiguredDecoder[EvolutionMetadata]

  object GenomeBoundData {
    case class DoubleBound(name: String, low: Double, high: Double) extends GenomeBoundData
    case class IntBound(name: String, low: Int, high: Int) extends GenomeBoundData
    case class DoubleSequenceBound(name: String, low: Array[Double], high: Array[Double]) extends GenomeBoundData
    case class IntSequenceBound(name: String, low: Array[Int], high: Array[Int]) extends GenomeBoundData
    case class Enumeration(name: String, values: Seq[String]) extends GenomeBoundData

    def name(data: GenomeBoundData) =
      data match {
        case d: DoubleBound         ⇒ d.name
        case d: IntBound            ⇒ d.name
        case d: DoubleSequenceBound ⇒ d.name
        case d: IntSequenceBound    ⇒ d.name
        case d: Enumeration         ⇒ d.name
      }
  }

  sealed trait GenomeBoundData

  case class ExactObjective(
    name:     String,
    delta:    Option[Double],
    negative: Boolean)

  case class NoisyObjective(
    name:     String,
    delta:    Option[Double],
    negative: Boolean)

  def method = "evolution"
  case object none extends EvolutionMetadata

  object PSE {
    type Grid = Seq[PSE.GridAxe]
    case class GridAxe(objective: String, grid: Seq[Double])
  }

  case class PSE(
    genome:     Seq[GenomeBoundData],
    objective:  Seq[ExactObjective],
    grid:       PSE.Grid,
    generation: Long,
    saveOption: SaveOption) extends EvolutionMetadata

  case class StochasticPSE(
    genome:     Seq[GenomeBoundData],
    objective:  Seq[NoisyObjective],
    grid:       PSE.Grid,
    sample:     Int,
    generation: Long,
    saveOption: SaveOption) extends EvolutionMetadata

  case class NSGA2(
    genome:         Seq[GenomeBoundData],
    objective:      Seq[ExactObjective],
    populationSize: Int,
    generation:     Long,
    saveOption:     SaveOption) extends EvolutionMetadata

  case class StochasticNSGA2(
    genome:         Seq[GenomeBoundData],
    objective:      Seq[NoisyObjective],
    populationSize: Int,
    sample:         Int,
    generation:     Long,
    saveOption:     SaveOption) extends EvolutionMetadata

}

sealed trait EvolutionMetadata

object AnalysisData {

  type GenomeData = String
  type ObjectiveData = String

  sealed trait Convergence
  sealed trait Generation

  object NSGA2 {
    case class Generation(generation: Long, genome: Vector[Vector[GenomeData]], objective: Vector[Objective]) extends AnalysisData.Generation
    case class Objective(objectives: Vector[ObjectiveData])
    case class Convergence(nadir: Option[Vector[Double]], generations: Vector[GenerationConvergence]) extends AnalysisData.Convergence
    case class GenerationConvergence(generation: Long, hypervolume: Option[Double], minimums: Option[Vector[Double]])
  }

  object StochasticNSGA2 {
    case class Generation(generation: Long, genome: Vector[Vector[GenomeData]], objective: Vector[Objective]) extends AnalysisData.Generation
    case class Objective(objectives: Vector[ObjectiveData], samples: Int)
    case class Convergence(nadir: Option[Vector[Double]], generations: Vector[GenerationConvergence]) extends AnalysisData.Convergence
    case class GenerationConvergence(generation: Long, hypervolume: Option[Double], minimums: Option[Vector[Double]])
  }

}
