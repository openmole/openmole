package org.openmole.plugin.method.evolution

import mgo.algorithm._
import org.openmole.core.context.{ Context, Val, Variable }
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.expansion.FromContext
import org.openmole.core.workflow.dsl._
import cats._
import cats.implicits._
import mgo.niche._
import monocle.macros._
import org.openmole.core.workflow.sampling._
import org.openmole.core.workflow.domain._
import org.openmole.plugin.method.evolution.NichedNSGA2.NichedElement

object NichedNSGA2Algorithm {

  import monocle.macros.{ GenLens, Lenses }

  import scala.language.higherKinds
  import mgo._
  import contexts._
  import mgo.niche._
  import mgo.breeding._
  import mgo.elitism._
  import mgo.ranking._
  import mgo.tools._
  import GenomeVectorDouble._
  import cats.data._
  import cats.implicits._
  import freedsl.tool._
  import mgo.niche
  import shapeless._
  import CDGenome._
  import DeterministicIndividual._

  case class Result[N](continuous: Vector[Double], discrete: Vector[Int], fitness: Vector[Double], niche: N)

  def result[N](population: Vector[Individual], niche: Individual ⇒ N, continuous: Vector[C]) =
    nicheElitism[Id, Individual, N](population, keepFirstFront(_, vectorFitness.get), niche).map { i ⇒
      Result(
        scaleContinuousValues(continuousValues.get(i.genome), continuous),
        Individual.genome composeLens discreteValues get i,
        i.fitness.toVector,
        niche(i))
    }

  def continuousProfile(x: Int, nX: Int): Niche[Individual, Int] =
    mgo.niche.continuousProfile[Individual]((Individual.genome composeLens continuousValues).get _, x, nX)

  def discreteProfile(x: Int): Niche[Individual, Int] =
    mgo.niche.discreteProfile[Individual]((Individual.genome composeLens discreteValues).get _, x)

  def boundedContinuousProfile(continuous: Vector[C], x: Int, nX: Int, min: Double, max: Double): Niche[Individual, Int] =
    mgo.niche.boundedContinuousProfile[Individual](i ⇒ scaleContinuousValues(continuousValues.get(i.genome), continuous), x, nX, min, max)

  def gridContinuousProfile(continuous: Vector[C], x: Int, intervals: Vector[Double]): Niche[Individual, Int] =
    mgo.niche.gridContinuousProfile[Individual](i ⇒ scaleContinuousValues(continuousValues.get(i.genome), continuous), x, intervals)

  def boundedObjectiveProfile(x: Int, nX: Int, min: Double, max: Double): Niche[Individual, Int] =
    mgo.niche.boundedContinuousProfile[Individual](vectorFitness.get _, x, nX, min, max)

  def gridObjectiveProfile(x: Int, intervals: Vector[Double]): Niche[Individual, Int] =
    mgo.niche.gridContinuousProfile[Individual](vectorFitness.get _, x, intervals)

  def initialGenomes[M[_]: cats.Monad: Random](lambda: Int, continuous: Vector[C], discrete: Vector[D]) =
    CDGenome.initialGenomes[M](lambda, continuous, discrete)

  def adaptiveBreeding[M[_]: Generation: Random: cats.Monad](lambda: Int, operatorExploration: Double, discrete: Vector[D]): Breeding[M, Individual, Genome] =
    NSGA2Operations.adaptiveBreeding[M, Individual, Genome](
      vectorFitness.get,
      Individual.genome.get,
      continuousValues.get,
      continuousOperator.get,
      discreteValues.get,
      discreteOperator.get,
      discrete,
      buildGenome,
      logOfPopulationSize,
      lambda,
      operatorExploration)

  def expression(fitness: (Vector[Double], Vector[Int]) ⇒ Vector[Double], components: Vector[C]): Genome ⇒ Individual =
    DeterministicIndividual.expression(fitness, components)

  def elitism[M[_]: cats.Monad: Random: Generation, N](niche: Niche[Individual, N], mu: Int, components: Vector[C]): Elitism[M, Individual] =
    ProfileOperations.elitism[M, Individual, N](
      vectorFitness.get,
      i ⇒ values(Individual.genome.get(i), components),
      niche,
      mu)

  def state[M[_]: cats.Monad: StartTime: Random: Generation] = mgo.algorithm.state[M, Unit](())

  def run[T](rng: util.Random)(f: contexts.run.Implicits ⇒ T): T = contexts.run(rng)(f)
  def run[T](state: EvolutionState[Unit])(f: contexts.run.Implicits ⇒ T): T = contexts.run(state)(f)

}

object NoisyNichedNSGA2Algorithm {

  import mgo._
  import mgo.breeding._
  import mgo.elitism._
  import mgo.diversity._
  import monocle.macros.{ GenLens, Lenses }
  import mgo.niche._
  import mgo.contexts._
  import mgo.ranking._
  import GenomeVectorDouble._
  import cats.data._
  import cats.implicits._
  import shapeless._
  import freedsl.tool._
  import mgo.niche
  import mgo.tools._
  import CDGenome._
  import NoisyIndividual._

  def aggregatedFitness[N](aggregation: Vector[Vector[Double]] ⇒ Vector[Double]) =
    NoisyNSGA2Operations.aggregated(vectorFitness.get, aggregation)(_)

  case class Result[N](continuous: Vector[Double], discrete: Vector[Int], fitness: Vector[Double], niche: N, replications: Int)

  def result[N](
    population:  Vector[Individual],
    aggregation: Vector[Vector[Double]] ⇒ Vector[Double],
    niche:       Individual ⇒ N,
    continuous:  Vector[C]) = {
    def nicheResult(population: Vector[Individual]) =
      keepFirstFront(population, aggregatedFitness(aggregation))

    nicheElitism[Id, Individual, N](population, nicheResult, niche).map { i ⇒
      val (c, d, f, r) = NoisyIndividual.aggregate(i, aggregation, continuous)
      Result(c, d, f, niche(i), r)
    }
  }

  def continuousProfile(x: Int, nX: Int): Niche[Individual, Int] =
    mgo.niche.continuousProfile[Individual]((Individual.genome composeLens continuousValues).get _, x, nX)

  def discreteProfile(x: Int): Niche[Individual, Int] =
    mgo.niche.discreteProfile[Individual]((Individual.genome composeLens discreteValues).get _, x)

  def boundedContinuousProfile(continuous: Vector[C], x: Int, nX: Int, min: Double, max: Double): Niche[Individual, Int] =
    mgo.niche.boundedContinuousProfile[Individual](i ⇒ scaleContinuousValues(continuousValues.get(i.genome), continuous), x, nX, min, max)

  def gridContinuousProfile(continuous: Vector[C], x: Int, intervals: Vector[Double]): Niche[Individual, Int] =
    mgo.niche.gridContinuousProfile[Individual](i ⇒ scaleContinuousValues(continuousValues.get(i.genome), continuous), x, intervals)

  def boundedObjectiveProfile(aggregation: Vector[Vector[Double]] ⇒ Vector[Double], x: Int, nX: Int, min: Double, max: Double): Niche[Individual, Int] =
    mgo.niche.boundedContinuousProfile[Individual](aggregatedFitness(aggregation), x, nX, min, max)

  def gridObjectiveProfile(aggregation: Vector[Vector[Double]] ⇒ Vector[Double], x: Int, intervals: Vector[Double]): Niche[Individual, Int] =
    mgo.niche.gridContinuousProfile[Individual](aggregatedFitness(aggregation), x, intervals)

  def adaptiveBreeding[M[_]: cats.Monad: Random: Generation](lambda: Int, operatorExploration: Double, cloneProbability: Double, aggregation: Vector[Vector[Double]] ⇒ Vector[Double], discrete: Vector[D]): Breeding[M, Individual, Genome] =
    NoisyNSGA2Operations.adaptiveBreeding[M, Individual, Genome](
      vectorFitness.get,
      aggregation,
      Individual.genome.get,
      continuousValues.get,
      continuousOperator.get,
      discreteValues.get,
      discreteOperator.get,
      discrete,
      buildGenome,
      logOfPopulationSize,
      lambda,
      operatorExploration,
      cloneProbability)

  def elitism[M[_]: cats.Monad: Random: Generation, N](niche: Niche[Individual, N], muByNiche: Int, historySize: Int, aggregation: Vector[Vector[Double]] ⇒ Vector[Double], components: Vector[C]): Elitism[M, Individual] =
    NoisyProfileOperations.elitism[M, Individual, N](
      vectorFitness,
      aggregation,
      i ⇒ values(Individual.genome.get(i), components),
      Individual.historyAge,
      historySize,
      niche,
      muByNiche)

  def expression(fitness: (util.Random, Vector[Double], Vector[Int]) ⇒ Vector[Double], continuous: Vector[C]): (util.Random, Genome) ⇒ Individual =
    NoisyIndividual.expression(fitness, continuous)

  def initialGenomes[M[_]: cats.Monad: Random](lambda: Int, continuous: Vector[C], discrete: Vector[D]) =
    CDGenome.initialGenomes[M](lambda, continuous, discrete)

  def state[M[_]: cats.Monad: StartTime: Random: Generation] = mgo.algorithm.state[M, Unit](())

  def run[T](rng: util.Random)(f: contexts.run.Implicits ⇒ T): T = contexts.run(rng)(f)
  def run[T](state: EvolutionState[Unit])(f: contexts.run.Implicits ⇒ T): T = contexts.run(state)(f)

}

object NichedNSGA2 {

  object NichedElement {
    implicit def fromValDouble(v: (Val[Double], Int)) = Continuous(v._1, v._2)
    implicit def fromValInt(v: Val[Int]) = Discrete(v)
    implicit def fromValString(v: Val[String]) = Discrete(v)
    implicit def fromDoubleDomainToPatternAxe[D](f: Factor[D, Double])(implicit fix: Fix[D, Double]) = GridContinuous(f.prototype, fix(f.domain).toVector)

    case class GridContinuous(v: Val[Double], intervals: Vector[Double]) extends NichedElement
    case class Continuous(v: Val[Double], n: Int) extends NichedElement
    case class ContinuousSequence(v: Val[Array[Double]], i: Int, n: Int) extends NichedElement
    case class Discrete(v: Val[_]) extends NichedElement
    case class DiscreteSequence(v: Val[Array[_]], i: Int) extends NichedElement
  }

  sealed trait NichedElement

  object DeterministicParams {

    def niche(genome: Genome, objectives: Objectives, profiled: Seq[NichedElement]) = {

      def notFoundInGenome(v: Val[_]) = throw new UserBadDataError(s"Variable $v not found in the genome")

      val niches =
        profiled.toVector.map {
          case c: NichedElement.Continuous ⇒
            val index = Genome.continuousIndex(genome, c.v).getOrElse(notFoundInGenome(c.v))
            NichedNSGA2Algorithm.continuousProfile(index, c.n).pure[FromContext]
          case c: NichedElement.ContinuousSequence ⇒
            val index = Genome.continuousIndex(genome, c.v).getOrElse(notFoundInGenome(c.v))
            NichedNSGA2Algorithm.continuousProfile(index + c.i, c.n).pure[FromContext]
          case c: NichedElement.Discrete ⇒
            val index = Genome.discreteIndex(genome, c.v).getOrElse(notFoundInGenome(c.v))
            NichedNSGA2Algorithm.discreteProfile(index).pure[FromContext]
          case c: NichedElement.DiscreteSequence ⇒
            val index = Genome.discreteIndex(genome, c.v).getOrElse(notFoundInGenome(c.v))
            NichedNSGA2Algorithm.discreteProfile(index + c.i).pure[FromContext]
          case c: NichedElement.GridContinuous ⇒ FromContext { p ⇒
            import p._
            (Genome.continuousIndex(genome, c.v), Objective.index(objectives, c.v)) match {
              case (Some(index), _) ⇒ NichedNSGA2Algorithm.gridContinuousProfile(Genome.continuous(genome).from(context), index, c.intervals)
              case (_, Some(index)) ⇒ NichedNSGA2Algorithm.gridObjectiveProfile(index, c.intervals)
              case _                ⇒ throw new UserBadDataError(s"Variable ${c.v} not found neither in the genome nor in the objectives")
            }

          }
        }.sequence

      niches.map { ns ⇒ mgo.niche.sequenceNiches[CDGenome.DeterministicIndividual.Individual, Int](ns) }
    }

    import CDGenome.DeterministicIndividual
    import cats.data._
    import freedsl.dsl._
    import mgo.contexts._

    implicit def integration = new MGOAPI.Integration[DeterministicParams, (Vector[Double], Vector[Int]), Vector[Double]] {
      type G = CDGenome.Genome
      type I = DeterministicIndividual.Individual
      type S = EvolutionState[Unit]

      def iManifest = implicitly
      def gManifest = implicitly
      def sManifest = implicitly

      private def interpret[U](f: mgo.contexts.run.Implicits ⇒ (S, U)) = State[S, U] { (s: S) ⇒
        mgo.algorithm.Profile.run(s)(f)
      }

      private def zipWithState[M[_]: cats.Monad: StartTime: Random: Generation, T](op: M[T]): M[(S, T)] = {
        import cats.implicits._
        for {
          t ← op
          newState ← mgo.algorithm.Profile.state[M]
        } yield (newState, t)
      }

      def operations(om: DeterministicParams) = new Ops {
        def randomLens = GenLens[S](_.random)
        def startTimeLens = GenLens[S](_.startTime)
        def generation(s: S) = s.generation
        def genomeValues(genome: G) = MGOAPI.paired(CDGenome.continuousValues.get _, CDGenome.discreteValues.get _)(genome)
        def buildIndividual(genome: G, phenotype: Vector[Double], context: Context) = CDGenome.DeterministicIndividual.buildIndividual(genome, phenotype)
        def initialState(rng: util.Random) = EvolutionState[Unit](random = rng, s = ())

        def result(population: Vector[I], state: S) = FromContext { p ⇒
          import p._

          val res = NichedNSGA2Algorithm.result(population, om.niche.from(context), Genome.continuous(om.genome).from(context))
          val genomes = GAIntegration.genomesOfPopulationToVariables(om.genome, res.map(_.continuous) zip res.map(_.discrete), scale = false).from(context)
          val fitness = GAIntegration.objectivesOfPopulationToVariables(om.objectives, res.map(_.fitness)).from(context)

          genomes ++ fitness
        }

        def initialGenomes(n: Int) =
          (Genome.continuous(om.genome) map2 Genome.discrete(om.genome)) { (continuous, discrete) ⇒
            interpret { impl ⇒
              import impl._
              zipWithState(mgo.algorithm.Profile.initialGenomes[DSL](n, continuous, discrete)).eval
            }
          }

        def breeding(population: Vector[I], n: Int) =
          Genome.discrete(om.genome).map { discrete ⇒
            interpret { impl ⇒
              import impl._
              zipWithState(mgo.algorithm.Profile.adaptiveBreeding[DSL](n, om.operatorExploration, discrete).run(population)).eval
            }
          }

        def elitism(population: Vector[I]) = FromContext { p ⇒
          import p._
          interpret { impl ⇒
            import impl._
            def step =
              for {
                elited ← NichedNSGA2Algorithm.elitism[DSL, Vector[Int]](om.niche.from(context), om.nicheSize, Genome.continuous(om.genome).from(context)) apply population
                _ ← mgo.elitism.incrementGeneration[DSL]
              } yield elited

            zipWithState(step).eval
          }
        }

        def afterGeneration(g: Long, population: Vector[I]): M[Boolean] = interpret { impl ⇒
          import impl._
          zipWithState(mgo.afterGeneration[DSL, I](g).run(population)).eval
        }

        def afterDuration(d: squants.Time, population: Vector[I]): M[Boolean] = interpret { impl ⇒
          import impl._
          zipWithState(mgo.afterDuration[DSL, I](d).run(population)).eval
        }

        def migrateToIsland(population: Vector[I]) = DeterministicGAIntegration.migrateToIsland(population)
        def migrateFromIsland(population: Vector[I], state: S) = population
      }

    }
  }

  case class DeterministicParams(
    nicheSize:           Int,
    niche:               FromContext[Niche[CDGenome.DeterministicIndividual.Individual, Vector[Int]]],
    genome:              Genome,
    objectives:          Objectives,
    operatorExploration: Double)

  object StochasticParams {
    import cats.data._
    import freedsl.dsl._
    import mgo.contexts._

    def niche(genome: Genome, objectives: Objectives, aggregation: Vector[Vector[Double]] ⇒ Vector[Double], profiled: Seq[NichedElement]) = {

      def notFoundInGenome(v: Val[_]) = throw new UserBadDataError(s"Variable $v not found in the genome")

      val niches =
        profiled.toVector.map {
          case c: NichedElement.Continuous ⇒
            val index = Genome.continuousIndex(genome, c.v).getOrElse(notFoundInGenome(c.v))
            NoisyNichedNSGA2Algorithm.continuousProfile(index, c.n).pure[FromContext]
          case c: NichedElement.ContinuousSequence ⇒
            val index = Genome.continuousIndex(genome, c.v).getOrElse(notFoundInGenome(c.v))
            NoisyNichedNSGA2Algorithm.continuousProfile(index + c.i, c.n).pure[FromContext]
          case c: NichedElement.Discrete ⇒
            val index = Genome.discreteIndex(genome, c.v).getOrElse(notFoundInGenome(c.v))
            NoisyNichedNSGA2Algorithm.discreteProfile(index).pure[FromContext]
          case c: NichedElement.DiscreteSequence ⇒
            val index = Genome.discreteIndex(genome, c.v).getOrElse(notFoundInGenome(c.v))
            NoisyNichedNSGA2Algorithm.discreteProfile(index + c.i).pure[FromContext]
          case c: NichedElement.GridContinuous ⇒ FromContext { p ⇒
            import p._
            (Genome.continuousIndex(genome, c.v), Objective.index(objectives, c.v)) match {
              case (Some(index), _) ⇒ NoisyNichedNSGA2Algorithm.gridContinuousProfile(Genome.continuous(genome).from(context), index, c.intervals)
              case (_, Some(index)) ⇒ NoisyNichedNSGA2Algorithm.gridObjectiveProfile(aggregation, index, c.intervals)
              case _                ⇒ throw new UserBadDataError(s"Variable ${c.v} not found neither in the genome nor in the objectives")
            }

          }
        }.sequence

      niches.map { ns ⇒ mgo.niche.sequenceNiches[CDGenome.NoisyIndividual.Individual, Int](ns) }
    }

    implicit def integration = new MGOAPI.Integration[StochasticParams, (Vector[Double], Vector[Int]), Vector[Double]] {
      type G = CDGenome.Genome
      type I = CDGenome.NoisyIndividual.Individual
      type S = EvolutionState[Unit]

      def iManifest = implicitly
      def gManifest = implicitly
      def sManifest = implicitly

      private def interpret[U](f: mgo.contexts.run.Implicits ⇒ (S, U)) = State[S, U] { (s: S) ⇒
        mgo.algorithm.NoisyProfile.run(s)(f)
      }

      private def zipWithState[M[_]: cats.Monad: StartTime: Random: Generation, T](op: M[T]): M[(S, T)] = {
        import cats.implicits._
        for {
          t ← op
          newState ← mgo.algorithm.NoisyProfile.state[M]
        } yield (newState, t)
      }

      def operations(om: StochasticParams) = new Ops {
        def randomLens = GenLens[S](_.random)
        def startTimeLens = GenLens[S](_.startTime)
        def generation(s: S) = s.generation
        def genomeValues(genome: G) = MGOAPI.paired(CDGenome.continuousValues.get _, CDGenome.discreteValues.get _)(genome)
        def buildIndividual(genome: G, phenotype: Vector[Double], context: Context) = CDGenome.NoisyIndividual.buildIndividual(genome, phenotype)
        def initialState(rng: util.Random) = EvolutionState[Unit](random = rng, s = ())

        def result(population: Vector[I], state: S) = FromContext { p ⇒
          import p._

          val res = NoisyNichedNSGA2Algorithm.result(population, om.aggregation, om.niche.from(context), Genome.continuous(om.genome).from(context))
          val genomes = GAIntegration.genomesOfPopulationToVariables(om.genome, res.map(_.continuous) zip res.map(_.discrete), scale = false).from(context)
          val fitness = GAIntegration.objectivesOfPopulationToVariables(om.objectives, res.map(_.fitness)).from(context)
          val samples = Variable(GAIntegration.samples.array, res.map(_.replications).toArray)

          genomes ++ fitness ++ Seq(samples)
        }

        def initialGenomes(n: Int) =
          (Genome.continuous(om.genome) map2 Genome.discrete(om.genome)) { (continuous, discrete) ⇒
            interpret { impl ⇒
              import impl._
              zipWithState(NoisyNichedNSGA2Algorithm.initialGenomes[DSL](n, continuous, discrete)).eval
            }
          }

        def breeding(individuals: Vector[I], n: Int) =
          Genome.discrete(om.genome).map { discrete ⇒
            interpret { impl ⇒
              import impl._
              zipWithState(NoisyNichedNSGA2Algorithm.adaptiveBreeding[DSL](n, om.operatorExploration, om.cloneProbability, om.aggregation, discrete).run(individuals)).eval
            }
          }

        def elitism(individuals: Vector[I]) =
          FromContext { p ⇒
            import p._
            interpret { impl ⇒
              import impl._
              def step =
                for {
                  elited ← NoisyNichedNSGA2Algorithm.elitism[DSL, Vector[Int]](
                    om.niche.from(context),
                    om.nicheSize,
                    om.historySize,
                    om.aggregation,
                    Genome.continuous(om.genome).from(context)) apply individuals
                  _ ← mgo.elitism.incrementGeneration[DSL]
                } yield elited

              zipWithState(step).eval
            }
          }

        def afterGeneration(g: Long, population: Vector[I]): M[Boolean] = interpret { impl ⇒
          import impl._
          zipWithState(mgo.afterGeneration[DSL, I](g).run(population)).eval
        }

        def afterDuration(d: squants.Time, population: Vector[I]): M[Boolean] = interpret { impl ⇒
          import impl._
          zipWithState(mgo.afterDuration[DSL, I](d).run(population)).eval
        }

        def migrateToIsland(population: Vector[I]) = StochasticGAIntegration.migrateToIsland(population)
        def migrateFromIsland(population: Vector[I], state: S) = population
      }

    }
  }

  case class StochasticParams(
    nicheSize:           Int,
    niche:               FromContext[Niche[mgo.algorithm.CDGenome.NoisyIndividual.Individual, Vector[Int]]],
    operatorExploration: Double,
    genome:              Genome,
    objectives:          Objectives,
    historySize:         Int,
    cloneProbability:    Double,
    aggregation:         Vector[Vector[Double]] ⇒ Vector[Double])

  def apply(
    niche:      Seq[NichedElement],
    genome:     Genome,
    objectives: Objectives,
    nicheSize:  Int,
    stochastic: OptionalArgument[Stochastic] = None) =
    stochastic.option match {
      case None ⇒
        val integration: WorkflowIntegration.DeterministicGA[_] = new WorkflowIntegration.DeterministicGA(
          DeterministicParams(
            genome = genome,
            objectives = objectives,
            niche = DeterministicParams.niche(genome, objectives, niche),
            operatorExploration = operatorExploration,
            nicheSize = nicheSize
          ),
          genome,
          objectives
        )

        WorkflowIntegration.DeterministicGA.toEvolutionWorkflow(integration)
      case Some(stochastic) ⇒
        import org.openmole.core.dsl.seqIsFunctor

        val seqStochastic: Stochastic =
          Stochastic(
            seed = stochastic.seed,
            replications = stochastic.replications,
            reevaluate = stochastic.reevaluate,
            aggregation = stochastic.aggregation
          )

        def aggregation(h: Vector[Vector[Double]]) =
          StochasticGAIntegration.aggregateVector(seqStochastic.aggregation, h)

        val integration: WorkflowIntegration.StochasticGA[_] = WorkflowIntegration.StochasticGA(
          StochasticParams(
            nicheSize = nicheSize,
            niche = StochasticParams.niche(genome, objectives, aggregation, niche),
            operatorExploration = operatorExploration,
            genome = genome,
            objectives = objectives,
            historySize = stochastic.replications,
            cloneProbability = stochastic.reevaluate,
            aggregation = aggregation),
          genome,
          objectives,
          seqStochastic
        )

        WorkflowIntegration.StochasticGA.toEvolutionWorkflow(integration)
    }

}

object NichedNSGA2Evolution {

  import org.openmole.core.dsl._
  import org.openmole.core.workflow.puzzle._

  def apply(
    evaluation:   Puzzle,
    termination:  OMTermination,
    niche:        Seq[NichedElement],
    genome:       Genome,
    objectives:   Objectives,
    nicheSize:    Int,
    stochastic:   OptionalArgument[Stochastic] = None,
    parallelism:  Int                          = 1,
    distribution: EvolutionPattern             = SteadyState()) =
    EvolutionPattern.build(
      algorithm =
        NichedNSGA2(
          niche = niche,
          genome = genome,
          nicheSize = nicheSize,
          objectives = objectives,
          stochastic = stochastic
        ),
      evaluation = evaluation,
      termination = termination,
      stochastic = stochastic,
      parallelism = parallelism,
      distribution = distribution
    )

}

