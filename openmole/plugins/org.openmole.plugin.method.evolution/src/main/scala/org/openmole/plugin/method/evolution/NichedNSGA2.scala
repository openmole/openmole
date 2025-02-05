package org.openmole.plugin.method.evolution

import org.openmole.core.exception.UserBadDataError
import cats._
import cats.implicits._
import mgo.evolution._
import mgo.evolution.algorithm._
import mgo.evolution.breeding._
import mgo.evolution.elitism._
import mgo.evolution.niche._
import monocle.macros.GenLens
import org.openmole.core.setter.{ DefinitionScope, ValueAssignment }
import org.openmole.core.workflow.domain._
import org.openmole.core.workflow.sampling._
import org.openmole.plugin.method.evolution.Genome.Suggestion
import org.openmole.plugin.method.evolution.NichedNSGA2.NichedElement
import squants.time.Time
import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.core.workspace.TmpDirectory
import org.openmole.tool.types.FromArray

import monocle._
import monocle.syntax.all._

object NichedNSGA2Algorithm {

  import CDGenome._
  import DeterministicIndividual._

  case class Result[N, P](continuous: Vector[Double], discrete: Vector[Int], fitness: Vector[Double], niche: N, individual: Individual[P])

  def result[N, P](population: Vector[Individual[P]], niche: Individual[P] ⇒ N, continuous: Vector[C], fitness: P ⇒ Vector[Double], keepAll: Boolean) =
    val individuals = if (keepAll) population else nicheElitism[Individual[P], N](population, keepFirstFront[Individual[P]](_, i ⇒ fitness(i.phenotype)), niche)

    individuals.map: i =>
      val (c, d) = scaledVectorValues(continuous)(i.genome)
      Result(
        c,
        d,
        fitness(i.phenotype),
        niche(i),
        i
      )


  def continuousProfile[P](x: Int, nX: Int): Niche[Individual[P], Int] =
    mgo.evolution.niche.continuousProfile[Individual[P]](_.focus(_.genome) andThen continuousVectorValues get, x, nX)

  def discreteProfile[P](x: Int): Niche[Individual[P], Int] =
    mgo.evolution.niche.discreteProfile[Individual[P]](_.focus(_.genome) andThen discreteVectorValues get, x)

  def boundedContinuousProfile[P](continuous: Vector[C], x: Int, nX: Int, min: Double, max: Double): Niche[Individual[P], Int] =
    mgo.evolution.niche.boundedContinuousProfile[Individual[P]](i ⇒ scaledVectorValues(continuous)(i.genome)._1, x, nX, min, max)

  def gridContinuousProfile[P](continuous: Vector[C], x: Int, intervals: Vector[Double]): Niche[Individual[P], Int] =
    mgo.evolution.niche.gridContinuousProfile[Individual[P]](i ⇒ scaledVectorValues(continuous)(i.genome)._1, x, intervals)

  def boundedObjectiveProfile[P](x: Int, nX: Int, min: Double, max: Double, fitness: P ⇒ Vector[Double]): Niche[Individual[P], Int] =
    mgo.evolution.niche.boundedContinuousProfile[Individual[P]](i ⇒ fitness(i.phenotype), x, nX, min, max)

  def gridObjectiveProfile[P](x: Int, intervals: Vector[Double], fitness: P ⇒ Vector[Double]): Niche[Individual[P], Int] =
    mgo.evolution.niche.gridContinuousProfile[Individual[P]](i ⇒ fitness(i.phenotype), x, intervals)

  def initialGenomes(lambda: Int, continuous: Vector[C], discrete: Vector[D], reject: Option[Genome ⇒ Boolean], rng: scala.util.Random) =
    CDGenome.initialGenomes(lambda, continuous, discrete, reject, rng)

  def adaptiveBreeding[S, P](lambda: Int, reject: Option[Genome ⇒ Boolean], operatorExploration: Double, discrete: Vector[D], fitness: P ⇒ Vector[Double]) =
    NSGA2Operations.adaptiveBreeding[S, Individual[P], Genome](
      i ⇒ fitness(i.phenotype),
      Focus[Individual[P]](_.genome).get,
      continuousValues.get,
      continuousOperator.get,
      discreteValues.get,
      discreteOperator.get,
      discrete,
      buildGenome,
      logOfPopulationSize,
      lambda,
      reject,
      operatorExploration)

  def expression[P](fitness: (IArray[Double], IArray[Int]) ⇒ P, components: Vector[C]) =
    DeterministicIndividual.expression[P](fitness, components)

  def elitism[S, N, P](niche: Niche[Individual[P], N], mu: Int, continuous: Vector[C], fitness: P ⇒ Vector[Double]) =
    ProfileOperations.elitism[S, Individual[P], N](
      i => fitness(i.phenotype),
      i => scaledValues(continuous)(i.genome),
      niche,
      mu)

}

object NoisyNichedNSGA2Algorithm {

  import CDGenome._
  import NoisyIndividual._
  import cats.implicits._
  //import shapeless._

  def aggregatedFitness[P: Manifest](aggregation: Vector[P] ⇒ Vector[Double]): Individual[P] ⇒ Vector[Double] = NoisyNSGA2.fitness[P](aggregation)
  case class Result[N, P](continuous: Vector[Double], discrete: Vector[Int], fitness: Vector[Double], niche: N, replications: Int, individual: Individual[P])

  def result[N, P: Manifest](
    population:  Vector[Individual[P]],
    aggregation: Vector[P] ⇒ Vector[Double],
    niche:       Individual[P] ⇒ N,
    continuous:  Vector[C],
    onlyOldest:  Boolean,
    keepAll:     Boolean) =
    def nicheResult(population: Vector[Individual[P]]) =
      if population.isEmpty
      then population
      else
        if onlyOldest
        then
          val firstFront = keepFirstFront(population, NoisyNSGA2.fitness[P](aggregation))
          val sorted = firstFront.sortBy(-_.phenotypeHistory.size)
          val maxHistory = sorted.head.phenotypeHistory.size
          firstFront.filter(_.phenotypeHistory.size == maxHistory)
        else keepFirstFront(population, NoisyNSGA2.fitness[P](aggregation))

    val individuals = if keepAll then population else nicheElitism[Individual[P], N](population, nicheResult, niche)

    individuals.map: i ⇒
      val (c, d, f, r) = NoisyIndividual.aggregate[P](i, aggregation, continuous)
      Result(c, d, f, niche(i), r, i)

  def continuousProfile[P](x: Int, nX: Int): Niche[Individual[P], Int] =
    mgo.evolution.niche.continuousProfile[Individual[P]](_.focus(_.genome) andThen continuousVectorValues get, x, nX)

  def discreteProfile[P](x: Int): Niche[Individual[P], Int] =
    mgo.evolution.niche.discreteProfile[Individual[P]](_.focus(_.genome) andThen discreteVectorValues get, x)

  def boundedContinuousProfile[P](continuous: Vector[C], x: Int, nX: Int, min: Double, max: Double): Niche[Individual[P], Int] =
    mgo.evolution.niche.boundedContinuousProfile[Individual[P]](i => scaleContinuousVectorValues(CDGenome.continuousVectorValues.get(i.genome), continuous), x, nX, min, max)

  def gridContinuousProfile[P](continuous: Vector[C], x: Int, intervals: Vector[Double]): Niche[Individual[P], Int] =
    mgo.evolution.niche.gridContinuousProfile[Individual[P]](i => scaleContinuousVectorValues(CDGenome.continuousVectorValues.get(i.genome), continuous), x, intervals)

  def boundedObjectiveProfile[P: Manifest](aggregation: Vector[P] ⇒ Vector[Double], x: Int, nX: Int, min: Double, max: Double): Niche[Individual[P], Int] =
    mgo.evolution.niche.boundedContinuousProfile[Individual[P]](aggregatedFitness[P](aggregation), x, nX, min, max)

  def gridObjectiveProfile[P: Manifest](aggregation: Vector[P] ⇒ Vector[Double], x: Int, intervals: Vector[Double]): Niche[Individual[P], Int] =
    mgo.evolution.niche.gridContinuousProfile[Individual[P]](aggregatedFitness(aggregation), x, intervals)

  def adaptiveBreeding[S, P: Manifest](lambda: Int, reject: Option[Genome ⇒ Boolean], operatorExploration: Double, cloneProbability: Double, aggregation: Vector[P] ⇒ Vector[Double], discrete: Vector[D]) =
    NoisyNSGA2Operations.adaptiveBreeding[S, Individual[P], Genome, P](
      aggregatedFitness(aggregation),
      Focus[Individual[P]](_.genome).get,
      continuousValues.get,
      continuousOperator.get,
      discreteValues.get,
      discreteOperator.get,
      discrete,
      buildGenome,
      logOfPopulationSize,
      lambda,
      reject,
      operatorExploration,
      cloneProbability)

  def elitism[S, N, P: Manifest](niche: Niche[Individual[P], N], muByNiche: Int, historySize: Int, aggregation: Vector[P] ⇒ Vector[Double], components: Vector[C]): Elitism[S, Individual[P]] =
    def individualValues(i: Individual[P]) = scaledValues(components)(i.genome)

    NoisyProfileOperations.elitism[S, Individual[P], N, P](
      aggregatedFitness(aggregation),
      mergeHistories(individualValues, vectorPhenotype, Focus[Individual[P]](_.historyAge), historySize),
      individualValues,
      niche,
      muByNiche)

  def expression[P: Manifest](fitness: (util.Random, IArray[Double], IArray[Int]) ⇒ P, continuous: Vector[C]) =
    NoisyIndividual.expression(fitness, continuous)

  def initialGenomes(lambda: Int, continuous: Vector[C], discrete: Vector[D], reject: Option[Genome ⇒ Boolean], rng: scala.util.Random) =
    CDGenome.initialGenomes(lambda, continuous, discrete, reject, rng)

}

object NichedNSGA2 {

  object NichedElement {
    implicit def fromValInt(v: Val[Int]): Discrete = Discrete(v)

    implicit def fromAggregateString[A](a: Evaluate[Val[A], String]): Aggregated = Aggregated(a.value, a.evaluate)

    implicit def fromAggregate[A, V[_]: FromArray](a: Evaluate[Val[A], V[A] ⇒ Int]): Aggregated = {
      val f =
        FromContext: p ⇒
          import p._
          a.evaluate(implicitly[FromArray[V]].apply(context(a.value.array)))

      Aggregated(a.value, f)
    }

    case class Discrete(v: Val[Int]) extends NichedElement
    case class Aggregated(v: Val[?], a: FromContext[Int]) extends NichedElement

    def valContent(n: NichedElement): Val[?] =
      n match
        case d: Discrete   ⇒ d.v
        case a: Aggregated ⇒ a.v

    def validate(n: NichedElement, values: Seq[Val[?]]): Validate =
      n match
        case d: Discrete ⇒ Validate.success
        case a: Aggregated ⇒ Validate { p ⇒
          import p._
          a.a.validate(inputs ++ values)
        }

    type Exact = Val[Int]
    type Noisy = FromContext[Int]

    def toExact(n: NichedElement) =
      n match
        case d: Discrete ⇒ d.v
        case _           ⇒ throw new UserBadDataError(s"Niche element $n cannot be aggregated it should be exact.")

    def toNoisy(n: NichedElement) = FromContext { p ⇒
      import p._

      n match
        case d: Discrete   ⇒ context(d.v.array).head
        case a: Aggregated ⇒ a.a.from(context)
    }

  }

  sealed trait NichedElement

  object DeterministicNichedNSGA2 {

    def niche(phenotypeContent: PhenotypeContent, niche: Seq[NichedElement.Exact]) = FromContext { p ⇒
      import p._

      (i: CDGenome.DeterministicIndividual.Individual[Phenotype]) ⇒
        val context = Context((phenotypeContent.outputs zip Phenotype.outputs(phenotypeContent, i.phenotype)).map { case (v, va) ⇒ Variable.unsecure(v, va) } *)
        niche.map(n ⇒ context(n)).toVector
    }

    import CDGenome.DeterministicIndividual

    implicit def integration: MGOAPI.Integration[DeterministicNichedNSGA2, (IArray[Double], IArray[Int]), Phenotype] = new MGOAPI.Integration[DeterministicNichedNSGA2, (IArray[Double], IArray[Int]), Phenotype] {
      type G = CDGenome.Genome
      type I = DeterministicIndividual.Individual[Phenotype]
      type S = EvolutionState[Unit]

      def iManifest = implicitly
      def gManifest = implicitly
      def sManifest = implicitly

      def operations(om: DeterministicNichedNSGA2) = new Ops:
        def startTimeLens = GenLens[S](_.startTime)
        def generationLens = GenLens[S](_.generation)
        def evaluatedLens = GenLens[S](_.evaluated)

        def genomeValues(genome: G) = MGOAPI.paired(CDGenome.continuousValues.get, CDGenome.discreteValues.get)(genome)

        def buildGenome(vs: Vector[Variable[?]]) =
          def buildGenome(v: (IArray[Double], IArray[Int])): G = CDGenome.buildGenome(v._1, None, v._2, None)
          buildGenome(Genome.fromVariables(vs, om.genome))

        def genomeToVariables(g: G): FromContext[Vector[Variable[?]]] =
          val (cs, is) = genomeValues(g)
          Genome.toVariables(om.genome, cs, is, scale = true)

        def buildIndividual(genome: G, phenotype: Phenotype, state: S) = CDGenome.DeterministicIndividual.buildIndividual(genome, phenotype, state.generation, false)
        def initialState = EvolutionState[Unit](s = ())

        def result(population: Vector[I], state: S, keepAll: Boolean, includeOutputs: Boolean) =
          FromContext: p ⇒
            import p._
            val res = NichedNSGA2Algorithm.result(population, om.niche.from(context), Genome.continuous(om.genome), Objective.toFitnessFunction(om.phenotypeContent, om.objectives).from(context), keepAll = keepAll)
            val genomes = GAIntegration.genomesOfPopulationToVariables(om.genome, res.map(_.continuous) zip res.map(_.discrete), scale = false)
            val fitness = GAIntegration.objectivesOfPopulationToVariables(om.objectives, res.map(_.fitness))
            val generated = Variable(GAIntegration.generatedVal.array, res.map(_.individual.generation).toArray)

            val outputValues = if (includeOutputs) DeterministicGAIntegration.outputValues(om.phenotypeContent, res.map(_.individual.phenotype)) else Seq()

            genomes ++ fitness ++ Seq(generated) ++ outputValues

        def initialGenomes(n: Int, rng: scala.util.Random) =
          FromContext: p ⇒
            import p._
            val continuous = Genome.continuous(om.genome)
            val discrete = Genome.discrete(om.genome)
            val rejectValue = om.reject.map(f ⇒ GAIntegration.rejectValue[G](f, om.genome, _.continuousValues, _.discreteValues).from(context))
            mgo.evolution.algorithm.Profile.initialGenomes(n, continuous, discrete, rejectValue, rng)

        def breeding(population: Vector[I], n: Int, s: S, rng: scala.util.Random) =
          FromContext: p ⇒
            import p._
            val discrete = Genome.discrete(om.genome)
            val rejectValue = om.reject.map(f ⇒ GAIntegration.rejectValue[G](f, om.genome, _.continuousValues, _.discreteValues).from(context))
            mgo.evolution.algorithm.Profile.adaptiveBreeding[Phenotype](n, om.operatorExploration, discrete, Objective.toFitnessFunction(om.phenotypeContent, om.objectives).from(context), rejectValue) apply (s, population, rng)

        def elitism(population: Vector[I], candidates: Vector[I], s: S, rng: scala.util.Random) =
          FromContext: p ⇒
            import p._
            NichedNSGA2Algorithm.elitism[S, Vector[Int], Phenotype](om.niche.from(context), om.nicheSize, Genome.continuous(om.genome), Objective.toFitnessFunction(om.phenotypeContent, om.objectives).from(context)) apply (s, population, candidates, rng)

        def mergeIslandState(state: S, islandState: S): S = state
        def migrateToIsland(population: Vector[I], state: S) = (DeterministicGAIntegration.migrateToIsland(population), state)
        def migrateFromIsland(population: Vector[I], initialState: S, state: S) = (DeterministicGAIntegration.migrateFromIsland(population, initialState.generation), state)

    }
  }

  case class DeterministicNichedNSGA2(
    nicheSize:           Int,
    niche:               FromContext[Niche[CDGenome.DeterministicIndividual.Individual[Phenotype], Vector[Int]]],
    genome:              Genome,
    phenotypeContent:    PhenotypeContent,
    objectives:          Seq[Objective],
    operatorExploration: Double,
    reject:              Option[Condition])

  object StochasticNichedNSGA2 {

    def niche(phenotypeContent: PhenotypeContent, niche: Seq[NichedElement.Noisy]) = FromContext { p ⇒
      import p._

      (i: CDGenome.NoisyIndividual.Individual[Phenotype]) ⇒
        import org.openmole.tool.types.TypeTool._
        val values = i.phenotypeHistory.map(Phenotype.outputs(phenotypeContent, _)).transpose
        val context =
          (phenotypeContent.outputs zip values).map: (v, va) ⇒
            val array = fillArray(v.`type`.manifest, va)
            Variable.unsecure(v.toArray, array)

        niche.map(_.from(context)).toVector

    }

    implicit def integration: MGOAPI.Integration[StochasticNichedNSGA2, (IArray[Double], IArray[Int]), Phenotype] = new MGOAPI.Integration[StochasticNichedNSGA2, (IArray[Double], IArray[Int]), Phenotype] {
      type G = CDGenome.Genome
      type I = CDGenome.NoisyIndividual.Individual[Phenotype]
      type S = EvolutionState[Unit]

      def iManifest = implicitly
      def gManifest = implicitly
      def sManifest = implicitly

      def operations(om: StochasticNichedNSGA2) = new Ops:
        def startTimeLens = GenLens[S](_.startTime)
        def generationLens = GenLens[S](_.generation)
        def evaluatedLens = GenLens[S](_.evaluated)

        def genomeValues(genome: G) = MGOAPI.paired(CDGenome.continuousValues.get, CDGenome.discreteValues.get)(genome)
        def buildGenome(vs: Vector[Variable[?]]) =
          def buildGenome(v: (IArray[Double], IArray[Int])): G = CDGenome.buildGenome(v._1, None, v._2, None)
          buildGenome(Genome.fromVariables(vs, om.genome))

        def genomeToVariables(g: G): FromContext[Vector[Variable[?]]] =
          val (cs, is) = genomeValues(g)
          Genome.toVariables(om.genome, cs, is, scale = true)

        def buildIndividual(genome: G, phenotype: Phenotype, state: S) = CDGenome.NoisyIndividual.buildIndividual(genome, phenotype, state.generation, false)
        def initialState = EvolutionState[Unit](s = ())

        def result(population: Vector[I], state: S, keepAll: Boolean, includeOutputs: Boolean) =
          FromContext: p ⇒
            import p._

            val res = NoisyNichedNSGA2Algorithm.result(population, Objective.aggregate(om.phenotypeContent, om.objectives).from(context), om.niche.from(context), Genome.continuous(om.genome), onlyOldest = true, keepAll = keepAll)
            val genomes = GAIntegration.genomesOfPopulationToVariables(om.genome, res.map(_.continuous) zip res.map(_.discrete), scale = false)
            val fitness = GAIntegration.objectivesOfPopulationToVariables(om.objectives, res.map(_.fitness))
            val samples = Variable(GAIntegration.samplesVal.array, res.map(_.replications).toArray)
            val generated = Variable(GAIntegration.generatedVal.array, res.map(_.individual.generation).toArray)

            val outputValues = if (includeOutputs) StochasticGAIntegration.outputValues(om.phenotypeContent, res.map(_.individual.phenotypeHistory)) else Seq()

            genomes ++ fitness ++ Seq(samples, generated) ++ outputValues

        def initialGenomes(n: Int, rng: scala.util.Random) =
          FromContext: p ⇒
            import p._
            val continuous = Genome.continuous(om.genome)
            val discrete = Genome.discrete(om.genome)
            val rejectValue = om.reject.map(f ⇒ GAIntegration.rejectValue[G](f, om.genome, _.continuousValues, _.discreteValues).from(context))
            NoisyNichedNSGA2Algorithm.initialGenomes(n, continuous, discrete, rejectValue, rng)

        def breeding(individuals: Vector[I], n: Int, s: S, rng: scala.util.Random) =
          FromContext: p ⇒
            import p._
            val discrete = Genome.discrete(om.genome)
            val rejectValue = om.reject.map(f ⇒ GAIntegration.rejectValue[G](f, om.genome, _.continuousValues, _.discreteValues).from(context))
            NoisyNichedNSGA2Algorithm.adaptiveBreeding[S, Phenotype](n, rejectValue, om.operatorExploration, om.cloneProbability, Objective.aggregate(om.phenotypeContent, om.objectives).from(context), discrete) apply (s, individuals, rng)

        def elitism(population: Vector[I], candidates: Vector[I], s: S, rng: scala.util.Random) =
          FromContext: p ⇒
            import p._
            NoisyNichedNSGA2Algorithm.elitism[S, Vector[Int], Phenotype](
              om.niche.from(context),
              om.nicheSize,
              om.historySize,
              Objective.aggregate(om.phenotypeContent, om.objectives).from(context),
              Genome.continuous(om.genome)) apply (s, population, candidates, rng)

        def mergeIslandState(state: S, islandState: S): S = state
        def migrateToIsland(population: Vector[I], state: S) = (StochasticGAIntegration.migrateToIsland(population), state)
        def migrateFromIsland(population: Vector[I], initialState: S, state: S) = (StochasticGAIntegration.migrateFromIsland(population, initialState.generation), state)


    }
  }

  case class StochasticNichedNSGA2(
    nicheSize:           Int,
    niche:               FromContext[Niche[mgo.evolution.algorithm.CDGenome.NoisyIndividual.Individual[Phenotype], Vector[Int]]],
    operatorExploration: Double,
    genome:              Genome,
    phenotypeContent:    PhenotypeContent,
    objectives:          Seq[Objective],
    historySize:         Int,
    cloneProbability:    Double,
    reject:              Option[Condition])

  def apply[P](
    niche:      Seq[NichedElement],
    genome:     Genome,
    objective:  Objectives,
    nicheSize:  Int,
    outputs:    Seq[Val[?]]                  = Seq(),
    stochastic: OptionalArgument[Stochastic] = None,
    reject:     OptionalArgument[Condition]  = None): EvolutionWorkflow =
    EvolutionWorkflow.stochasticity(objective, stochastic.option) match
      case None ⇒
        val exactObjectives = Objectives.toExact(objective)
        val nicheVals = niche.map(NichedElement.valContent)
        val phenotypeContent = PhenotypeContent(Objectives.prototypes(exactObjectives) ++ nicheVals, outputs)

        def validation: Validate =
          niche.map(n ⇒ NichedElement.validate(n, outputs)) ++
            Objectives.validate(exactObjectives, outputs)

        EvolutionWorkflow.deterministicGAIntegration(
          DeterministicNichedNSGA2(
            genome = genome,
            objectives = exactObjectives,
            phenotypeContent = phenotypeContent,
            niche = DeterministicNichedNSGA2.niche(phenotypeContent, niche.map(NichedElement.toExact)),
            operatorExploration = EvolutionWorkflow.operatorExploration,
            nicheSize = nicheSize,
            reject = reject.option),
          genome = genome,
          phenotypeContent = phenotypeContent,
          validate = validation
        )

      case Some(stochasticValue) ⇒
        val noisyObjectives = Objectives.toNoisy(objective)
        val nicheVals = niche.map(NichedElement.valContent)
        val phenotypeContent = PhenotypeContent(Objectives.prototypes(noisyObjectives) ++ nicheVals, outputs)

        def validation: Validate =
          val aOutputs = outputs.map(_.toArray)
          niche.map(n ⇒ NichedElement.validate(n, aOutputs)) ++
            Objectives.validate(noisyObjectives, aOutputs)

        EvolutionWorkflow.stochasticGAIntegration(
          StochasticNichedNSGA2(
            nicheSize = nicheSize,
            niche = StochasticNichedNSGA2.niche(phenotypeContent, niche.map(NichedElement.toNoisy)),
            operatorExploration = EvolutionWorkflow.operatorExploration,
            genome = genome,
            phenotypeContent = phenotypeContent,
            objectives = noisyObjectives,
            historySize = stochasticValue.sample,
            cloneProbability = stochasticValue.reevaluate,
            reject = reject.option),
          genome,
          phenotypeContent,
          stochasticValue,
          validate = validation
        )



}

import EvolutionWorkflow._
import monocle.macros._

object NichedNSGA2Evolution {

  given EvolutionMethod[NichedNSGA2Evolution] =
    p =>
      NichedNSGA2(
        niche = p.niche,
        genome = p.genome,
        outputs = p.evaluation.outputs,
        nicheSize = p.nicheSize,
        objective = p.objective,
        stochastic = p.stochastic,
        reject = p.reject
      )

  given ExplorationMethod[NichedNSGA2Evolution, EvolutionWorkflow] =
    p ⇒
      EvolutionWorkflow(
        method = p,
        evaluation = p.evaluation,
        termination = p.termination,
        parallelism = p.parallelism,
        distribution = p.distribution,
        suggestion = p.suggestion(p.genome),
        scope = p.scope
      )

  given ExplorationMethodSetter[NichedNSGA2Evolution, EvolutionPattern] = (e, p) ⇒ e.copy(distribution = p)

}

case class NichedNSGA2Evolution(
  evaluation:   DSL,
  termination:  OMTermination,
  niche:        Seq[NichedElement],
  genome:       Genome,
  objective:    Objectives,
  nicheSize:    Int,
  stochastic:   OptionalArgument[Stochastic] = None,
  parallelism:  Int                          = EvolutionWorkflow.parallelism,
  reject:       OptionalArgument[Condition]  = None,
  distribution: EvolutionPattern             = SteadyState(),
  suggestion:   Suggestion                   = Suggestion.empty,
  scope:        DefinitionScope              = "niched nsga2")

