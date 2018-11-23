package org.openmole.plugin.method.sensitivity

import org.openmole.core.context.Namespace
import org.openmole.core.outputmanager.OutputManager
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.tools.ScalarOrSequenceOfDouble
import org.openmole.core.dsl
import org.openmole.core.dsl._

object SaltelliAggregation {

  val namespace = Namespace("saltelli")

  // SensitivityIndices
  val firstOrderSI = Val[Array[Double]]("firstOrderSI", namespace = namespace)
  val totalOrderSI = Val[Array[Double]]("totalOrderSI", namespace = namespace)

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
    fA: Seq[Option[Double]],
    fB: Seq[Option[Double]],
    fC: Seq[Seq[Option[Double]]]): (Seq[Double], Seq[Double]) = {
    val fBSome = fB.collect { case Some(i) ⇒ i }
    val NB = fBSome.size
    val k = fC.size //Number of parameters
    val f02 = math.pow(fBSome.sum / NB.toDouble, 2)
    val varY = fBSome.map(fBj ⇒ math.pow(fBj, 2)).sum / NB.toDouble - f02
    def avgProduct(u: Seq[Option[Double]], v: Seq[Option[Double]]): Double = {
      val prods = (u zip v).collect({ case (Some(uj), Some(vj)) ⇒ uj * vj })
      prods.sum / prods.size.toDouble
    }

    val firstOrderEffects = (1 to k).map { i ⇒
      {
        val sumTerms = (fA zip fB zip fC(i - 1)).collect {
          case ((Some(fAj), Some(fBj)), Some(fCij)) ⇒ fBj * (fCij - fAj)
        }
        val N = sumTerms.size
        (sumTerms.sum / N) / varY
      }
    }.toVector

    val totalOrderEffects = (1 to k).map { i ⇒
      {
        val squaredDiff = (fA zip fC(i - 1)).collect {
          case (Some(fAj), Some(fCij)) ⇒ math.pow(fAj - fCij, 2)
        }
        val N = squaredDiff.size
        (squaredDiff.sum / (2.0 * N)) / varY
      }
    }.toVector

    (firstOrderEffects, totalOrderEffects)
  }

  // def totalOrder(a: Seq[Double], b: Seq[Double], c: Seq[Double]) = {
  //   val n = a.size

  //   val bxcAvg = (b zip c map { case (b, c) ⇒ b * c } sum) / n

  //   val axaAvg = (a map { a ⇒ a * a } sum) / n
  //   val f0 = (a sum) / n

  //   1 - (bxcAvg - math.pow(f0, 2)) / (axaAvg - math.pow(f0, 2))
  // }

  def apply(inputs: Seq[ScalarOrSequenceOfDouble[_]], outputs: Seq[Val[Double]])(implicit name: sourcecode.Name, definitionScope: DefinitionScope) =
    ClosureTask("SaltelliAggregation") { (context, _, _) ⇒

      //OutputManager.systemOutput.println()

      println(context)
      val matrixNames: Array[String] =
        context(SaltelliSampling.matrixName.array)
      // an array of a,b,c for each pair of inputs
      val fA: Seq[Array[Double]] = outputs.map(o ⇒ context(o.toArray))

      def indices(names: Array[String], value: String) = (names zipWithIndex).filter(_._1 == value).map(_._2)

      //indices(matrixNames, "a").map()

      context +
        (SaltelliAggregation.firstOrderSI, Array(0.0, 1.7, 9.8)) +
        (SaltelliAggregation.totalOrderSI, Array(8.7, 8.7, 6.5))

    } set (
      dsl.inputs ++= outputs.map(_.array)
    )

}

