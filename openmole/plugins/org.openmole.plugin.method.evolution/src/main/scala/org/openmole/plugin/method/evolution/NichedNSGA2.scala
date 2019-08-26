package org.openmole.plugin.method.evolution

import org.openmole.core.context.{ Context, Val, Variable }
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.expansion.FromContext
import org.openmole.core.workflow.tools.OptionalArgument
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
import org.openmole.core.workflow.builder.{ DefinitionScope, ValueAssignment }
import org.openmole.core.workflow.domain._
import org.openmole.core.workflow.sampling._
import org.openmole.plugin.method.evolution.Genome.Suggestion
import org.openmole.plugin.method.evolution.NichedNSGA2.NichedElement

import scala.language.higherKinds

object NichedNSGA2Algorithm {

  import CDGenome._
  import DeterministicIndividual._

  case class Result[N](continuous: Vector[Double], discrete: Vector[Int], fitness: Vector[Double], niche: N)

  def result[N, P](population: Vector[Individual[P]], niche: Individual[P] ⇒ N, continuous: Vector[C], fitness: P ⇒ Vector[Double]) = {
    nicheElitism[Id, Individual[P], N](population, keepFirstFront[Individual[P]](_, i ⇒ fitness(i.phenotype)), niche).map { i ⇒
      Result(
        scaleContinuousValues(continuousValues.get(i.genome), continuous),
        Individual.genome composeLens discreteValues get i,
        fitness(i.phenotype),
        niche(i))
    }
  }

  def continuousProfile[P](x: Int, nX: Int): Niche[Individual[P], Int] =
    mgo.evolution.niche.continuousProfile[Individual[P]]((Individual.genome composeLens continuousValues).get _, x, nX)

  def discreteProfile[P](x: Int): Niche[Individual[P], Int] =
    mgo.evolution.niche.discreteProfile[Individual[P]]((Individual.genome composeLens discreteValues).get _, x)

  def boundedContinuousProfile[P](continuous: Vector[C], x: Int, nX: Int, min: Double, max: Double): Niche[Individual[P], Int] =
    mgo.evolution.niche.boundedContinuousProfile[Individual[P]](i ⇒ scaleContinuousValues(continuousValues.get(i.genome), continuous), x, nX, min, max)

  def gridContinuousProfile[P](continuous: Vector[C], x: Int, intervals: Vector[Double]): Niche[Individual[P], Int] =
    mgo.evolution.niche.gridContinuousProfile[Individual[P]](i ⇒ scaleContinuousValues(continuousValues.get(i.genome), continuous), x, intervals)

  def boundedObjectiveProfile[P](x: Int, nX: Int, min: Double, max: Double, fitness: P ⇒ Vector[Double]): Niche[Individual[P], Int] =
    mgo.evolution.niche.boundedContinuousProfile[Individual[P]](i ⇒ fitness(i.phenotype), x, nX, min, max)

  def gridObjectiveProfile[P](x: Int, intervals: Vector[Double], fitness: P ⇒ Vector[Double]): Niche[Individual[P], Int] =
    mgo.evolution.niche.gridContinuousProfile[Individual[P]](i ⇒ fitness(i.phenotype), x, intervals)

  def initialGenomes[M[_]: cats.Monad: Random](lambda: Int, continuous: Vector[C], discrete: Vector[D]) =
    CDGenome.initialGenomes[M](lambda, continuous, discrete)

  def adaptiveBreeding[M[_]: Generation: Random: cats.Monad, P](lambda: Int, operatorExploration: Double, discrete: Vector[D], fitness: P ⇒ Vector[Double]): Breeding[M, Individual[P], Genome] =
    NSGA2Operations.adaptiveBreeding[M, Individual[P], Genome](
      i ⇒ fitness(i.phenotype),
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

  def expression[P](fitness: (Vector[Double], Vector[Int]) ⇒ P, components: Vector[C]): Genome ⇒ Individual[P] =
    DeterministicIndividual.expression[P](fitness, components)

  def elitism[M[_]: cats.Monad: Random: Generation, N, P](niche: Niche[Individual[P], N], mu: Int, components: Vector[C], fitness: P ⇒ Vector[Double]): Elitism[M, Individual[P]] =
    ProfileOperations.elitism[M, Individual[P], N](
      i ⇒ fitness(i.phenotype),
      i ⇒ values(Individual.genome.get(i), components),
      niche,
      mu)

  def state[M[_]: cats.Monad: StartTime: Random: Generation] = mgo.evolution.algorithm.state[M, Unit](())

  def run[T](rng: util.Random)(f: contexts.run.Implicits ⇒ T): T = contexts.run(rng)(f)
  def run[T](state: EvolutionState[Unit])(f: contexts.run.Implicits ⇒ T): T = contexts.run(state)(f)

}

object NoisyNichedNSGA2Algorithm {

  import CDGenome._
  import NoisyIndividual._
  import cats.implicits._
  import shapeless._

  def aggregatedFitness[P: Manifest](aggregation: Vector[P] ⇒ Vector[Double]): Individual[P] ⇒ Vector[Double] = NoisyNSGA2.fitness[P](aggregation)
  case class Result[N](continuous: Vector[Double], discrete: Vector[Int], fitness: Vector[Double], niche: N, replications: Int)

  def result[N, P: Manifest](
    population:  Vector[Individual[P]],
    aggregation: Vector[P] ⇒ Vector[Double],
    niche:       Individual[P] ⇒ N,
    continuous:  Vector[C],
    onlyOldest:  Boolean) = {
    def nicheResult(population: Vector[Individual[P]]) =
      if (population.isEmpty) population
      else if (onlyOldest) {
        val firstFront = keepFirstFront(population, NoisyNSGA2.fitness[P](aggregation))
        val sorted = firstFront.sortBy(-_.phenotypeHistory.size)
        val maxHistory = sorted.head.phenotypeHistory.size
        firstFront.filter(_.phenotypeHistory.size == maxHistory)
      }
      else keepFirstFront(population, NoisyNSGA2.fitness[P](aggregation))

    nicheElitism[Id, Individual[P], N](population, nicheResult, niche).map { i ⇒
      val (c, d, f, r) = NoisyIndividual.aggregate[P](i, aggregation, continuous)
      Result(c, d, f, niche(i), r)
    }
  }

  def continuousProfile[P](x: Int, nX: Int): Niche[Individual[P], Int] =
    mgo.evolution.niche.continuousProfile[Individual[P]]((Individual.genome composeLens continuousValues).get _, x, nX)

  def discreteProfile[P](x: Int): Niche[Individual[P], Int] =
    mgo.evolution.niche.discreteProfile[Individual[P]]((Individual.genome composeLens discreteValues).get _, x)

  def boundedContinuousProfile[P](continuous: Vector[C], x: Int, nX: Int, min: Double, max: Double): Niche[Individual[P], Int] =
    mgo.evolution.niche.boundedContinuousProfile[Individual[P]](i ⇒ scaleContinuousValues(continuousValues.get(i.genome), continuous), x, nX, min, max)

  def gridContinuousProfile[P](continuous: Vector[C], x: Int, intervals: Vector[Double]): Niche[Individual[P], Int] =
    mgo.evolution.niche.gridContinuousProfile[Individual[P]](i ⇒ scaleContinuousValues(continuousValues.get(i.genome), continuous), x, intervals)

  def boundedObjectiveProfile[P: Manifest](aggregation: Vector[P] ⇒ Vector[Double], x: Int, nX: Int, min: Double, max: Double): Niche[Individual[P], Int] =
    mgo.evolution.niche.boundedContinuousProfile[Individual[P]](aggregatedFitness[P](aggregation), x, nX, min, max)

  def gridObjectiveProfile[P: Manifest](aggregation: Vector[P] ⇒ Vector[Double], x: Int, intervals: Vector[Double]): Niche[Individual[P], Int] =
    mgo.evolution.niche.gridContinuousProfile[Individual[P]](aggregatedFitness(aggregation), x, intervals)

  def adaptiveBreeding[M[_]: cats.Monad: Random: Generation, P: Manifest](lambda: Int, operatorExploration: Double, cloneProbability: Double, aggregation: Vector[P] ⇒ Vector[Double], discrete: Vector[D]): Breeding[M, Individual[P], Genome] =
    NoisyNSGA2Operations.adaptiveBreeding[M, Individual[P], Genome, P](
      aggregatedFitness(aggregation),
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

  def elitism[M[_]: cats.Monad: Random: Generation, N, P: Manifest](niche: Niche[Individual[P], N], muByNiche: Int, historySize: Int, aggregation: Vector[P] ⇒ Vector[Double], components: Vector[C]): Elitism[M, Individual[P]] = {
    def individualValues(i: Individual[P]) = values(Individual.genome.get(i), components)

    NoisyProfileOperations.elitism[M, Individual[P], N, P](
      aggregatedFitness(aggregation),
      mergeHistories(individualValues, vectorPhenotype, Individual.historyAge, historySize),
      individualValues,
      niche,
      muByNiche)
  }

  def expression[P: Manifest](fitness: (util.Random, Vector[Double], Vector[Int]) ⇒ P, continuous: Vector[C]): (util.Random, Genome) ⇒ Individual[P] =
    NoisyIndividual.expression(fitness, continuous)

  def initialGenomes[M[_]: cats.Monad: Random](lambda: Int, continuous: Vector[C], discrete: Vector[D]) =
    CDGenome.initialGenomes[M](lambda, continuous, discrete)

  def state[M[_]: cats.Monad: StartTime: Random: Generation] = mgo.evolution.algorithm.state[M, Unit](())

  def run[T](rng: util.Random)(f: contexts.run.Implicits ⇒ T): T = contexts.run(rng)(f)
  def run[T](state: EvolutionState[Unit])(f: contexts.run.Implicits ⇒ T): T = contexts.run(state)(f)

}

object NichedNSGA2 {

  object NichedElement {
    implicit def fromValDouble(v: (Val[Double], Int)) = Continuous(v._1, v._2)
    implicit def fromValInt(v: Val[Int]) = Discrete(v)
    implicit def fromValString(v: Val[String]) = Discrete(v)
    implicit def fromDoubleDomainToPatternAxe[D](f: Factor[D, Double])(implicit fix: Fix[D, Double]) = GridContinuous(f.value, fix(f.domain).toVector)

    case class GridContinuous(v: Val[Double], intervals: Vector[Double]) extends NichedElement
    case class Continuous(v: Val[Double], n: Int) extends NichedElement
    case class ContinuousSequence(v: Val[Array[Double]], i: Int, n: Int) extends NichedElement
    case class Discrete(v: Val[_]) extends NichedElement
    case class DiscreteSequence(v: Val[Array[_]], i: Int) extends NichedElement
  }

  sealed trait NichedElement

  object DeterministicParams {

    def niche(genome: Genome, objectives: Seq[ExactObjective[_]], profiled: Seq[NichedElement]) = {

      def notFoundInGenome(v: Val[_]) = throw new UserBadDataError(s"Variable $v not found in the genome")

      val niches =
        profiled.toVector.map {
          case c: NichedElement.Continuous ⇒
            val index = Genome.continuousIndex(genome, c.v).getOrElse(notFoundInGenome(c.v))
            NichedNSGA2Algorithm.continuousProfile[Array[Any]](index, c.n).pure[FromContext]
          case c: NichedElement.ContinuousSequence ⇒
            val index = Genome.continuousIndex(genome, c.v).getOrElse(notFoundInGenome(c.v))
            NichedNSGA2Algorithm.continuousProfile[Array[Any]](index + c.i, c.n).pure[FromContext]
          case c: NichedElement.Discrete ⇒
            val index = Genome.discreteIndex(genome, c.v).getOrElse(notFoundInGenome(c.v))
            NichedNSGA2Algorithm.discreteProfile[Array[Any]](index).pure[FromContext]
          case c: NichedElement.DiscreteSequence ⇒
            val index = Genome.discreteIndex(genome, c.v).getOrElse(notFoundInGenome(c.v))
            NichedNSGA2Algorithm.discreteProfile[Array[Any]](index + c.i).pure[FromContext]
          case c: NichedElement.GridContinuous ⇒ FromContext { p ⇒
            import p._
            (Genome.continuousIndex(genome, c.v), Objective.index(objectives, c.v)) match {
              case (Some(index), _) ⇒ NichedNSGA2Algorithm.gridContinuousProfile[Array[Any]](Genome.continuous(genome).from(context), index, c.intervals)
              case (_, Some(index)) ⇒ NichedNSGA2Algorithm.gridObjectiveProfile[Array[Any]](index, c.intervals, ExactObjective.toFitnessFunction(objectives))
              case _                ⇒ throw new UserBadDataError(s"Variable ${c.v} not found neither in the genome nor in the objectives")
            }

          }
        }.sequence

      niches.map { ns ⇒ mgo.evolution.niche.sequenceNiches[CDGenome.DeterministicIndividual.Individual[Array[Any]], Int](ns) }
    }

    import CDGenome.DeterministicIndividual
    import cats.data._
    import mgo.evolution.contexts._

    implicit def integration = new MGOAPI.Integration[DeterministicParams, (Vector[Double], Vector[Int]), Array[Any]] {
      type G = CDGenome.Genome
      type I = DeterministicIndividual.Individual[Array[Any]]
      type S = EvolutionState[Unit]

      def iManifest = implicitly
      def gManifest = implicitly
      def sManifest = implicitly

      private def interpret[U](f: mgo.evolution.contexts.run.Implicits ⇒ (S, U)) = State[S, U] { (s: S) ⇒
        mgo.evolution.algorithm.Profile.run(s)(f)
      }

      private def zipWithState[M[_]: cats.Monad: StartTime: Random: Generation, T](op: M[T]): M[(S, T)] = {
        import cats.implicits._
        for {
          t ← op
          newState ← mgo.evolution.algorithm.Profile.state[M]
        } yield (newState, t)
      }

      def operations(om: DeterministicParams) = new Ops {
        def randomLens = GenLens[S](_.random)
        def startTimeLens = GenLens[S](_.startTime)
        def generationLens = GenLens[S](_.generation)

        def genomeValues(genome: G) = MGOAPI.paired(CDGenome.continuousValues.get _, CDGenome.discreteValues.get _)(genome)
        def buildGenome(v: (Vector[Double], Vector[Int])): G = CDGenome.buildGenome(v._1, None, v._2, None)
        def buildGenome(vs: Vector[Variable[_]]) = Genome.fromVariables(vs, om.genome).map(buildGenome)

        def buildIndividual(genome: G, phenotype: Array[Any], context: Context) = CDGenome.DeterministicIndividual.buildIndividual(genome, phenotype)
        def initialState(rng: util.Random) = EvolutionState[Unit](random = rng, s = ())

        def result(population: Vector[I], state: S) = FromContext { p ⇒
          import p._

          val res = NichedNSGA2Algorithm.result(population, om.niche.from(context), Genome.continuous(om.genome).from(context), ExactObjective.toFitnessFunction(om.objectives))
          val genomes = GAIntegration.genomesOfPopulationToVariables(om.genome, res.map(_.continuous) zip res.map(_.discrete), scale = false).from(context)
          val fitness = GAIntegration.objectivesOfPopulationToVariables(om.objectives, res.map(_.fitness)).from(context)

          genomes ++ fitness
        }

        def initialGenomes(n: Int) =
          (Genome.continuous(om.genome) map2 Genome.discrete(om.genome)) { (continuous, discrete) ⇒
            interpret { impl ⇒
              import impl._
              zipWithState(mgo.evolution.algorithm.Profile.initialGenomes[DSL](n, continuous, discrete)).eval
            }
          }

        def breeding(population: Vector[I], n: Int) =
          Genome.discrete(om.genome).map { discrete ⇒
            interpret { impl ⇒
              import impl._
              zipWithState(mgo.evolution.algorithm.Profile.adaptiveBreeding[DSL, Array[Any]](n, om.operatorExploration, discrete, ExactObjective.toFitnessFunction(om.objectives)).run(population)).eval
            }
          }

        def elitism(population: Vector[I], candidates: Vector[I]) = FromContext { p ⇒
          import p._
          interpret { impl ⇒
            import impl._
            def step =
              for {
                elited ← NichedNSGA2Algorithm.elitism[DSL, Vector[Int], Array[Any]](om.niche.from(context), om.nicheSize, Genome.continuous(om.genome).from(context), ExactObjective.toFitnessFunction((om.objectives))) apply (population, candidates)
                _ ← mgo.evolution.elitism.incrementGeneration[DSL]
              } yield elited

            zipWithState(step).eval
          }
        }

        def afterGeneration(g: Long, population: Vector[I]): M[Boolean] = interpret { impl ⇒
          import impl._
          zipWithState(mgo.evolution.afterGeneration[DSL, I](g).run(population)).eval
        }

        def afterDuration(d: squants.Time, population: Vector[I]): M[Boolean] = interpret { impl ⇒
          import impl._
          zipWithState(mgo.evolution.afterDuration[DSL, I](d).run(population)).eval
        }

        def migrateToIsland(population: Vector[I]) = DeterministicGAIntegration.migrateToIsland(population)
        def migrateFromIsland(population: Vector[I], state: S) = DeterministicGAIntegration.migrateFromIsland(population)
      }

    }
  }

  case class DeterministicParams(
    nicheSize:           Int,
    niche:               FromContext[Niche[CDGenome.DeterministicIndividual.Individual[Array[Any]], Vector[Int]]],
    genome:              Genome,
    objectives:          Seq[ExactObjective[_]],
    operatorExploration: Double)

  object StochasticParams {

    def niche(genome: Genome, objectives: Seq[NoisyObjective[_]], profiled: Seq[NichedElement]) = {

      def notFoundInGenome(v: Val[_]) = throw new UserBadDataError(s"Variable $v not found in the genome")

      val niches =
        profiled.toVector.map {
          case c: NichedElement.Continuous ⇒
            val index = Genome.continuousIndex(genome, c.v).getOrElse(notFoundInGenome(c.v))
            NoisyNichedNSGA2Algorithm.continuousProfile[Array[Any]](index, c.n).pure[FromContext]
          case c: NichedElement.ContinuousSequence ⇒
            val index = Genome.continuousIndex(genome, c.v).getOrElse(notFoundInGenome(c.v))
            NoisyNichedNSGA2Algorithm.continuousProfile[Array[Any]](index + c.i, c.n).pure[FromContext]
          case c: NichedElement.Discrete ⇒
            val index = Genome.discreteIndex(genome, c.v).getOrElse(notFoundInGenome(c.v))
            NoisyNichedNSGA2Algorithm.discreteProfile[Array[Any]](index).pure[FromContext]
          case c: NichedElement.DiscreteSequence ⇒
            val index = Genome.discreteIndex(genome, c.v).getOrElse(notFoundInGenome(c.v))
            NoisyNichedNSGA2Algorithm.discreteProfile[Array[Any]](index + c.i).pure[FromContext]
          case c: NichedElement.GridContinuous ⇒ FromContext { p ⇒
            import p._
            (Genome.continuousIndex(genome, c.v), Objective.index(objectives, c.v)) match {
              case (Some(index), _) ⇒ NoisyNichedNSGA2Algorithm.gridContinuousProfile[Array[Any]](Genome.continuous(genome).from(context), index, c.intervals)
              case (_, Some(index)) ⇒ NoisyNichedNSGA2Algorithm.gridObjectiveProfile[Array[Any]](NoisyObjective.aggregate(objectives), index, c.intervals)
              case _                ⇒ throw new UserBadDataError(s"Variable ${c.v} not found neither in the genome nor in the objectives")
            }

          }
        }.sequence

      niches.map { ns ⇒ mgo.evolution.niche.sequenceNiches[CDGenome.NoisyIndividual.Individual[Array[Any]], Int](ns) }
    }

    implicit def integration = new MGOAPI.Integration[StochasticParams, (Vector[Double], Vector[Int]), Array[Any]] {
      type G = CDGenome.Genome
      type I = CDGenome.NoisyIndividual.Individual[Array[Any]]
      type S = EvolutionState[Unit]

      def iManifest = implicitly
      def gManifest = implicitly
      def sManifest = implicitly

      private def interpret[U](f: mgo.evolution.contexts.run.Implicits ⇒ (S, U)) = State[S, U] { (s: S) ⇒
        mgo.evolution.algorithm.NoisyProfile.run(s)(f)
      }

      private def zipWithState[M[_]: cats.Monad: StartTime: Random: Generation, T](op: M[T]): M[(S, T)] = {
        import cats.implicits._
        for {
          t ← op
          newState ← mgo.evolution.algorithm.NoisyProfile.state[M]
        } yield (newState, t)
      }

      def operations(om: StochasticParams) = new Ops {

        def randomLens = GenLens[S](_.random)
        def startTimeLens = GenLens[S](_.startTime)
        def generationLens = GenLens[S](_.generation)

        def genomeValues(genome: G) = MGOAPI.paired(CDGenome.continuousValues.get _, CDGenome.discreteValues.get _)(genome)
        def buildGenome(v: (Vector[Double], Vector[Int])): G = CDGenome.buildGenome(v._1, None, v._2, None)
        def buildGenome(vs: Vector[Variable[_]]) = Genome.fromVariables(vs, om.genome).map(buildGenome)

        def buildIndividual(genome: G, phenotype: Array[Any], context: Context) = CDGenome.NoisyIndividual.buildIndividual(genome, phenotype)
        def initialState(rng: util.Random) = EvolutionState[Unit](random = rng, s = ())

        def result(population: Vector[I], state: S) = FromContext { p ⇒
          import p._

          val res = NoisyNichedNSGA2Algorithm.result(population, NoisyObjective.aggregate(om.objectives), om.niche.from(context), Genome.continuous(om.genome).from(context), onlyOldest = true)
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
              zipWithState(NoisyNichedNSGA2Algorithm.adaptiveBreeding[DSL, Array[Any]](n, om.operatorExploration, om.cloneProbability, NoisyObjective.aggregate(om.objectives), discrete).run(individuals)).eval
            }
          }

        def elitism(population: Vector[I], candidates: Vector[I]) =
          FromContext { p ⇒
            import p._
            interpret { impl ⇒
              import impl._
              def step =
                for {
                  elited ← NoisyNichedNSGA2Algorithm.elitism[DSL, Vector[Int], Array[Any]](
                    om.niche.from(context),
                    om.nicheSize,
                    om.historySize,
                    NoisyObjective.aggregate(om.objectives),
                    Genome.continuous(om.genome).from(context)) apply (population, candidates)
                  _ ← incrementGeneration[DSL]
                } yield elited

              zipWithState(step).eval
            }
          }

        def afterGeneration(g: Long, population: Vector[I]): M[Boolean] = interpret { impl ⇒
          import impl._
          zipWithState(mgo.evolution.afterGeneration[DSL, I](g).run(population)).eval
        }

        def afterDuration(d: squants.Time, population: Vector[I]): M[Boolean] = interpret { impl ⇒
          import impl._
          zipWithState(mgo.evolution.afterDuration[DSL, I](d).run(population)).eval
        }

        def migrateToIsland(population: Vector[I]) = StochasticGAIntegration.migrateToIsland[I](population, CDGenome.NoisyIndividual.Individual.historyAge)
        def migrateFromIsland(population: Vector[I], state: S) = StochasticGAIntegration.migrateFromIsland[I, Array[Any]](population, CDGenome.NoisyIndividual.Individual.historyAge, CDGenome.NoisyIndividual.Individual.phenotypeHistory)
      }

    }
  }

  case class StochasticParams(
    nicheSize:           Int,
    niche:               FromContext[Niche[mgo.evolution.algorithm.CDGenome.NoisyIndividual.Individual[Array[Any]], Vector[Int]]],
    operatorExploration: Double,
    genome:              Genome,
    objectives:          Seq[NoisyObjective[_]],
    historySize:         Int,
    cloneProbability:    Double)

  def apply[P](
    niche:      Seq[NichedElement],
    genome:     Genome,
    objectives: Objectives,
    nicheSize:  Int,
    stochastic: OptionalArgument[Stochastic] = None
  ): EvolutionWorkflow =
    WorkflowIntegration.stochasticity(objectives, stochastic.option) match {
      case None ⇒
        val exactObjectives = objectives.map(o ⇒ Objective.toExact(o))
        val integration: WorkflowIntegration.DeterministicGA[_] = new WorkflowIntegration.DeterministicGA(
          DeterministicParams(
            genome = genome,
            objectives = exactObjectives,
            niche = DeterministicParams.niche(genome, exactObjectives, niche),
            operatorExploration = operatorExploration,
            nicheSize = nicheSize
          ),
          genome,
          exactObjectives
        )

        WorkflowIntegration.DeterministicGA.toEvolutionWorkflow(integration)

      case Some(stochasticValue) ⇒
        val noisyObjectives = objectives.map(o ⇒ Objective.toNoisy(o))

        val integration: WorkflowIntegration.StochasticGA[_] = WorkflowIntegration.StochasticGA(
          StochasticParams(
            nicheSize = nicheSize,
            niche = StochasticParams.niche(genome, noisyObjectives, niche),
            operatorExploration = operatorExploration,
            genome = genome,
            objectives = noisyObjectives,
            historySize = stochasticValue.replications,
            cloneProbability = stochasticValue.reevaluate),
          genome,
          noisyObjectives,
          stochasticValue
        )

        WorkflowIntegration.StochasticGA.toEvolutionWorkflow(integration)
    }

}

object NichedNSGA2Evolution {

  import org.openmole.core.dsl.DSL

  def apply(
    evaluation:   DSL,
    termination:  OMTermination,
    niche:        Seq[NichedElement],
    genome:       Genome,
    objectives:   Objectives,
    nicheSize:    Int,
    stochastic:   OptionalArgument[Stochastic] = None,
    parallelism:  Int                          = 1,
    distribution: EvolutionPattern             = SteadyState(),
    suggestion:   Suggestion                   = Suggestion.empty,
    scope:        DefinitionScope              = "niched nsga2") =
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
      distribution = distribution,
      suggestion = suggestion(genome),
      scope = scope
    )

}

