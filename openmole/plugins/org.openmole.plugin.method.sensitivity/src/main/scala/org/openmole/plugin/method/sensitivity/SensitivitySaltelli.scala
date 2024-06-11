package org.openmole.plugin.method.sensitivity

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

import org.openmole.core.dsl
import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*
import org.openmole.core.workflow.sampling.ScalableValue
import org.openmole.plugin.sampling.lhs.LHS
import org.openmole.plugin.sampling.quasirandom.SobolSampling
import org.openmole.plugin.tool.pattern.MapReduce

object SensitivitySaltelli {

  def methodName = MethodMetaData.name(SensitivitySaltelli)

  def firstOrder(input: Val[_], output: Val[_]) = input.withNamespace(Namespace("firstOrder", output.name))
  def totalOrder(input: Val[_], output: Val[_]) = input.withNamespace(Namespace("totalOrder", output.name))


  object MetaData:
    import io.circe.*

    given Codec[MetaData] = Codec.AsObject.derivedConfigured

    def apply(method: Method) =
      new MetaData(
        inputs = method.inputs.map(_.prototype).map(ValData.apply),
        outputs = method.outputs.map(ValData.apply)
      )

  case class MetaData(inputs: Seq[ValData], outputs: Seq[ValData])

    given MethodMetaData[MetaData] = MethodMetaData(SensitivitySaltelli.methodName)

  case class Method(inputs: Seq[ScalableValue], outputs: Seq[Val[_]])

  given ExplorationMethod[SensitivitySaltelli, Method] = p =>
    implicit def defScope: DefinitionScope = p.scope

    val sampling = SaltelliSampling(p.sample, p.inputs: _*)

    val aggregation =
      SaltelliAggregation(
        modelInputs = p.inputs,
        modelOutputs = p.outputs,
      ) set (dsl.inputs += (SaltelliSampling.matrixName.array, SaltelliSampling.matrixIndex.array))

    val w =
      MapReduce(
        evaluation = p.evaluation,
        sampler = ExplorationTask(sampling),
        aggregation = aggregation
      )

    def validate =
      val nonContinuous = p.inputs.filter(v => !ScalableValue.isContinuous(v))
      if nonContinuous.nonEmpty
      then Seq(new UserBadDataError(s"Factor of Saltelli should be continuous values, but some are not: ${nonContinuous.map(_.prototype.quotedString).mkString(", ")}"))
      else Seq()

    DSLContainer(w, method = Method(p.inputs, p.outputs), validate = validate)


  object SaltelliAggregation {

    val namespace = Namespace("saltelli")

    /**
     * Compute the first and total order effects from the given output values
     * of a model. From Saltelli 2010 Variance based sensitivity analysis of model output.
     * fA(j) contains the model output value for the j-th row of matrix A (see paper),
     * fB(j) contains the model output value for the j-th row of B,
     * fC(i)(j) contains the model output for the j-th row of matrix
     * C^i (the matrix where all columns are from B except the i-th which
     * is from A).
     */
    def firstAndTotalOrderIndices(
      fA: Array[Double],
      fB: Array[Double],
      fC: Array[Array[Double]]): (Array[Double], Array[Double]) = {
      val NB = fB.size
      val k = fC.size //Number of parameters
      val f02 = math.pow(fB.sum / NB.toDouble, 2)
      // val varY = fB.map(fBj ⇒ math.pow(fBj, 2)).sum / NB.toDouble - f02
      val f0 = fB.sum / NB.toDouble
      val varY = fB.map(fBj ⇒ math.pow(fBj - f0, 2)).sum / NB.toDouble
      def avgProduct(u: Array[Double], v: Array[Double]): Double = {
        val prods = (u zip v).map({ case (uj, vj) ⇒ uj * vj })
        prods.sum / prods.size.toDouble
      }

      val firstOrderEffects = (1 to k).map { i ⇒
      {
        val sumTerms = (fA zip fB zip fC(i - 1)).map {
          case ((fAj, fBj), fCij) ⇒ fBj * (fCij - fAj)
        }
        val N = sumTerms.size
        (sumTerms.sum / N) / varY
      }
      }.toArray

      val totalOrderEffects = (1 to k).map { i ⇒
      {
        val squaredDiff = (fA zip fC(i - 1)).map {
          case (fAj, fCij) ⇒ math.pow(fAj - fCij, 2)
        }
        val N = squaredDiff.size
        (squaredDiff.sum / (2.0 * N)) / varY
      }
      }.toArray

      (firstOrderEffects, totalOrderEffects)
    }

    def apply(
      modelInputs:  Seq[ScalableValue],
      modelOutputs: Seq[Val[Double]],
      firstOrderSI: Val[Array[Array[Double]]]     = Val[Array[Array[Double]]]("firstOrderSI"),
      totalOrderSI: Val[Array[Array[Double]]]     = Val[Array[Array[Double]]]("totalOrderSI"))(implicit name: sourcecode.Name, definitionScope: DefinitionScope) = {

      def saltelliOutputs(
        modelInputs:  Seq[ScalableValue],
        modelOutputs: Seq[Val[Double]]) =
        for {
          o ← modelOutputs
          i ← ScalableValue.prototypes(modelInputs)
        } yield (i, o)

      val fOOutputs = saltelliOutputs(modelInputs, modelOutputs).map { case (i, o) ⇒ SensitivitySaltelli.firstOrder(i, o) }
      val tOOutputs = saltelliOutputs(modelInputs, modelOutputs).map { case (i, o) ⇒ SensitivitySaltelli.totalOrder(i, o) }

      Task("SaltelliAggregation") { p ⇒
        import p._

        val matrixNames: Array[String] = context(SaltelliSampling.matrixName.array)
        val matrixIndex: Array[Int] = context(SaltelliSampling.matrixIndex.array)
        // outputValues(i)(j) gives the j-th value for output i.
        val outputValues: Array[Array[Double]] =
          modelOutputs.map(o ⇒ context(o.toArray)).toArray

        // reindex(n)(i)(j) gives the i-th model output where the model is given as input the line j of matrix n.
        val reindex: Map[String, Array[Array[Double]]] =
          (matrixNames zip (matrixIndex zip (outputValues.transpose[Double])))
            .groupBy { case (n, _) ⇒ n }
            .view.mapValues { xs ⇒
            xs.sortBy { case (_, (i, _)) ⇒ i }
              .map { case (_, (_, v)) ⇒ v }
              .transpose[Double]
          }.toMap

        // Output value for each matrix. fA(i)(j) and fB(i)(j) gives the value of i-th model output evaluated on the j-th line of matrix A. fC(i)(k)(j) gives the value of the i-th model output evaluated on the j-th line of matrix Ck.
        val fA: Array[Array[Double]] = reindex("a")
        val fB: Array[Array[Double]] = reindex("b")
        val fC: Array[Array[Array[Double]]] =
          modelInputs.map { i ⇒ reindex("c$" ++ i.prototype.name) }.toArray.transpose

        // ftoi(o)._1(i) contains first order index for input i on output o.
        // ftoi(o)._2(i) contains total order index for input i on output o.
        val ftoi: Array[(Array[Double], Array[Double])] =
        (fA zip fB zip fC).map { case ((fAo, fBo), fCo) ⇒ firstAndTotalOrderIndices(fAo, fBo, fCo) }

        // first order indices
        // fosi(o)(i) contains first order index for input i on output o.
        val fosi = ftoi.map { _._1.toArray }.toArray

        // total order indices
        // tosi(o)(i) contains total order index for input i on output o.
        val tosi = ftoi.map { _._2.toArray }.toArray

        val fosiv =
          for {
            (on, oi) ← modelOutputs.zipWithIndex
            v ← ScalableValue.toVariables(modelInputs, fosi(oi), scale = false).from(context)
          } yield v.value

        val tosiv =
          for {
            (on, oi) ← modelOutputs.zipWithIndex
            v ← ScalableValue.toVariables(modelInputs, tosi(oi), scale = false).from(context)
          } yield v.value

        context ++
          (fOOutputs zip fosiv).map { case (fo, v) ⇒ Variable.unsecure(fo, v) } ++
          (tOOutputs zip tosiv).map { case (to, v) ⇒ Variable.unsecure(to, v) }

      } set (
        dsl.inputs ++= modelOutputs.map(_.array),
        dsl.outputs ++= (fOOutputs, tOOutputs)
      )
    }

  }


  object SaltelliHook:

    def apply(method: Method, output: WritableOutput)(implicit name: sourcecode.Name, definitionScope: DefinitionScope) =
      Hook("SaltelliHook"): p ⇒
        import p._
        import WritableOutput._

        val inputs = ScalableValue.prototypes(method.inputs)

        import OutputFormat.*

        def sections =
          OutputContent(
            "firstOrderIndices" -> Sensitivity.variableResults(inputs, method.outputs, SensitivitySaltelli.firstOrder(_, _)).from(context),
            "totalOrderIndices" -> Sensitivity.variableResults(inputs, method.outputs, SensitivitySaltelli.totalOrder(_, _)).from(context)
          )

        OMROutputFormat.write(executionContext, output, sections, MetaData(method)).from(context)

        context


  object SaltelliSampling {

    val namespace = Namespace("saltelli")
    val matrixName = org.openmole.core.context.Val[String]("matrix", namespace = namespace)
    val matrixIndex = org.openmole.core.context.Val[Int]("index", namespace = namespace)

    def matrix = Seq(matrixName, matrixIndex)

    implicit def isSampling: IsSampling[SaltelliSampling] = saltelli =>
      def apply = FromContext { p =>
        import p._

        val s = saltelli.samples.from(context)
        val vectorSize = saltelli.factors.map(_.size(context)).sum
        val isSobol = saltelli.sobolSampling.from(context)

        val (a, b) =
          if (isSobol) {
            val ab = SobolSampling.sobolValues(2 * vectorSize, s).map(_.toArray).toArray
            (ab.transpose.take(vectorSize).transpose, ab.transpose.takeRight(vectorSize).transpose)
          }
          else (LHS.lhsValues(vectorSize, s, random()), LHS.lhsValues(vectorSize, s, random()))

        val cIndices =
          for {
            f ← saltelli.factors
            j ← (0 until f.size(context))
          } yield (f, j, f.isScalar)

        def toVariables(
          matrix: Array[Array[Double]],
          m:      Namespace): List[Iterable[Variable[_]]] =
          matrix.zipWithIndex.map {
            case (l, index) ⇒
              def line = ScalableValue.toVariables(saltelli.factors, l).from(context)
              Variable(SaltelliSampling.matrixName, m.toString) :: Variable(SaltelliSampling.matrixIndex, index) :: line
          }.toList

        def aVariables = toVariables(a, Namespace("a"))
        def bVariables = toVariables(b, Namespace("b"))

        def cVariables =
          cIndices.zipWithIndex.flatMap {
            case ((f, j, scalar), i) ⇒
              val matrixName =
                if (scalar) Namespace("c", f.prototype.name)
                else Namespace("c", j.toString, f.prototype.name)

              toVariables(
                SaltelliSampling.buildC(i, a, b),
                matrixName
              )
          }

        (aVariables ++ bVariables ++ cVariables).iterator

      }

      Sampling(
        apply,
        saltelli.factors.map(_.prototype) ++ matrix,
        saltelli.factors.flatMap(_.inputs),
        saltelli.samples.validate
      )


    def apply(samples: FromContext[Int], factors: ScalableValue*): SaltelliSampling =
      new SaltelliSampling(samples, true, factors: _*)

    def buildC(
      i: Int,
      a: Array[Array[Double]],
      b: Array[Array[Double]]) =
      a zip b map {
        case (lineOfA, lineOfB) ⇒ buildLineOfC(i, lineOfA, lineOfB)
      }

    def buildLineOfC(i: Int, lineOfA: Array[Double], lineOfB: Array[Double]) =
      (lineOfA zip lineOfB zipWithIndex) map {
        case ((a, b), index) ⇒ if (index == i) b else a
      }

  }

  case class SaltelliSampling(samples: FromContext[Int], sobolSampling: FromContext[Boolean], factors: ScalableValue*)


}


/**
 * Variance-based sensitivity indices (Saltelli method).
 *   Saltelli, A., Annoni, P., Azzini, I., Campolongo, F., Ratto, M., & Tarantola, S. (2010). Variance based sensitivity analysis of model output. Design and estimator for the total sensitivity index. Computer Physics Communications, 181(2), 259-270.
 *
 * @param evaluation
 * @param inputs input prototypes
 * @param outputs outputs double prototypes
 * @param sample number of samples to estimates sensitivity indices
 * @param scope
 * @return
 */
case class SensitivitySaltelli(
  evaluation:   DSL,
  inputs:  Seq[ScalableValue],
  outputs: Seq[Val[Double]],
  sample:      FromContext[Int],
  scope: DefinitionScope = "sensitivity saltelli")
