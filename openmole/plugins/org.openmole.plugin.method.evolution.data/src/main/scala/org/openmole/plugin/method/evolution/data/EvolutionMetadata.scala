package org.openmole.plugin.method.evolution.data

case class SavedData(
  generation: Long,
  frequency:  Option[Long],
  name:       String,
  last:       Boolean)

object EvolutionMetadata {
  def method = "evolution"
  case object none extends EvolutionMetadata
}

sealed trait EvolutionMetadata

case class StochasticNSGA2Data(
  genome:    Seq[GenomeBoundData],
  objective: Seq[NoisyObjectiveData],
  mu:        Int,
  sample:    Int,
  saved:     SavedData) extends EvolutionMetadata

sealed trait GenomeBoundData
case class DoubleBoundData(value: String, low: Double, high: Double) extends GenomeBoundData
case class IntBoundData(value: String, low: Int, high: Int) extends GenomeBoundData
case class DoubleSequenceBoundData(value: String, low: Array[Double], high: Array[Double]) extends GenomeBoundData
case class IntSequenceBoundData(value: String, low: Array[Int], high: Array[Int]) extends GenomeBoundData
case class EnumerationData(value: String, values: Seq[String]) extends GenomeBoundData

case class NoisyObjectiveData(
  name:     String,
  delta:    Option[Double],
  negative: Boolean)