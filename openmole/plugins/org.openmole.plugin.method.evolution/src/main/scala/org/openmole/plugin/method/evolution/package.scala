/*
 * Copyright (C) 22/11/12 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.method

import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.puzzle._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.transition._
import org.openmole.plugin.task.tools._
import org.openmole.plugin.tool.pattern._
import org.openmole.core.context._
import org.openmole.core.expansion._
import org.openmole.tool.types._
import squants.time.Time
import mgo.double2Scalable
import org.openmole.core.fileservice.FileService

import org.openmole.core.workflow.tools._

import scala.annotation.tailrec

package object evolution {

  val operatorExploration = 0.1

  object Genome {

    sealed trait GenomeBound

    object GenomeBound {
      case class SequenceOfDouble(v: Val[Array[Double]], low: FromContext[Array[Double]], high: FromContext[Array[Double]]) extends GenomeBound
      case class ScalarDouble(v: Val[Double], low: FromContext[Double], high: FromContext[Double]) extends GenomeBound
      case class SequenceOfInt(v: Val[Array[Int]], low: FromContext[Array[Int]], high: FromContext[Array[Int]]) extends GenomeBound
      case class ScalarInt(v: Val[Int], low: FromContext[Int], high: FromContext[Int]) extends GenomeBound
      case class Enumeration[T](v: Val[T], values: Vector[T]) extends GenomeBound

      import org.openmole.core.workflow.domain._
      import org.openmole.core.workflow.sampling._

      implicit def factorToBoundDouble[D](f: Factor[D, Double])(implicit bounded: Bounds[D, Double]) =
        ScalarDouble(f.prototype, bounded.min(f.domain), bounded.max(f.domain))

      implicit def factorToBoundInt[D](f: Factor[D, Int])(implicit bounded: Bounds[D, Int]) =
        ScalarInt(f.prototype, bounded.min(f.domain), bounded.max(f.domain))

      implicit def factorOfSequenceIsScalableDouble[D](f: Factor[D, Array[Double]])(implicit bounded: Bounds[D, Array[Double]]) =
        SequenceOfDouble(f.prototype, bounded.min(f.domain), bounded.max(f.domain))

      implicit def factorOfSequenceIsScalableInt[D](f: Factor[D, Array[Int]])(implicit bounded: Bounds[D, Array[Int]]) =
        SequenceOfInt(f.prototype, bounded.min(f.domain), bounded.max(f.domain))

      implicit def fixIsEnumeration[D, T](f: Factor[D, T])(implicit fix: Fix[D, T]) =
        Enumeration(f.prototype, fix.apply(f.domain).toVector)

    }

    import _root_.mgo.{ C, D }
    import cats.implicits._

    def continuous(genome: Genome) = {
      val bounds = genome.toVector.collect {
        case s: GenomeBound.ScalarDouble ⇒
          (s.low map2 s.high) { case (l, h) ⇒ Vector(C(l, h)) }
        case s: GenomeBound.SequenceOfDouble ⇒
          (s.low map2 s.high) { case (low, high) ⇒ (low zip high).toVector.map { case (l, h) ⇒ C(l, h) } }
      }
      bounds.sequence.map(_.flatten)
    }

    def discrete(genome: Genome) = {
      val bounds = genome.toVector.collect {
        case s: GenomeBound.ScalarInt ⇒
          (s.low map2 s.high) { case (l, h) ⇒ Vector(D(l, h)) }
        case s: GenomeBound.SequenceOfInt ⇒
          (s.low map2 s.high) { case (low, high) ⇒ (low zip high).toVector.map { case (l, h) ⇒ D(l, h) } }
        case s: GenomeBound.Enumeration[_] ⇒
          FromContext { _ ⇒ Vector(D(0, s.values.size - 1)) }
      }
      bounds.sequence.map(_.flatten)
    }

    def vals(genome: Genome) =
      genome.map {
        case b: GenomeBound.ScalarDouble     ⇒ b.v
        case b: GenomeBound.ScalarInt        ⇒ b.v
        case b: GenomeBound.SequenceOfDouble ⇒ b.v
        case b: GenomeBound.SequenceOfInt    ⇒ b.v
        case b: GenomeBound.Enumeration[_]   ⇒ b.v
      }

    def size(g: GenomeBound.SequenceOfDouble) = (g.low map2 g.high) { case (l, h) ⇒ math.min(l.size, h.size) }
    def size(g: GenomeBound.SequenceOfInt) = (g.low map2 g.high) { case (l, h) ⇒ math.min(l.size, h.size) }

    def continuousIndex(genome: Genome, v: Val[_]): Option[FromContext[Int]] = {
      def indexOf0(l: List[GenomeBound], index: FromContext[Int]): Option[FromContext[Int]] = {
        l match {
          case Nil                                    ⇒ None
          case (h: GenomeBound.ScalarDouble) :: t     ⇒ if (h.v == v) Some(index) else indexOf0(t, index.map(_ + 1))
          case (h: GenomeBound.SequenceOfDouble) :: t ⇒ if (h.v == v) Some(index) else indexOf0(t, (index map2 size(h)) { case (i, h) ⇒ i + h })
          case h :: t                                 ⇒ indexOf0(t, index)
        }
      }
      indexOf0(genome.toList, 0)
    }

    def discreteIndex(genome: Genome, v: Val[_]): Option[FromContext[Int]] = {
      def indexOf0(l: List[GenomeBound], index: FromContext[Int]): Option[FromContext[Int]] = {
        l match {
          case Nil                                  ⇒ None
          case (h: GenomeBound.ScalarInt) :: t      ⇒ if (h.v == v) Some(index) else indexOf0(t, index.map(_ + 1))
          case (h: GenomeBound.Enumeration[_]) :: t ⇒ if (h.v == v) Some(index) else indexOf0(t, index.map(_ + 1))
          case (h: GenomeBound.SequenceOfInt) :: t  ⇒ if (h.v == v) Some(index) else indexOf0(t, (index map2 size(h)) { case (i, h) ⇒ i + h })
          case h :: t                               ⇒ indexOf0(t, index)
        }
      }
      indexOf0(genome.toList, 0)
    }

    def toVariables(genome: Genome, continuousValues: Vector[Double], discreteValue: Vector[Int], scale: Boolean) = {

      def toVariables0(genome: List[Genome.GenomeBound], continuousValues: List[Double], discreteValues: List[Int], acc: List[Variable[_]]): FromContext[Vector[Variable[_]]] = FromContext { p ⇒
        import p._
        genome match {
          case Nil ⇒ acc.reverse.toVector
          case (h: GenomeBound.ScalarDouble) :: t ⇒
            val value = if (scale) continuousValues.head.scale(h.low.from(context), h.high.from(context)) else continuousValues.head
            val v = Variable(h.v, value)
            toVariables0(t, continuousValues.tail, discreteValues, v :: acc).from(context)
          case (h: GenomeBound.SequenceOfDouble) :: t ⇒
            val value = (h.low.from(context) zip h.high.from(context) zip continuousValues) map { case ((l, h), v) ⇒ if (scale) v.scale(l, h) else v }
            val v = Variable(h.v, value)
            toVariables0(t, continuousValues.drop(value.size), discreteValues, v :: acc).from(context)
          case (h: GenomeBound.ScalarInt) :: t ⇒
            val v = Variable(h.v, discreteValues.head)
            toVariables0(t, continuousValues, discreteValues.tail, v :: acc).from(context)
          case (h: GenomeBound.SequenceOfInt) :: t ⇒
            val value = (h.low.from(context) zip h.high.from(context) zip discreteValues) map { case (_, v) ⇒ v }
            val v = Variable(h.v, value)
            toVariables0(t, continuousValues, discreteValues.drop(value.size), v :: acc).from(context)
          case (h: GenomeBound.Enumeration[_]) :: t ⇒
            val value = h.values(discreteValue.head)
            val v = Variable(h.v, value)
            toVariables0(t, continuousValues, discreteValues.tail, v :: acc).from(context)
        }
      }

      toVariables0(genome.toList, continuousValues.toList, discreteValue.toList, List.empty)
    }
  }

  object Objective {
    implicit def valToObjective[T](v: Val[T])(implicit td: ToDouble[T]) = Objective(v, context ⇒ td(context(v)))
    def index(obj: Objectives, v: Val[_]) = obj.indexWhere(_.prototype == v) match {
      case -1 ⇒ None
      case x  ⇒ Some(x)
    }
  }

  case class Objective(prototype: Val[_], fromContext: Context ⇒ Double)

  type Objectives = Seq[Objective]
  type FitnessAggregation = Seq[Double] ⇒ Double
  type Genome = Seq[Genome.GenomeBound]

  implicit def intToCounterTerminationConverter(n: Long): AfterGeneration = AfterGeneration(n)
  implicit def durationToDurationTerminationConverter(d: Time): AfterDuration = AfterDuration(d)

  object OMTermination {
    def toTermination(oMTermination: OMTermination, integration: EvolutionWorkflow) =
      oMTermination match {
        case AfterGeneration(s) ⇒ (population: Vector[integration.I]) ⇒ integration.operations.afterGeneration(s, population)
        case AfterDuration(d) ⇒ (population: Vector[integration.I]) ⇒ integration.operations.afterDuration(d, population)
      }
  }

  sealed trait OMTermination
  case class AfterGeneration(steps: Long) extends OMTermination
  case class AfterDuration(duration: Time) extends OMTermination

  import shapeless._

  def SteadyStateEvolution[T](algorithm: T, evaluation: Puzzle, termination: OMTermination, parallelism: Int = 1)(implicit wfi: WorkflowIntegration[T]) = {
    val t = wfi(algorithm)

    val evaluationCapsule = Slot(MoleTask(evaluation))

    val randomGenomes =
      BreedTask(algorithm, parallelism) set (
        name := "randomGenome",
        outputs += t.populationPrototype
      )

    val scalingGenomeTask = ScalingGenomeTask(algorithm) set (
      name := "scalingGenome"
    )

    val toOffspring =
      ToOffspringTask(algorithm) set (
        name := "toOffspring"
      )

    val elitismTask = ElitismTask(algorithm) set (name := "elitism")

    val terminationTask = TerminationTask(algorithm, termination) set (name := "termination")

    val breed = BreedTask(algorithm, 1) set (name := "breed")

    val masterFirst =
      EmptyTask() set (
        name := "masterFirst",
        (inputs, outputs) += (t.populationPrototype, t.genomePrototype, t.statePrototype),
        (inputs, outputs) += (t.objectives.map(_.prototype): _*)
      )

    val masterLast =
      EmptyTask() set (
        name := "masterLast",
        (inputs, outputs) += (t.populationPrototype, t.statePrototype, t.genomePrototype.toArray, t.terminatedPrototype, t.generationPrototype)
      )

    val masterFirstCapsule = Capsule(masterFirst)
    val elitismSlot = Slot(elitismTask)
    val masterLastSlot = Slot(masterLast)
    val terminationCapsule = Capsule(terminationTask)
    val breedSlot = Slot(breed)

    val master =
      (masterFirstCapsule --
        (toOffspring keep (Seq(t.statePrototype, t.genomePrototype) ++ t.objectives.map(_.prototype): _*)) --
        elitismSlot --
        terminationCapsule --
        breedSlot --
        masterLastSlot) &
        (masterFirstCapsule -- (elitismSlot keep t.populationPrototype)) &
        (elitismSlot -- (breedSlot keep t.populationPrototype)) &
        (elitismSlot -- (masterLastSlot keep t.populationPrototype)) &
        (terminationCapsule -- (masterLastSlot keep (t.terminatedPrototype, t.generationPrototype)))

    val masterTask = MoleTask(master) set (exploredOutputs += t.genomePrototype.toArray)

    val masterSlave = MasterSlave(
      randomGenomes,
      masterTask,
      scalingGenomeTask -- Strain(evaluationCapsule),
      t.populationPrototype, t.statePrototype
    )

    val firstTask = InitialStateTask(algorithm) set (name := "first")

    val firstCapsule = Capsule(firstTask, strain = true)

    val last = EmptyTask() set (
      name := "last",
      (inputs, outputs) += (t.statePrototype, t.populationPrototype)
    )

    val puzzle =
      ((firstCapsule -- masterSlave) >| (Capsule(last, strain = true) when t.terminatedPrototype)) &
        (firstCapsule oo (evaluationCapsule, filter = Block(t.populationPrototype, t.statePrototype)))

    val gaPuzzle =
      new OutputEnvironmentPuzzleContainer(puzzle, masterSlave.last, evaluationCapsule) {
        def generation = t.generationPrototype
        def population = t.populationPrototype
        def state = t.statePrototype
      }

    gaPuzzle :: algorithm :: HNil
  }

  def IslandEvolution[HL <: HList, T](
    island:      HL,
    parallelism: Int,
    termination: OMTermination,
    sample:      OptionalArgument[Int] = None
  )(implicit
    wfi: WorkflowIntegrationSelector[HL, T],
    selectPuzzle: ops.hlist.Selector[HL, _ <: PuzzleContainer]) = {
    val algorithm: T = wfi(island)
    implicit val wi = wfi.wi
    val t = wi(algorithm)

    val islandPopulationPrototype = t.populationPrototype.withName("islandPopulation")

    val masterFirst =
      EmptyTask() set (
        name := "masterFirst",
        (inputs, outputs) += (t.populationPrototype, t.offspringPrototype, t.statePrototype)
      )

    val masterLast =
      EmptyTask() set (
        name := "masterLast",
        (inputs, outputs) += (t.populationPrototype, t.statePrototype, islandPopulationPrototype.toArray, t.terminatedPrototype, t.generationPrototype)
      )

    val elitismTask = ElitismTask(algorithm) set (name := "elitism")

    val generateIsland = GenerateIslandTask(algorithm, sample, 1, islandPopulationPrototype)

    val terminationTask = TerminationTask(algorithm, termination) set (name := "termination")

    val islandPopulationToPopulation =
      AssignTask(islandPopulationPrototype → t.populationPrototype) set (
        name := "islandPopulationToPopulation"
      )

    val reassingRNGTask = ReassignStateRNGTask(algorithm)

    val fromIsland = FromIslandTask(algorithm)

    val populationToOffspring =
      AssignTask(t.populationPrototype → t.offspringPrototype) set (
        name := "populationToOffspring"
      )

    val elitismSlot = Slot(elitismTask)
    val terminationCapsule = Capsule(terminationTask)
    val masterLastSlot = Slot(masterLast)

    val master =
      (
        masterFirst --
        (elitismSlot keep (t.statePrototype, t.populationPrototype, t.offspringPrototype)) --
        terminationCapsule --
        (masterLastSlot keep (t.terminatedPrototype, t.generationPrototype, t.statePrototype))
      ) &
        (elitismSlot -- generateIsland -- masterLastSlot) &
        (elitismSlot -- (masterLastSlot keep t.populationPrototype))

    val masterTask = MoleTask(master) set (
      name := "islandMaster",
      exploredOutputs += islandPopulationPrototype.toArray
    )

    val generateInitialIslands =
      GenerateIslandTask(algorithm, sample, parallelism, islandPopulationPrototype) set (
        name := "generateInitialIslands",
        (inputs, outputs) += t.statePrototype,
        outputs += t.populationPrototype
      )

    val islandCapsule = Slot(MoleTask(selectPuzzle(island)))

    val slaveFist = EmptyTask() set (
      name := "slaveFirst",
      (inputs, outputs) += (t.statePrototype, islandPopulationPrototype)
    )

    val slave = slaveFist -- (islandPopulationToPopulation, reassingRNGTask) -- islandCapsule -- fromIsland -- populationToOffspring

    val masterSlave = MasterSlave(
      generateInitialIslands,
      masterTask,
      slave,
      t.populationPrototype, t.statePrototype
    )

    val firstTask = InitialStateTask(algorithm) set (name := "first")

    val firstCapsule = Capsule(firstTask, strain = true)

    val last = EmptyTask() set (
      name := "last",
      (inputs, outputs) += (t.populationPrototype, t.statePrototype)
    )

    val puzzle =
      ((firstCapsule -- masterSlave) >| (Capsule(last, strain = true) when t.terminatedPrototype)) &
        (firstCapsule oo (islandCapsule, Block(t.populationPrototype, t.statePrototype)))

    val gaPuzzle =
      new OutputEnvironmentPuzzleContainer(puzzle, masterSlave.last, islandCapsule) {
        def generation = t.generationPrototype
        def population = t.populationPrototype
        def state = t.statePrototype
      }

    gaPuzzle :: algorithm :: HNil
  }

}
