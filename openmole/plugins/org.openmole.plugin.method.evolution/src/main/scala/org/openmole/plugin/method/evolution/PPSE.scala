//package org.openmole.plugin.method.evolution
//
//import monocle.Focus
//import org.openmole.core.dsl._
//import org.openmole.core.dsl.extension._
//import org.openmole.plugin.method.evolution.PSE.{ DeterministicParams, PatternAxe }
//import squants.time.Time
//
///*
// * Copyright (C) 2021 Romain Reuillon
// *
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU Affero General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU Affero General Public License for more details.
// *
// * You should have received a copy of the GNU Affero General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// */
//
//object PPSE {
//
//  def densityVal = Val[Double]("density", GAIntegration.namespace)
//
//  case class DeterministicParams(
//    pattern:          Vector[Double] ⇒ Vector[Int],
//    genome:           GenomeDouble,
//    phenotypeContent: PhenotypeContent,
//    objectives:       Seq[Objective],
//    grid:             Seq[PatternAxe],
//    dilation:         Double,
//    rarest:           Int,
//    reject:           Option[Condition],
//    gmmIterations:    Int                          = 100,
//    gmmTolerance:     Double                       = 0.0001,
//    warmupSampler:    Int                          = 10000)
//
//  object DeterministicParams {
//
//    import mgo.evolution.algorithm.{ PSE ⇒ _, _ }
//    import cats.data._
//
//    implicit def integration: MGOAPI.Integration[DeterministicParams, Vector[Double], Phenotype] = new MGOAPI.Integration[DeterministicParams, Vector[Double], Phenotype] { api ⇒
//      type G = (Array[Double], Double)
//      type I = mgo.evolution.algorithm.EMPPSE.Individual[Phenotype]
//      type S = EvolutionState[mgo.evolution.algorithm.EMPPSE.EMPPSEState]
//
//      def iManifest = implicitly
//      def gManifest = implicitly
//      def sManifest = implicitly
//
//      def operations(om: DeterministicParams) = new Ops {
//
//        def startTimeLens = Focus[S](_.startTime)
//        def generationLens = Focus[S](_.generation)
//        def evaluatedLens = Focus[S](_.evaluated)
//
//        def genomeValues(genome: G) = genome._1.toVector
//
//        def buildGenome(vs: Vector[Variable[?]]) =
//          (GenomeDouble.fromVariables(vs, om.genome).toArray, 0.0)
//
//        def genomeToVariables(g: G): FromContext[Vector[Variable[?]]] = {
//          val cs = genomeValues(g)
//          GenomeDouble.toVariables(om.genome, cs, scale = true)
//        }
//
//        def buildIndividual(genome: G, phenotype: Phenotype, context: Context) = mgo.evolution.algorithm.EMPPSE.buildIndividual(genome, phenotype)
//
//        def initialState = EvolutionState(s = mgo.evolution.algorithm.EMPPSE.EMPPSEState())
//
//        def afterEvaluated(g: Long, s: S, population: Vector[I]): Boolean = mgo.evolution.stop.afterEvaluated[S, I](g, Focus[S](_.evaluated))(s, population)
//        def afterGeneration(g: Long, s: S, population: Vector[I]): Boolean = mgo.evolution.stop.afterGeneration[S, I](g, Focus[S](_.generation))(s, population)
//        def afterDuration(d: Time, s: S, population: Vector[I]): Boolean = mgo.evolution.stop.afterDuration[S, I](d, Focus[S](_.startTime))(s, population)
//
//        def result(population: Vector[I], state: S, keepAll: Boolean, includeOutputs: Boolean) = FromContext { p ⇒
//          import p._
//          val res = mgo.evolution.algorithm.EMPPSE.result[Phenotype](
//            population,
//            state,
//            om.genome.continuous,
//            Objective.toFitnessFunction(om.phenotypeContent, om.objectives).from(context).apply _,
//            om.pattern
//          )
//
//          val genomes = GAIntegration.genomesDoubleOfPopulationToVariables(om.genome, res.map(_.continuous), scale = false)
//          val fitness = GAIntegration.objectivesOfPopulationToVariables(om.objectives, res.map(_.phenotype))
//          val densities = Seq(Variable(densityVal.array, res.map(_.density).toArray))
//          val outputValues = if (includeOutputs) DeterministicGAIntegration.outputValues(om.phenotypeContent, res.map(_.individual.phenotype)) else Seq()
//
//          genomes ++ fitness ++ densities ++ outputValues
//        }
//
//        def initialGenomes(n: Int, rng: scala.util.Random) = FromContext { p ⇒
//          import p._
//          val continuous = om.genome.continuous
//          //val rejectValue = om.reject.map(f ⇒ GAIntegration.rejectValue[G](f, om.genome, _.continuousValues, CDGenome.discreteValues(om.genome.discrete).get).from(context))
//          mgo.evolution.algorithm.EMPPSE.initialGenomes(n, continuous, rng)
//        }
//
//        private def pattern(phenotype: Phenotype) = FromContext { p ⇒
//          import p._
//          om.pattern(Objective.toFitnessFunction(om.phenotypeContent, om.objectives).from(context).apply(phenotype))
//        }
//
//        def breeding(individuals: Vector[I], n: Int, s: S, rng: scala.util.Random) = FromContext { p ⇒
//          import p._
//          //val discrete = om.genome.discrete
//          val rejectValue = om.reject.map(f ⇒ GAIntegration.rejectValue[Vector[Double]](f, om.genome, identity, _ => Vector()).from(context))
//          mgo.evolution.algorithm.EMPPSEOperation.breeding[S, I, G](
//            om.genome.continuous,
//            identity,
//            n,
//            rejectValue,
//            Focus[S](_.s.gmm).get)(s, individuals, rng)
//        }
//
//        def elitism(population: Vector[I], candidates: Vector[I], s: S, evaluated: Long, rng: scala.util.Random) = FromContext { p ⇒
//          import p._
//
//          val rejectValue = om.reject.map(f ⇒ GAIntegration.rejectValue[Vector[Double]](f, om.genome, identity, _ => Vector()).from(context))
//          val (s2, elited) = mgo.evolution.algorithm.EMPPSEOperation.elitism[S, I, Phenotype](
//            _.genome,
//            _.phenotype,
//            pattern(_).from(context),
//            om.genome.continuous,
//            rejectValue,
//            Focus[S](_.s.probabilityMap),
//            Focus[S](_.s.hitmap),
//            Focus[S](_.s.gmm),
//            fitOnRarest = om.rarest,
//            iterations = om.gmmIterations,
//            tolerance = om.gmmTolerance,
//            warmupSampler = om.warmupSampler,
//            dilation = om.dilation) apply (s, population, candidates, rng)
//
//          val s3 = Focus[S](_.generation).modify(_ + 1)(s2)
//          val s4 = Focus[S](_.evaluated).modify(_ + evaluated)(s3)
//          (s4, elited)
//        }
//
//        def migrateToIsland(population: Vector[I]) = ??? //population.map(PSEAlgorithm.Individual.foundedIsland.set(true))
//        def migrateFromIsland(population: Vector[I], state: S) = ???
//        //          population.filter(i ⇒ !PSEAlgorithm.Individual.foundedIsland.get(i)).
//        //            map(PSEAlgorithm.Individual.foundedIsland.set(false))
//      }
//
//    }
//  }
//
//  def apply(
//    genome:    GenomeDouble,
//    objective: Seq[PatternAxe],
//    dilation:  Double,
//    rarest:    Int,
//    reject:    Option[Condition],
//    outputs:   Seq[Val[?]]     = Seq()) = {
//    val exactObjectives = Objectives.toExact(objective.map(_.p))
//    val phenotypeContent = PhenotypeContent(Objectives.prototypes(exactObjectives), outputs)
//
//    EvolutionWorkflow.deterministicGAIntegration(
//      DeterministicParams(
//        mgo.evolution.niche.irregularGrid(objective.map(_.scale).toVector),
//        genome,
//        phenotypeContent,
//        exactObjectives,
//        reject = reject,
//        grid = objective,
//        dilation = dilation,
//        rarest = rarest),
//      genome,
//      phenotypeContent,
//      validate = Objectives.validate(exactObjectives, outputs)
//    )
//  }
//
//}
//import monocle.macros._
//import EvolutionWorkflow._
//
//object PPSEEvolution {
//
//  import org.openmole.core.dsl.DSL
//
//  implicit def method: ExplorationMethod[PPSEEvolution, EvolutionWorkflow] =
//    p ⇒
//      EvolutionPattern.build(
//        algorithm =
//          PPSE(
//            genome = p.genome,
//            objective = p.objective,
//            outputs = p.evaluation.outputs,
//            dilation = p.dilation,
//            rarest = p.rarest,
//            reject = p.reject
//          ),
//        evaluation = p.evaluation,
//        termination = p.termination,
//        parallelism = p.parallelism,
//        distribution = SteadyState(),
//        suggestion = Genome.Suggestion.empty.apply(p.genome),
//        scope = p.scope
//      )
//
//  implicit def patternContainer: ExplorationMethodSetter[PSEEvolution, EvolutionPattern] = (e, p) ⇒ e.copy(distribution = p)
//
//}
//
//case class PPSEEvolution(
//  genome:      GenomeDouble,
//  objective:   Seq[PSE.PatternAxe],
//  evaluation:  DSL,
//  termination: OMTermination,
//  reject:      OptionalArgument[Condition]  = None,
//  parallelism: Int                 = EvolutionWorkflow.parallelism,
//  dilation:    Double              = 2.0,
//  rarest:      Int                 = 100,
//  scope:       DefinitionScope     = "ppse")
