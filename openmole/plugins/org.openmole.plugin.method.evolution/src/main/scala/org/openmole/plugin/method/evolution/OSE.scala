package org.openmole.plugin.method.evolution

import org.openmole.core.exception.UserBadDataError
import org.openmole.core.expansion.FromContext
import cats._
import cats.data._
import cats.implicits._
import mgo.evolution._
import mgo.evolution.algorithm._
import mgo.evolution.breeding._
import mgo.evolution.contexts._
import mgo.evolution.elitism._
import mgo.evolution.niche._
import mgo.tagtools._
import monocle.macros.GenLens
import org.openmole.core.context.{ Context, Variable }
import org.openmole.core.keyword.Under
import org.openmole.core.workflow.builder.{ DefinitionScope, ValueAssignment }
import org.openmole.core.workflow.domain._
import org.openmole.core.workflow.sampling._
import org.openmole.plugin.method.evolution.Genome.GenomeBound
import org.openmole.plugin.method.evolution.NichedNSGA2.NichedElement
import org.openmole.tool.types.ToDouble
import squants.time.Time

import scala.language.higherKinds

object OSE {

  case class DeterministicParams(
    mu:                  Int,
    origin:              (Vector[Double], Vector[Int]) ⇒ Vector[Int],
    limit:               Vector[Double],
    genome:              Genome,
    objectives:          Objectives,
    operatorExploration: Double)

  object DeterministicParams {

    import mgo.evolution.algorithm.{ OSE ⇒ MGOOSE, _ }
    import mgo.evolution.algorithm.OSE._
    import cats.data._
    import mgo.evolution.contexts._

    implicit def integration = new MGOAPI.Integration[DeterministicParams, (Vector[Double], Vector[Int]), Vector[Double]] { api ⇒
      type G = CDGenome.Genome
      type I = CDGenome.DeterministicIndividual.Individual
      type S = EvolutionState[OSEState]

      def iManifest = implicitly
      def gManifest = implicitly
      def sManifest = implicitly

      private def interpret[U](f: MGOOSE.OSEImplicits ⇒ (S, U)) = State[S, U] { (s: S) ⇒
        MGOOSE.run(s)(f)
      }

      private def zipWithState[M[_]: cats.Monad: StartTime: Random: Generation: ReachMap, T](op: M[T])(implicit archive: Archive[M, I]): M[(S, T)] = {
        import cats.implicits._
        for {
          t ← op
          newState ← MGOOSE.state[M]
        } yield (newState, t)
      }

      def afterGeneration(g: Long, population: Vector[I]): M[Boolean] = interpret { impl ⇒
        import impl._
        zipWithState(mgo.evolution.afterGeneration[DSL, I](g).run(population)).eval
      }

      def afterDuration(d: squants.Time, population: Vector[I]): M[Boolean] = interpret { impl ⇒
        import impl._
        zipWithState(mgo.evolution.afterDuration[DSL, I](d).run(population)).eval
      }

      def operations(om: DeterministicParams) = new Ops {

        def randomLens = GenLens[S](_.random)
        def startTimeLens = GenLens[S](_.startTime)
        def generationLens = GenLens[S](_.generation)

        def genomeValues(genome: G) = MGOAPI.paired(CDGenome.continuousValues.get _, CDGenome.discreteValues.get _)(genome)
        def buildGenome(v: (Vector[Double], Vector[Int])): G = CDGenome.buildGenome(v._1, None, v._2, None)
        def buildGenome(vs: Vector[Variable[_]]) = Genome.fromVariables(vs, om.genome).map(buildGenome)

        def buildIndividual(genome: G, phenotype: Vector[Double], context: Context) = CDGenome.DeterministicIndividual.buildIndividual(genome, phenotype)

        def initialState(rng: util.Random) = EvolutionState[OSEState](random = rng, s = (Array.empty, Array.empty))

        def afterGeneration(g: Long, population: Vector[I]) = api.afterGeneration(g, population)
        def afterDuration(d: squants.Time, population: Vector[I]) = api.afterDuration(d, population)

        def result(population: Vector[I], state: S) = FromContext { p ⇒
          import p._

          val res = MGOOSE.result(state, Genome.continuous(om.genome).from(context))
          val genomes = GAIntegration.genomesOfPopulationToVariables(om.genome, res.map(_.continuous) zip res.map(_.discrete), scale = false).from(context)
          val fitness = GAIntegration.objectivesOfPopulationToVariables(om.objectives, res.map(_.fitness)).from(context)

          genomes ++ fitness
        }

        def initialGenomes(n: Int) =
          (Genome.continuous(om.genome) map2 Genome.discrete(om.genome)) { (continuous, discrete) ⇒
            interpret { impl ⇒
              import impl._
              zipWithState(
                MGOOSE.initialGenomes[DSL](n, continuous, discrete)
              ).eval
            }
          }

        def breeding(individuals: Vector[I], n: Int) =
          Genome.discrete(om.genome).map { discrete ⇒
            interpret { impl ⇒
              import impl._
              zipWithState(
                MGOOSE.adaptiveBreeding[DSL](
                  n,
                  om.operatorExploration,
                  discrete,
                  om.origin).run(individuals)).eval
            }
          }

        def elitism(population: Vector[I], candidates: Vector[I]) =
          Genome.continuous(om.genome).map { continuous ⇒
            interpret { impl ⇒
              import impl._
              def step =
                for {
                  elited ← MGOOSE.elitism[DSL](om.mu, om.limit, om.origin, continuous) apply (population, candidates)
                  _ ← mgo.evolution.elitism.incrementGeneration[DSL]
                } yield elited

              zipWithState(step).eval
            }
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
    operatorExploration: Double)

  object StochasticParams {

    import mgo.evolution.algorithm._
    import mgo.evolution.algorithm.{ NoisyOSE ⇒ MGONoisyOSE, _ }
    import mgo.evolution.algorithm.NoisyOSE._
    import cats.data._
    import mgo.evolution.contexts._

    implicit def integration = new MGOAPI.Integration[StochasticParams, (Vector[Double], Vector[Int]), Vector[Any]] { api ⇒
      type G = CDGenome.Genome
      type I = CDGenome.NoisyIndividual.Individual[Vector[Any]]
      type S = EvolutionState[OSEState[Vector[Any]]]

      def iManifest = implicitly
      def gManifest = implicitly
      def sManifest = implicitly

      private def interpret[U](f: OSEImplicits[Vector[Any]] ⇒ (S, U)) = State[S, U] { (s: S) ⇒
        mgo.evolution.algorithm.NoisyOSE.run(s)(f)
      }

      private def zipWithState[M[_]: cats.Monad: StartTime: Random: Generation: ReachMap, T](op: M[T])(implicit archive: Archive[M, I]): M[(S, T)] = {
        import cats.implicits._
        for {
          t ← op
          newState ← MGONoisyOSE.state[M, Vector[Any]]
        } yield (newState, t)
      }

      def afterGeneration(g: Long, population: Vector[I]): M[Boolean] = interpret { impl ⇒
        import impl._
        zipWithState(mgo.evolution.afterGeneration[DSL, I](g).run(population)).eval
      }

      def afterDuration(d: squants.Time, population: Vector[I]): M[Boolean] = interpret { impl ⇒
        import impl._
        zipWithState(mgo.evolution.afterDuration[DSL, I](d).run(population)).eval
      }

      def operations(om: StochasticParams) = new Ops {
        def randomLens = GenLens[S](_.random)
        def startTimeLens = GenLens[S](_.startTime)
        def generationLens = GenLens[S](_.generation)

        def genomeValues(genome: G) = MGOAPI.paired(CDGenome.continuousValues.get _, CDGenome.discreteValues.get _)(genome)
        def buildGenome(v: (Vector[Double], Vector[Int])): G = CDGenome.buildGenome(v._1, None, v._2, None)
        def buildGenome(vs: Vector[Variable[_]]) = Genome.fromVariables(vs, om.genome).map(buildGenome)

        def buildIndividual(genome: G, phenotype: Vector[Any], context: Context) = CDGenome.NoisyIndividual.buildIndividual(genome, phenotype)
        def initialState(rng: util.Random) = EvolutionState[OSEState[Vector[Any]]](random = rng, s = (Array.empty, Array.empty))

        def result(population: Vector[I], state: S) = FromContext { p ⇒
          import p._
          import org.openmole.core.context._

          val res = MGONoisyOSE.result(state, population, NoisyObjective.aggregate(om.objectives), Genome.continuous(om.genome).from(context), om.limit)
          val genomes = GAIntegration.genomesOfPopulationToVariables(om.genome, res.map(_.continuous) zip res.map(_.discrete), scale = false).from(context)
          val fitness = GAIntegration.objectivesOfPopulationToVariables(om.objectives, res.map(_.fitness)).from(context)
          val samples = Variable(GAIntegration.samples.array, res.map(_.replications).toArray)

          genomes ++ fitness ++ Seq(samples)
        }

        def initialGenomes(n: Int) =
          (Genome.continuous(om.genome) map2 Genome.discrete(om.genome)) { (continuous, discrete) ⇒
            interpret { impl ⇒
              import impl._
              zipWithState(MGONoisyOSE.initialGenomes[DSL](n, continuous, discrete)).eval
            }
          }

        def breeding(individuals: Vector[I], n: Int) =
          Genome.discrete(om.genome).map { discrete ⇒
            interpret { impl ⇒
              import impl._
              zipWithState(MGONoisyOSE.adaptiveBreeding[DSL, Vector[Any]](
                n,
                om.operatorExploration,
                om.cloneProbability,
                NoisyObjective.aggregate(om.objectives),
                discrete,
                om.origin,
                om.limit).run(individuals)).eval
            }
          }

        def elitism(population: Vector[I], candidates: Vector[I]) =
          Genome.continuous(om.genome).map { continuous ⇒
            interpret { impl ⇒
              import impl._
              def step =
                for {
                  elited ← MGONoisyOSE.elitism[DSL, Vector[Any]](
                    om.mu,
                    om.historySize,
                    NoisyObjective.aggregate(om.objectives),
                    continuous,
                    om.origin,
                    om.limit) apply (population, candidates)
                  _ ← mgo.evolution.elitism.incrementGeneration[DSL]
                } yield elited

              zipWithState(step).eval
            }
          }

        def afterGeneration(g: Long, population: Vector[I]) = api.afterGeneration(g, population)
        def afterDuration(d: squants.Time, population: Vector[I]) = api.afterDuration(d, population)
        def migrateToIsland(population: Vector[I]) = StochasticGAIntegration.migrateToIsland(population)
        def migrateFromIsland(population: Vector[I], state: S) = StochasticGAIntegration.migrateFromIsland(population ++ state.s._1)

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
    implicit def fromUnderToObjective[T](v: Under[Val[T], T])(implicit td: ToDouble[T]) = FitnessPattern(v.value, td(v.under))
    implicit def fromAggregate[DT, T](v: Under[Aggregate[Val[DT], Vector[DT] ⇒ Double], T])(implicit td: ToDouble[T]) = FitnessPattern(Objective.aggregateToObjective(v.value), td(v.under))

    def toLimit(f: Seq[FitnessPattern]) = f.toVector.map(_.limit)
    def toObjectives(f: Seq[FitnessPattern]) = f.map(_.objective)
  }

  case class FitnessPattern(objective: Objective[_], limit: Double)

  def apply(
    origin:     Seq[OriginAxe],
    objectives: Seq[FitnessPattern],
    genome:     Genome                       = Seq(),
    mu:         Int                          = 200,
    stochastic: OptionalArgument[Stochastic] = None): EvolutionWorkflow =
    WorkflowIntegration.stochasticity(objectives.map(_.objective), stochastic.option) match {
      case None ⇒
        val exactObjectives = FitnessPattern.toObjectives(objectives).map(o ⇒ Objective.toExact(o))
        val fg = OriginAxe.fullGenome(origin, genome)

        val integration: WorkflowIntegration.DeterministicGA[_] =
          WorkflowIntegration.DeterministicGA(
            DeterministicParams(
              mu = mu,
              origin = OriginAxe.toOrigin(origin, genome),
              genome = fg,
              objectives = exactObjectives,
              limit = FitnessPattern.toLimit(objectives),
              operatorExploration = operatorExploration),
            fg,
            exactObjectives
          )

        WorkflowIntegration.DeterministicGA.toEvolutionWorkflow(integration)
      case Some(stochasticValue) ⇒
        val fg = OriginAxe.fullGenome(origin, genome)
        val noisyObjectives = FitnessPattern.toObjectives(objectives).map(o ⇒ Objective.toNoisy(o))

        val integration: WorkflowIntegration.StochasticGA[_] =
          WorkflowIntegration.StochasticGA(
            StochasticParams(
              mu = mu,
              origin = OriginAxe.toOrigin(origin, genome),
              genome = fg,
              objectives = noisyObjectives,
              limit = FitnessPattern.toLimit(objectives),
              operatorExploration = operatorExploration,
              historySize = stochasticValue.replications,
              cloneProbability = stochasticValue.reevaluate),
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
    objectives:   Seq[OSE.FitnessPattern],
    evaluation:   DSL,
    termination:  OMTermination,
    mu:           Int                          = 200,
    genome:       Genome                       = Seq(),
    stochastic:   OptionalArgument[Stochastic] = None,
    parallelism:  Int                          = 1,
    distribution: EvolutionPattern             = SteadyState(),
    suggestion:   Seq[Seq[ValueAssignment[_]]] = Seq(),
    scope:        DefinitionScope              = "ose") =
    EvolutionPattern.build(
      algorithm =
        OSE(
          origin = origin,
          genome = genome,
          objectives = objectives,
          stochastic = stochastic,
          mu = mu
        ),
      evaluation = evaluation,
      termination = termination,
      stochastic = stochastic,
      parallelism = parallelism,
      distribution = distribution,
      suggestion = suggestion,
      scope = scope
    )

}
