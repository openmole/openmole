package org.openmole.plugin.method.evolution.data

case class SaveOption(
  frequency: Option[Long],
  last:      Boolean)

object EvolutionMetadata {

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

  case class NoisyObjective(
    name:     String,
    delta:    Option[Double],
    negative: Boolean)

  def method = "evolution"
  case object none extends EvolutionMetadata

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

  object StochasticNSGA2 {
    case class Generation(generation: Long, genome: Vector[Vector[GenomeData]], objective: Vector[Objective]) extends AnalysisData.Generation
    case class Objective(objectives: Vector[ObjectiveData], samples: Int)
    case class Convergence(nadir: Option[Vector[Double]], generations: Vector[GenerationConvergence]) extends AnalysisData.Convergence
    case class GenerationConvergence(generation: Long, hypervolume: Option[Double], minimums: Option[Vector[Double]])
  }

}
