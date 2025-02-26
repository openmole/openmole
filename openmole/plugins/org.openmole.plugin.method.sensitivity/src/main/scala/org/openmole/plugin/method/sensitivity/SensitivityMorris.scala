package org.openmole.plugin.method.sensitivity

import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*
import org.openmole.core.workflow.sampling.ScalableValue
import org.openmole.plugin.tool.pattern.MapReduce

/*
 * Copyright (C) 2021 Romain Reuillon
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
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

object SensitivityMorris {
  def methodName = MethodMetaData.name(SensitivityMorris)
  
  def mu(input: Val[?], output: Val[?]) = input.withNamespace(Namespace("mu", output.name))
  def muStar(input: Val[?], output: Val[?]) = input.withNamespace(Namespace("muStar", output.name))
  def sigma(input: Val[?], output: Val[?]) = input.withNamespace(Namespace("sigma", output.name))

  import io.circe.*

  object MetaData:
    given MethodMetaData[MetaData] = MethodMetaData(SensitivityMorris.methodName)

    def apply(method: Method) =
      new MetaData(
        inputs = method.inputs.map(_.prototype).map(ValData.apply),
        outputs = method.outputs.map(ValData.apply)
      )

  case class MetaData(inputs: Seq[ValData], outputs: Seq[ValData]) derives derivation.ConfiguredCodec


  case class Method(inputs: Seq[ScalableValue], outputs: Seq[Val[?]])

  object MorrisHook:

    def apply(method: Method, output: WritableOutput)(implicit name: sourcecode.Name, definitionScope: DefinitionScope, scriptSourceData: ScriptSourceData) =
      Hook("MorrisHook"): p =>
        import p._
        import WritableOutput._

        val inputs = ScalableValue.prototypes(method.inputs)

        import OutputFormat.*

        def sections =
          OutputContent(
            ("mu", Sensitivity.variableResults(inputs, method.outputs, SensitivityMorris.mu(_, _)).from(context)),
            ("muStar", Sensitivity.variableResults(inputs, method.outputs, SensitivityMorris.muStar(_, _)).from(context)),
            ("sigma", Sensitivity.variableResults(inputs, method.outputs, SensitivityMorris.sigma(_, _)).from(context))
          )

        OMROutputFormat.write(executionContext, output, sections, MetaData(method)).from(context)

        context


  /**
   * Describes a part of the space of inputs/ouputs of a model (or actually, puzzle)
   * on which to assess the sensitivity.
   * Describes that we wish to analyze the impact of variations of a given input on a given output,
   * and return it into variables indicatorMu, indicatorMu* and indicatorSigma.
   */
  case class SubspaceToAnalyze(
    input:  Val[Double],
    output: Val[Double]
  )

  object MorrisAggregation {

    /**
     * Variables produced by Morris analysis
     */
    val namespace = Namespace("morris")

    /**
     * Takes lists for trajectories:
     * factor:   [,    a,    b,  c,  ,   c,  a,  b]
     * deltas:   [0.,  0.2,  -1, 0.1,0,  -1, 0.1, 2]
     * so for each result, we are able to now which factor was changed compared to the previous one, and of how much
     *
     *
     * Also receives the list of all the ouputs to analyze:
     * List(
     *   o1:     [1,   2,    1,  0,   4, 1,  2,  3]
     *   o2:     [2,   1,    1,  0,  1,  3,  1,  2]
     *   ...
     * )
     *
     *
     *
     */

    def sumAbs(res: Double, b: Double): Double = res + Math.abs(b)

    def squaredDiff(ee: Double, mu: Double): Double = Math.pow(ee - mu, 2)

    /**
     * For a given model input named inputName,
     * analyze the output values outputValues
     * based on metadata passed by sampling which describes for each line of input
     * which factor was changed and of how much delta
     */
    def elementaryEffect(
      input:         Val[?],
      output:        Val[?],
      outputValues:  Array[Double],
      factorChanged: Array[String],
      deltas:        Array[Double]): (Double, Double, Double) =

      // indices of results in which the input had been changed
      val indicesWithEffect: Array[Int] = factorChanged.zipWithIndex
        .collect { case (name, idx) if (input.name == name) => idx }
      val r: Int = indicesWithEffect.length

      MorrisSampling.Log.logger.fine("measuring the elementary effects of " + input + " on " + output + " based on " + r + " repetitions")
      indicesWithEffect.foreach(idxChanged => MorrisSampling.Log.logger.fine("For a delta: " + deltas(idxChanged) + ", value changed from " + outputValues(idxChanged - 1) + " to " + outputValues(idxChanged) + " => " + (outputValues(idxChanged) - outputValues(idxChanged - 1)) / (deltas(idxChanged)) + ")"))

      // ... so we know each index - 1 leads to the case before changing the factor
      // elementary effects
      val elementaryEffects: Array[Double] = indicesWithEffect.map(idxChanged => (outputValues(idxChanged) - outputValues(idxChanged - 1)) / (deltas(idxChanged)))
      // leading to indicators
      val rD: Double = r.toDouble
      val mu: Double = elementaryEffects.sum / rD
      val muStar: Double = elementaryEffects.reduceLeft(sumAbs) / rD
      val sigma: Double = Math.sqrt(elementaryEffects.map(ee => squaredDiff(ee, mu)).sum / rD)

      MorrisSampling.Log.logger.fine("=> aggregate impact of " + input + " on " + output + ": mu=" + mu + ", mu*=" + muStar + ", sigma=" + sigma)

      (mu, muStar, sigma)


    /**
     * Takes from the user the list of subspaces to analyze,
     * that is tuples of (model input, model output) for which
     * indicators (mu, mu* and sigma) should be computed.
     * Receives at execution time the lists of
     */

    def apply[T](
      modelInputs:  Seq[ScalableValue],
      modelOutputs: Seq[Val[Double]])(implicit name: sourcecode.Name, definitionScope: DefinitionScope) =

      def morrisOutputs(
        modelInputs:  Seq[ScalableValue],
        modelOutputs: Seq[Val[Double]]) =
        for {
          i ← ScalableValue.prototypes(modelInputs)
          o ← modelOutputs
        } yield (i, o)

      val muOutputs = morrisOutputs(modelInputs, modelOutputs).map { case (i, o) => SensitivityMorris.mu(i, o) }
      val muStarOutputs = morrisOutputs(modelInputs, modelOutputs).map { case (i, o) => SensitivityMorris.muStar(i, o) }
      val sigmaOutputs = morrisOutputs(modelInputs, modelOutputs).map { case (i, o) => SensitivityMorris.sigma(i, o) }

      Task("MorrisAggregation") { p =>
        import p._

        // retrieve the metadata passed by the sampling method
        val factorChanged: Array[String] = context(MorrisSampling.varFactorName.toArray)
        val deltas: Array[Double] = context(MorrisSampling.varDelta.toArray)

        // for each part of the space we were asked to explore, compute the elementary effects and returns them
        // into the variables passed by the user
        val effects: Seq[Seq[Double]] =
          morrisOutputs(modelInputs, modelOutputs).map {
            case (input, output) =>
              val outputValues: Array[Double] = context(output.toArray)
              MorrisSampling.Log.logger.fine("Processing the elementary change for input " + input + " on " + output)
              val (mu, muStar, sigma) = elementaryEffect(input, output, outputValues, factorChanged, deltas)
              Seq[Double](mu, muStar, sigma)
          }.transpose

        def mu = effects(0)
        def muStar = effects(1)
        def sigma = effects(2)

        context ++
          (muOutputs zip mu).map { case (v, i) => Variable.unsecure(v, i) } ++
          (muStarOutputs zip muStar).map { case (v, i) => Variable.unsecure(v, i) } ++
          (sigmaOutputs zip sigma).map { case (v, i) => Variable.unsecure(v, i) }
      } set (
        // we expect as inputs:
        // ... the outputs of the model we want to analyze
        inputs ++= modelOutputs.map(_.array),

        // ... the metadata generated by the sampling
        inputs += (
          MorrisSampling.varFactorName.toArray,
          MorrisSampling.varDelta.toArray
        ),

          // we provide as outputs
          // ... our output indicators
          outputs ++= (muOutputs, muStarOutputs, sigmaOutputs)
      )

  }

  /**
   * A trajectory in the Morris OAT understanding,
   * that is a set of points in an N-dimensional spaces,
   * with a seed being the starting point, then each next
   * point changing only one variable of a given delta.
   * A raw trajectory is expressed with indices of factors
   * and a space of parameters in [0:1]
   */
  case class Trajectory(
    seed:          Array[Double],
    variableOrder: List[Int],
    deltas:        List[Double],
    points:        List[Array[Double]]
  )

  object MorrisSampling extends JavaLogger {

    implicit def isSampling: IsSampling[MorrisSampling] = s =>
      Sampling(
        s.apply(),
        s.outputs,
        s.inputs,
        s.validate
      )

    def apply(
      repetitions: FromContext[Int],
      levels:      FromContext[Int],
      factors:     Seq[ScalableValue]) =
      new MorrisSampling(repetitions, levels, factors)

    /**
     * Namespace for the variables peculiar to this Morris sampling
     */
    val namespace = Namespace("morrissampling")

    /**
     * The variable named like this contains the name of the factor which was changed
     * in a given point.
     */
    val varFactorName: Val[String] = Val[String]("factorname", namespace = namespace)
    val varDelta: Val[Double] = Val[Double]("delta", namespace = namespace)

    /**
     * For a given count of factors k, a number of levels p,
     * generates an initial seed for the k factors.
     * For instance for k=5 and p=4, it might return Array(0.25, 0.75, 0.25, 0.75, 0.5)
     */
    def seed(k: Int, p: Int, rng: scala.util.Random): Array[Double] = {
      val delta: Double = 1.0 / p.toDouble
      (1 to k).map(_ => (rng.nextInt(p - 1) + 1).toDouble * delta).toArray
    }

    /**
     * recursively builds trajectories based on a given delta, an initial seed,
     * arbitrary orders and directions for changing variables.
     * For instance, for delta=0.25, seed=(0.5,0.25,0.5),
     * order=(1,2,0) and directions (+1,+1,+1), it returns a list of hyperpoints (hyperpoint = one value per factor),
     * along with a list stating what factor was changed, and another stating how much change was induced.
     * List(
     *   Array(0.5, 0.5, 0.5)     factoridx=1     delta=-0.25
     *   Array(0.5, 0.5, 0.75)    factoridx=2     delta=+0.25
     *   Array(0.75, 0.5, 0.75)   factoridx=0     delta=+0.25
     *   )
     */
    @annotation.tailrec
    def trajectoryBuilder(delta: Double, order: List[Int], direction: List[Int], seed: Array[Double],
                          accPoints: List[Array[Double]] = List(),
                          accDeltas: List[Double]        = List()): (List[Array[Double]], List[Double]) = {

      if (order.isEmpty) (accPoints.reverse, accDeltas.reverse)
      else {
        val idx: Int = order.head
        val deltaOriented: Double = delta * direction.head
        val newSeed: Array[Double] = seed.updated(idx, seed(idx) + deltaOriented)
        //System.out.println("changing " + idx + "th element of " + deltaOriented)
        trajectoryBuilder(delta, order.tail, direction.tail, newSeed,
          newSeed :: accPoints, deltaOriented :: accDeltas)
      }
    }

    /**
     * Builds a trajectory for k variables sampled with p levels, builds a k+1 long trajectory.
     * For instance for k=3, p=4, it might return: TrajectoryRaw(
     *   seed = Array(0.25, 0.5, 0.75),
     *   variablesOrder = (2,1,0),
     *   deltas = (-0.25,+0.25,+0.25),
     *   points = List(
     *        Array(0.25, 0.5, 1.0)
     *        Array(0.25, 0.75, 1.0)
     *        Array(0.0, 0.75, 1.0))
     * )
     */
    def trajectory(k: Int, p: Int, rng: scala.util.Random): Trajectory = {
      val delta: Double = 1.0 / p.toDouble
      val seed: Array[Double] = MorrisSampling.seed(k, p, rng)
      val orderVariables: List[Int] = rng.shuffle(0 to k - 1).toList
      val directionVariables: List[Int] = Array.fill(k)(rng.nextInt(2) * 2 - 1).toList
      val (points, deltas) = trajectoryBuilder(delta, orderVariables, directionVariables, seed)
      Trajectory(seed, orderVariables, deltas, points)
    }

    /**
     * Generates r independent trajectories for k variables sampled with p levels.
     */
    @annotation.tailrec
    def trajectories(k: Int, p: Int, r: Int, rng: scala.util.Random, acc: List[Trajectory] = List()): List[Trajectory] = {
      if (r == 0) return acc
      else trajectories(k, p, r - 1, rng, trajectory(k, p, rng) :: acc)
    }

  }

  import MorrisSampling.Log._

  sealed class MorrisSampling(
    val repetitions: FromContext[Int],
    val levels:      FromContext[Int],
    val factors:     Seq[ScalableValue]) {

    def validate = repetitions.validate ++ levels.validate

    def inputs = factors.flatMap(_.inputs)
    def outputs = factors.map { _.prototype } ++ Seq(
      MorrisSampling.varDelta,
      MorrisSampling.varFactorName)

    /**
     * Converts a raw trajectory to a list of lists of variables (points to run)
     * to execute in a given context. Replaces the index of factor by the variable name,
     * and scales each of the points in a n-dimensional [0:1] space into their actual range.
     */
    def trajectoryToVariables(t: Trajectory, idTraj: Int): FromContext[List[List[Variable[?]]]] = FromContext { p =>

      import p._
      // forge the list of variables for the first run (reference run)
      val variablesForRefRun: List[Variable[?]] = List(
        Variable(MorrisSampling.varFactorName, ""),
        Variable(MorrisSampling.varDelta, 0.0)
      ) ++ ScalableValue.toVariables(factors, t.seed).from(context)

      // forge lists of lists of variables for the runs of the trajectory
      val variablesForElementaryEffects = 
        (t.points zip t.deltas zip t.variableOrder.zipWithIndex).map {
          case ((point, delta), order2idx) =>
            val factoridx = order2idx._1
            val iterationId = order2idx._2 + 1
            List(
              Variable(MorrisSampling.varFactorName, factors(factoridx).prototype.name),
              Variable(MorrisSampling.varDelta, point(factoridx) - t.seed(factoridx))
            ) ++ ScalableValue.toVariables(factors, point).from(context)
        }

      variablesForRefRun :: variablesForElementaryEffects

    }

    def apply(): FromContext[Iterator[Iterable[Variable[?]]]] = FromContext { ctxt =>
      import ctxt._
      val r: Int = repetitions.from(context)
      val p: Int = levels.from(context)
      val k: Int = factors.length

      val trajectoriesRaw: List[Trajectory] = MorrisSampling.trajectories(k, p, r, random())

      logger.fine("should explore a " + k + "-dimensions " +
        p + "-levels grid with " +
        r + " repetitions => " +
        trajectoriesRaw.length + " trajectories")

      trajectoriesRaw.zipWithIndex.flatMap {
        case (t, idTraj) =>
          trajectoryToVariables(t, idTraj).from(context)
      }.iterator
    }

  }

  implicit def method: ExplorationMethod[SensitivityMorris, Method] = m => {
    implicit def defScope: DefinitionScope = m.scope

    // the sampling for Morris is a One At a Time one,
    // with respect to the user settings for repetitions, levels and inputs
    val sampling = MorrisSampling(m.sample, m.level, m.inputs)

    // the aggregation obviously is a Morris aggregation!
    // it collects all the specific inputs added from the sampling
    // to interpret the results
    val aggregation = MorrisAggregation(m.inputs, m.outputs)

    val w =
      MapReduce(
        evaluation = m.evaluation,
        sampler = ExplorationTask(sampling),
        aggregation = aggregation
      )

    def validate =
      val nonContinuous = m.inputs.filter(v => !ScalableValue.isContinuous(v))
      if nonContinuous.nonEmpty
      then Seq(new UserBadDataError(s"Factor of Morris should be continuous values, but some are not: ${nonContinuous.map(_.prototype.quotedString).mkString(", ")}"))
      else Seq()

    DSLContainer(w, method = Method(m.inputs, m.outputs), validate = validate)
  }

}

/**
 * A Morris Sensitivity Analysis takes a puzzle (a model) that we want to analyse,
 * the list of the inputs (and their ranges), the list of outputs we want
 * to test the sensitivity of the inputs on, how many repetitions to conduct,
 * and in how many levels inputs should be analyzed on.
 *
 * The sensitivity analysis is driven as an exploration based on the Morris sampling,
 * running the model, and aggregating the result to produce the sensitivity outputs.
 */
case class SensitivityMorris(
  evaluation: DSL,
  inputs:     Seq[ScalableValue],
  outputs:    Seq[Val[Double]],
  sample:     Int,
  level:      Int,
  scope:      DefinitionScope               = "sensitivity morris")

