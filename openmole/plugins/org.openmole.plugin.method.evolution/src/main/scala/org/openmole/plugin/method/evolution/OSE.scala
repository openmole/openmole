package org.openmole.plugin.method.evolution

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import cats.implicits._
import monocle.macros.GenLens
import org.openmole.core.context.{ Context, Variable }
import org.openmole.core.expansion.FromContext
import org.openmole.core.workflow.builder.{ DefinitionScope, ValueAssignment }
import org.openmole.core.workflow.domain._
import org.openmole.core.workflow.sampling._
import org.openmole.plugin.method.evolution.Genome.{ GenomeBound, Suggestion }
import org.openmole.plugin.method.evolution.Objective.{ ToExactObjective, ToNoisyObjective }
import org.openmole.tool.types.ToDouble
import squants.time.Time

import scala.language.higherKinds
import scala.reflect.ClassTag

object OSE {

  case class DeterministicParams(
    mu:                  Int,
    origin:              (Vector[Double], Vector[Int]) ⇒ Vector[Int],
    limit:               Vector[Double],
    genome:              Genome,
    objectives:          Seq[ExactObjective[_]],
    operatorExploration: Double,
    reject:              Option[Condition])

  object DeterministicParams {

    import cats.data._
    import mgo.evolution.algorithm.OSE._
    import mgo.evolution.algorithm.{ OSE ⇒ MGOOSE, _ }

    implicit def integration = new MGOAPI.Integration[DeterministicParams, (Vector[Double], Vector[Int]), Array[Any]] { api ⇒
      type G = CDGenome.Genome
      type I = CDGenome.DeterministicIndividual.Individual[Array[Any]]
      type S = OSEState[Array[Any]]

      def iManifest = implicitly
      def gManifest = implicitly
      def sManifest = implicitly

      def operations(om: DeterministicParams) = new Ops {
        def startTimeLens = GenLens[S](_.startTime)
        def generationLens = GenLens[S](_.generation)

        def genomeValues(genome: G) = MGOAPI.paired(CDGenome.continuousValues.get _, CDGenome.discreteValues.get _)(genome)

        def buildGenome(v: (Vector[Double], Vector[Int])): G = CDGenome.buildGenome(v._1, None, v._2, None)
        def buildGenome(vs: Vector[Variable[_]]) = Genome.fromVariables(vs, om.genome).map(buildGenome)
        def buildIndividual(genome: G, phenotype: Array[Any], context: Context) = CDGenome.DeterministicIndividual.buildIndividual(genome, phenotype)

        def initialState = EvolutionState(s = (Array.empty, Array.empty))

        def afterGeneration(g: Long, s: S, population: Vector[I]): Boolean = mgo.evolution.stop.afterGeneration[S, I](g, EvolutionState.generation)(s, population)
        def afterDuration(d: Time, s: S, population: Vector[I]): Boolean = mgo.evolution.stop.afterDuration[S, I](d, EvolutionState.startTime)(s, population)

        def result(population: Vector[I], state: S) = FromContext { p ⇒
          import p._

          val res = MGOOSE.result[Array[Any]](state, Genome.continuous(om.genome).from(context), ExactObjective.toFitnessFunction(om.objectives))
          val genomes = GAIntegration.genomesOfPopulationToVariables(om.genome, res.map(_.continuous) zip res.map(_.discrete), scale = false).from(context)
          val fitness = GAIntegration.objectivesOfPopulationToVariables(om.objectives, res.map(_.fitness)).from(context)

          genomes ++ fitness
        }

        def initialGenomes(n: Int, rng: scala.util.Random) = FromContext { p ⇒
          import p._
          val continuous = Genome.continuous(om.genome).from(context)
          val discrete = Genome.discrete(om.genome).from(context)
          val rejectValue = om.reject.map(f ⇒ GAIntegration.rejectValue[G](f, om.genome, _.continuousValues.toVector, _.discreteValues.toVector).from(context))
          MGOOSE.initialGenomes(n, continuous, discrete, rejectValue, rng)
        }

        def breeding(individuals: Vector[I], n: Int, s: S, rng: scala.util.Random) = FromContext { p ⇒
          import p._
          val discrete = Genome.discrete(om.genome).from(context)
          val rejectValue = om.reject.map(f ⇒ GAIntegration.rejectValue[G](f, om.genome, _.continuousValues.toVector, _.discreteValues.toVector).from(context))
          MGOOSE.adaptiveBreeding[Array[Any]](
            n,
            om.operatorExploration,
            discrete,
            om.origin,
            ExactObjective.toFitnessFunction(om.objectives),
            rejectValue) apply (s, individuals, rng)
        }

        def elitism(population: Vector[I], candidates: Vector[I], s: S, rng: scala.util.Random) =
          Genome.continuous(om.genome).map { continuous ⇒
            val (s2, elited) = MGOOSE.elitism[Array[Any]](om.mu, om.limit, om.origin, continuous, ExactObjective.toFitnessFunction(om.objectives)) apply (s, population, candidates, rng)
            val s3 = EvolutionState.generation.modify(_ + 1)(s2)
            (s3, elited)
          }

        def migrateToIsland(population: Vector[I]) = population
        def migrateFromIsland(population: Vector[I], state: S) = population ++ state.s._1
      }

    }
  }

  case class StochasticParams(
    mu:                  Int,
    origin:              (Vector[Double], Vector[Int]) ⇒ Vector[Int],
    limit:               Vector[Double],
    genome:              Genome,
    objectives:          Seq[NoisyObjective[_]],
    historySize:         Int,
    cloneProbability:    Double,
    operatorExploration: Double,
    reject:              Option[Condition])

  object StochasticParams {

    import mgo.evolution.algorithm.NoisyOSE._
    import mgo.evolution.algorithm.{ NoisyOSE ⇒ MGONoisyOSE, _ }

    implicit def integration = new MGOAPI.Integration[StochasticParams, (Vector[Double], Vector[Int]), Array[Any]] { api ⇒
      type G = CDGenome.Genome
      type I = CDGenome.NoisyIndividual.Individual[Array[Any]]
      type S = OSEState[Array[Any]]

      def iManifest = implicitly
      def gManifest = implicitly
      def sManifest = implicitly

      def operations(om: StochasticParams) = new Ops {
        def afterGeneration(g: Long, s: S, population: Vector[I]): Boolean = mgo.evolution.stop.afterGeneration[S, I](g, EvolutionState.generation)(s, population)
        def afterDuration(d: Time, s: S, population: Vector[I]): Boolean = mgo.evolution.stop.afterDuration[S, I](d, EvolutionState.startTime)(s, population)

        def startTimeLens = GenLens[S](_.startTime)
        def generationLens = GenLens[S](_.generation)

        def genomeValues(genome: G) = MGOAPI.paired(CDGenome.continuousValues.get _, CDGenome.discreteValues.get _)(genome)
        def buildGenome(v: (Vector[Double], Vector[Int])): G = CDGenome.buildGenome(v._1, None, v._2, None)
        def buildGenome(vs: Vector[Variable[_]]) = Genome.fromVariables(vs, om.genome).map(buildGenome)

        def buildIndividual(genome: G, phenotype: Array[Any], context: Context) = CDGenome.NoisyIndividual.buildIndividual(genome, phenotype)

        def initialState = EvolutionState(s = (Array.empty, Array.empty))

        def result(population: Vector[I], state: S) = FromContext { p ⇒
          import org.openmole.core.context._
          import p._

          val res = MGONoisyOSE.result(state, population, NoisyObjective.aggregate(om.objectives), Genome.continuous(om.genome).from(context), om.limit)
          val genomes = GAIntegration.genomesOfPopulationToVariables(om.genome, res.map(_.continuous) zip res.map(_.discrete), scale = false).from(context)
          val fitness = GAIntegration.objectivesOfPopulationToVariables(om.objectives, res.map(_.fitness)).from(context)
          val samples = Variable(GAIntegration.samples.array, res.map(_.replications).toArray)

          genomes ++ fitness ++ Seq(samples)
        }

        def initialGenomes(n: Int, rng: scala.util.Random) = FromContext { p ⇒
          import p._
          val continuous = Genome.continuous(om.genome).from(context)
          val discrete = Genome.discrete(om.genome).from(context)
          val rejectValue = om.reject.map(f ⇒ GAIntegration.rejectValue[G](f, om.genome, _.continuousValues.toVector, _.discreteValues.toVector).from(context))
          MGONoisyOSE.initialGenomes(n, continuous, discrete, rejectValue, rng)
        }

        def breeding(individuals: Vector[I], n: Int, s: S, rng: scala.util.Random) = FromContext { p ⇒
          import p._
          val discrete = Genome.discrete(om.genome).from(context)
          val rejectValue = om.reject.map(f ⇒ GAIntegration.rejectValue[G](f, om.genome, _.continuousValues.toVector, _.discreteValues.toVector).from(context))

          MGONoisyOSE.adaptiveBreeding[Array[Any]](
            n,
            om.operatorExploration,
            om.cloneProbability,
            NoisyObjective.aggregate(om.objectives),
            discrete,
            om.origin,
            om.limit,
            rejectValue) apply (s, individuals, rng)
        }

        def elitism(population: Vector[I], candidates: Vector[I], s: S, rng: scala.util.Random) =
          Genome.continuous(om.genome).map { continuous ⇒
            val (s2, elited) =
              MGONoisyOSE.elitism[Array[Any]](
                om.mu,
                om.historySize,
                NoisyObjective.aggregate(om.objectives),
                continuous,
                om.origin,
                om.limit) apply (s, population, candidates, rng)
            val s3 = EvolutionState.generation.modify(_ + 1)(s2)
            (s3, elited)
          }

        def migrateToIsland(population: Vector[I]) = StochasticGAIntegration.migrateToIsland[I](population, CDGenome.NoisyIndividual.Individual.historyAge)
        def migrateFromIsland(population: Vector[I], state: S) = StochasticGAIntegration.migrateFromIsland[I, Array[Any]](population ++ state.s._1, CDGenome.NoisyIndividual.Individual.historyAge, CDGenome.NoisyIndividual.Individual.phenotypeHistory[Array[Any]])
      }

    }
  }

  import org.openmole.core.dsl._

  object OriginAxe {

    implicit def fromDoubleDomainToOriginAxe[D](f: Factor[D, Double])(implicit fix: Fix[D, Double]): OriginAxe = {
      val domain = fix(f.domain).toVector
      ContinuousOriginAxe(GenomeBound.ScalarDouble(f.value, domain.min, domain.max), domain)
    }

    implicit def fromSeqOfDoubleDomainToOriginAxe[D](f: Factor[D, Array[Double]])(implicit fix: Fix[D, Array[Double]]): OriginAxe = {
      val domain = fix(f.domain)
      ContinuousSequenceOriginAxe(
        GenomeBound.SequenceOfDouble(f.value, FromContext.value(domain.map(_.min).toArray), FromContext.value(domain.map(_.max).toArray), domain.size),
        domain.toVector.map(_.toVector))
    }

    implicit def fromIntDomainToPatternAxe[D](f: Factor[D, Int])(implicit fix: Fix[D, Int]): OriginAxe = {
      val domain = fix(f.domain).toVector
      DiscreteOriginAxe(GenomeBound.ScalarInt(f.value, domain.min, domain.max), domain)
    }

    implicit def fromSeqOfIntDomainToOriginAxe[D](f: Factor[D, Array[Int]])(implicit fix: Fix[D, Array[Int]]): OriginAxe = {
      val domain = fix(f.domain)
      DiscreteSequenceOriginAxe(
        GenomeBound.SequenceOfInt(f.value, FromContext.value(domain.map(_.min).toArray), FromContext.value(domain.map(_.max).toArray), domain.size),
        domain.toVector.map(_.toVector))
    }

    def genomeBound(originAxe: OriginAxe) = originAxe match {
      case c: ContinuousOriginAxe          ⇒ c.p
      case d: DiscreteOriginAxe            ⇒ d.p
      case cs: ContinuousSequenceOriginAxe ⇒ cs.p
      case ds: DiscreteSequenceOriginAxe   ⇒ ds.p
    }

    def fullGenome(origin: Seq[OriginAxe], genome: Genome): Genome =
      origin.map(genomeBound) ++ genome

    def toOrigin(origin: Seq[OriginAxe], genome: Genome) = {
      val fg = fullGenome(origin, genome)
      def grid(continuous: Vector[Double], discrete: Vector[Int]): Vector[Int] =
        origin.toVector.flatMap {
          case ContinuousOriginAxe(p, scale)         ⇒ Vector(mgo.tools.findInterval(scale, Genome.continuousValue(fg, p.v, continuous)))
          case DiscreteOriginAxe(p, scale)           ⇒ Vector(mgo.tools.findInterval(scale, Genome.discreteValue(fg, p.v, discrete)))
          case ContinuousSequenceOriginAxe(p, scale) ⇒ mgo.evolution.niche.irregularGrid[Double](scale)(Genome.continuousSequenceValue(fg, p.v, p.size, continuous))
          case DiscreteSequenceOriginAxe(p, scale)   ⇒ mgo.evolution.niche.irregularGrid[Int](scale)(Genome.discreteSequenceValue(fg, p.v, p.size, discrete))
        }

      grid(_, _)
    }

  }

  sealed trait OriginAxe
  case class ContinuousOriginAxe(p: Genome.GenomeBound.ScalarDouble, scale: Vector[Double]) extends OriginAxe
  case class ContinuousSequenceOriginAxe(p: Genome.GenomeBound.SequenceOfDouble, scale: Vector[Vector[Double]]) extends OriginAxe
  case class DiscreteOriginAxe(p: Genome.GenomeBound.ScalarInt, scale: Vector[Int]) extends OriginAxe
  case class DiscreteSequenceOriginAxe(p: Genome.GenomeBound.SequenceOfInt, scale: Vector[Vector[Int]]) extends OriginAxe

  object FitnessPattern {
    implicit def fromUnderExactToPattern[T, V](v: Under[T, V])(implicit td: ToDouble[V], te: ToExactObjective[T]) = FitnessPattern(te.apply(v.value), td(v.under))
    implicit def fromUnderNoisyToPattern[T, V](v: Under[T, V])(implicit td: ToDouble[V], te: ToNoisyObjective[T]) = FitnessPattern(te.apply(v.value), td(v.under))

    //    implicit def fromUnderToObjective[T](v: Under[Val[T], T])(implicit td: ToDouble[T]) = FitnessPattern(v.value, td(v.under))
    //    implicit def fromNegativeUnderToObjective[T](v: Under[Negative[Val[T]], T])(implicit td: ToDouble[T]) = FitnessPattern(v.value, td(v.under))

    //    implicit def fromAggregate[DT: ClassTag, T](v: Under[Aggregate[Val[DT], Array[DT] ⇒ Double], T])(implicit td: ToDouble[T]) = FitnessPattern(Objective.aggregateToObjective(v.value), td(v.under))
    //    implicit def fromNegativeAggregate[DT: ClassTag, T](v: Under[Aggregate[Negative[Val[DT]], Array[DT] ⇒ Double], T])(implicit td: ToDouble[T]) = FitnessPattern(v.value, td(v.under))

    def toLimit(f: Seq[FitnessPattern]) = f.toVector.map(_.limit)
    def toObjectives(f: Seq[FitnessPattern]) = f.map(_.objective)
  }

  case class FitnessPattern(objective: Objective[_], limit: Double)

  def apply(
    origin:     Seq[OriginAxe],
    objective:  Seq[FitnessPattern],
    genome:     Genome                       = Seq(),
    mu:         Int                          = 200,
    stochastic: OptionalArgument[Stochastic] = None,
    reject:     OptionalArgument[Condition]  = None): EvolutionWorkflow =
    WorkflowIntegration.stochasticity(objective.map(_.objective), stochastic.option) match {
      case None ⇒
        val exactObjectives = FitnessPattern.toObjectives(objective).map(o ⇒ Objective.toExact(o))
        val fg = OriginAxe.fullGenome(origin, genome)

        val integration: WorkflowIntegration.DeterministicGA[_] =
          WorkflowIntegration.DeterministicGA(
            DeterministicParams(
              mu = mu,
              origin = OriginAxe.toOrigin(origin, genome),
              genome = fg,
              objectives = exactObjectives,
              limit = FitnessPattern.toLimit(objective),
              operatorExploration = operatorExploration,
              reject = reject.option),
            fg,
            exactObjectives
          )

        WorkflowIntegration.DeterministicGA.toEvolutionWorkflow(integration)
      case Some(stochasticValue) ⇒
        val fg = OriginAxe.fullGenome(origin, genome)
        val noisyObjectives = FitnessPattern.toObjectives(objective).map(o ⇒ Objective.toNoisy(o))

        val integration: WorkflowIntegration.StochasticGA[_] =
          WorkflowIntegration.StochasticGA(
            StochasticParams(
              mu = mu,
              origin = OriginAxe.toOrigin(origin, genome),
              genome = fg,
              objectives = noisyObjectives,
              limit = FitnessPattern.toLimit(objective),
              operatorExploration = operatorExploration,
              historySize = stochasticValue.sample,
              cloneProbability = stochasticValue.reevaluate,
              reject = reject.option),
            fg,
            noisyObjectives,
            stochasticValue
          )(StochasticParams.integration)

        WorkflowIntegration.StochasticGA.toEvolutionWorkflow(integration)
    }

}

object OSEEvolution {

  import org.openmole.core.dsl._

  def apply(
    origin:       Seq[OSE.OriginAxe],
    objective:    Seq[OSE.FitnessPattern],
    evaluation:   DSL,
    termination:  OMTermination,
    mu:           Int                          = 200,
    genome:       Genome                       = Seq(),
    stochastic:   OptionalArgument[Stochastic] = None,
    reject:       OptionalArgument[Condition]  = None,
    parallelism:  Int                          = 1,
    distribution: EvolutionPattern             = SteadyState(),
    suggestion:   Suggestion                   = Suggestion.empty,
    scope:        DefinitionScope              = "ose") =
    EvolutionPattern.build(
      algorithm =
        OSE(
          origin = origin,
          genome = genome,
          objective = objective,
          stochastic = stochastic,
          mu = mu,
          reject = reject
        ),
      evaluation = evaluation,
      termination = termination,
      parallelism = parallelism,
      distribution = distribution,
      suggestion = suggestion(genome),
      scope = scope
    )

}
