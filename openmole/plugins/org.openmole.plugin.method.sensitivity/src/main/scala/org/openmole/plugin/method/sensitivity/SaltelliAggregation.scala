package org.openmole.plugin.method.sensitivity

import org.openmole.core.context.{ Namespace, Variable }
import org.openmole.core.outputmanager.OutputManager
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.tools.ScalarOrSequenceOfDouble
import org.openmole.core.dsl
import org.openmole.core.dsl._

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
    modelInputs:  Seq[ScalarOrSequenceOfDouble[_]],
    modelOutputs: Seq[Val[Double]],
    firstOrderSI: Val[Array[Array[Double]]]        = Val[Array[Array[Double]]]("firstOrderSI"),
    totalOrderSI: Val[Array[Array[Double]]]        = Val[Array[Array[Double]]]("totalOrderSI"))(implicit name: sourcecode.Name, definitionScope: DefinitionScope) = {

    def saltelliOutputs(
      modelInputs:  Seq[ScalarOrSequenceOfDouble[_]],
      modelOutputs: Seq[Val[Double]]) =
      for {
        o ← modelOutputs
        i ← ScalarOrSequenceOfDouble.prototypes(modelInputs)
      } yield (i, o)

    val fOOutputs = saltelliOutputs(modelInputs, modelOutputs).map { case (i, o) ⇒ Saltelli.firstOrder(i, o) }
    val tOOutputs = saltelliOutputs(modelInputs, modelOutputs).map { case (i, o) ⇒ Saltelli.totalOrder(i, o) }

    FromContextTask("SaltelliAggregation") { p ⇒
      import p._

      val matrixNames: Array[String] =
        context(SaltelliSampling.matrixName.array)
      val matrixIndex: Array[Int] =
        context(SaltelliSampling.matrixIndex.array)
      // outputValues(i)(j) gives the j-th value for output i.
      val outputValues: Array[Array[Double]] =
        modelOutputs.map(o ⇒ context(o.toArray)).toArray

      // reindex(n)(i)(j) gives the i-th model output where the model is given as input the line j of matrix n.
      val reindex: Map[String, Array[Array[Double]]] =
        (matrixNames zip (matrixIndex zip (outputValues.transpose[Double])))
          .groupBy { case (n, _) ⇒ n }
          .mapValues { xs ⇒
            xs.sortBy { case (_, (i, _)) ⇒ i }
              .map { case (_, (_, v)) ⇒ v }
              .transpose[Double]
          }

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
          v ← ScalarOrSequenceOfDouble.unflatten(modelInputs, fosi(oi), scale = false).from(context)
        } yield v.value

      val tosiv =
        for {
          (on, oi) ← modelOutputs.zipWithIndex
          v ← ScalarOrSequenceOfDouble.unflatten(modelInputs, tosi(oi), scale = false).from(context)
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

