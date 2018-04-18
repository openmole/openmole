package org.openmole.plugin.method.evolution

import monocle.macros.GenLens
import org.openmole.core.expansion.FromContext
import org.openmole.core.workflow.domain.Fix
import org.openmole.core.workflow.sampling.Factor
import cats.implicits._

object OSE {

  //  object PatternAxe {
  //
  //    implicit def fromDoubleDomainToPatternAxe[D](f: Factor[D, Double])(implicit fix: Fix[D, Double]): PatternAxe =
  //      PatternAxe(f.prototype, fix(f.domain).toVector)
  //
  //    implicit def fromIntDomainToPatternAxe[D](f: Factor[D, Int])(implicit fix: Fix[D, Int]): PatternAxe =
  //      PatternAxe(f.prototype, fix(f.domain).toVector.map(_.toDouble))
  //
  //    implicit def fromLongDomainToPatternAxe[D](f: Factor[D, Long])(implicit fix: Fix[D, Long]): PatternAxe =
  //      PatternAxe(f.prototype, fix(f.domain).toVector.map(_.toDouble))
  //
  //  }
  //
  //  case class PatternAxe(p: Objective, scale: Vector[Double])
  //
  case class DeterministicParams(
    mu:                  Int,
    origin:              (Vector[Double], Vector[Int]) ⇒ Vector[Int],
    limit:               Vector[Double],
    genome:              Genome,
    objectives:          Objectives,
    operatorExploration: Double)

  object DeterministicParams {

    import mgo.algorithm.{ OSE ⇒ MGOOSE, _ }
    import mgo.algorithm.OSE._
    import cats.data._
    import freedsl.dsl._
    import mgo.contexts._

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
        zipWithState(mgo.afterGeneration[DSL, I](g).run(population)).eval
      }

      def afterDuration(d: squants.Time, population: Vector[I]): M[Boolean] = interpret { impl ⇒
        import impl._
        zipWithState(mgo.afterDuration[DSL, I](d).run(population)).eval
      }

      def operations(om: DeterministicParams) = new Ops {

        def randomLens = GenLens[S](_.random)
        def startTimeLens = GenLens[S](_.startTime)
        def generation(s: S) = s.generation

        def genomeValues(genome: G) = MGOAPI.paired(CDGenome.continuousValues.get _, CDGenome.discreteValues.get _)(genome)
        def buildIndividual(genome: G, phenotype: Vector[Double]) = CDGenome.DeterministicIndividual.buildIndividual(genome, phenotype)

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

        def elitism(individuals: Vector[I]) =
          Genome.continuous(om.genome).map { continuous ⇒
            interpret { impl ⇒
              import impl._
              def step =
                for {
                  elited ← MGOOSE.elitism[DSL](om.mu, om.limit, om.origin, continuous) apply individuals
                  _ ← mgo.elitism.incrementGeneration[DSL]
                } yield elited

              zipWithState(step).eval
            }
          }

        def migrateToIsland(population: Vector[I]) = population
        def migrateFromIsland(population: Vector[I]) = population
      }

    }
  }

  case class StochasticParams(
    mu:                  Int,
    origin:              (Vector[Double], Vector[Int]) ⇒ Vector[Int],
    limit:               Vector[Double],
    aggregation:         Vector[Vector[Double]] ⇒ Vector[Double],
    genome:              Genome,
    objectives:          Objectives,
    historySize:         Int,
    cloneProbability:    Double,
    operatorExploration: Double
  )

  object StochasticParams {

    import mgo.algorithm._
    import mgo.algorithm.{ NoisyOSE ⇒ MGONoisyOSE, _ }
    import mgo.algorithm.NoisyOSE._
    import cats.data._
    import freedsl.dsl._
    import mgo.contexts._

    implicit def integration = new MGOAPI.Integration[StochasticParams, (Vector[Double], Vector[Int]), Vector[Double]] { api ⇒
      type G = CDGenome.Genome
      type I = CDGenome.NoisyIndividual.Individual
      type S = EvolutionState[OSEState]

      def iManifest = implicitly
      def gManifest = implicitly
      def sManifest = implicitly

      private def interpret[U](f: OSEImplicits ⇒ (S, U)) = State[S, U] { (s: S) ⇒
        mgo.algorithm.NoisyOSE.run(s)(f)
      }

      private def zipWithState[M[_]: cats.Monad: StartTime: Random: Generation: ReachMap, T](op: M[T])(implicit archive: Archive[M, I]): M[(S, T)] = {
        import cats.implicits._
        for {
          t ← op
          newState ← MGONoisyOSE.state[M]
        } yield (newState, t)
      }

      def afterGeneration(g: Long, population: Vector[I]): M[Boolean] = interpret { impl ⇒
        import impl._
        zipWithState(mgo.afterGeneration[DSL, I](g).run(population)).eval
      }

      def afterDuration(d: squants.Time, population: Vector[I]): M[Boolean] = interpret { impl ⇒
        import impl._
        zipWithState(mgo.afterDuration[DSL, I](d).run(population)).eval
      }

      def operations(om: StochasticParams) = new Ops {
        def randomLens = GenLens[S](_.random)
        def startTimeLens = GenLens[S](_.startTime)

        def generation(s: S) = s.generation
        def genomeValues(genome: G) = MGOAPI.paired(CDGenome.continuousValues.get _, CDGenome.discreteValues.get _)(genome)

        def buildIndividual(genome: G, phenotype: Vector[Double]) = CDGenome.NoisyIndividual.buildIndividual(genome, phenotype)
        def initialState(rng: util.Random) = EvolutionState[OSEState](random = rng, s = (Array.empty, Array.empty))

        def result(population: Vector[I], state: S) = FromContext { p ⇒
          import p._
          import org.openmole.core.context._

          val res = MGONoisyOSE.result(state, om.aggregation, Genome.continuous(om.genome).from(context))
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
              zipWithState(MGONoisyOSE.adaptiveBreeding[DSL](
                n,
                om.operatorExploration,
                om.cloneProbability,
                om.aggregation,
                discrete,
                om.origin,
                om.limit).run(individuals)).eval
            }
          }

        def elitism(individuals: Vector[I]) =
          Genome.continuous(om.genome).map { continuous ⇒
            interpret { impl ⇒
              import impl._
              def step =
                for {
                  elited ← MGONoisyOSE.elitism[DSL](
                    om.mu,
                    om.historySize,
                    om.aggregation,
                    continuous,
                    om.origin,
                    om.limit) apply individuals
                  _ ← mgo.elitism.incrementGeneration[DSL]
                } yield elited

              zipWithState(step).eval
            }
          }

        def afterGeneration(g: Long, population: Vector[I]) = api.afterGeneration(g, population)
        def afterDuration(d: squants.Time, population: Vector[I]) = api.afterDuration(d, population)
        def migrateToIsland(population: Vector[I]) = population
        def migrateFromIsland(population: Vector[I]) = population

      }

    }
  }
  //
  //  import org.openmole.core.dsl._
  //
  //  def apply(
  //             genome:     Genome,
  //             objectives: Seq[PatternAxe],
  //             stochastic: OptionalArgument[Stochastic] = None
  //           ) = stochastic.option match {
  //    case None ⇒
  //      val integration: WorkflowIntegration.DeterministicGA[_] = WorkflowIntegration.DeterministicGA(
  //        DeterministicParams(
  //          mgo.niche.irregularGrid(objectives.map(_.scale).toVector),
  //          genome,
  //          objectives.map(_.p),
  //          operatorExploration),
  //        genome,
  //        objectives.map(_.p)
  //      )
  //
  //      WorkflowIntegration.DeterministicGA.toEvolutionWorkflow(integration)
  //    case Some(stochastic) ⇒
  //      val integration: WorkflowIntegration.StochasticGA[_] = WorkflowIntegration.StochasticGA(
  //        StochasticParams(
  //          pattern = mgo.niche.irregularGrid(objectives.map(_.scale).toVector),
  //          aggregation = StochasticGAIntegration.aggregateVector(stochastic.aggregation, _),
  //          genome = genome,
  //          objectives = objectives.map(_.p),
  //          historySize = stochastic.replications,
  //          cloneProbability = stochastic.reevaluate,
  //          operatorExploration = operatorExploration
  //        ),
  //        genome,
  //        objectives.map(_.p),
  //        stochastic
  //      )
  //
  //      WorkflowIntegration.StochasticGA.toEvolutionWorkflow(integration)
  //  }

}
