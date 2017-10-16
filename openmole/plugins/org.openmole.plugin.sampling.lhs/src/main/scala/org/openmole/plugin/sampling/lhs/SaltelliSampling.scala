package org.openmole.plugin.sampling.lhs

import org.openmole.core.context._
import org.openmole.core.expansion.FromContext
import org.openmole.core.workflow.sampling.Sampling
import org.openmole.core.workflow.tools._

object SaltelliSampling {

  def buildC(
    i: Int,
    a: Array[Array[Double]],
    b: Array[Array[Double]]
  ) =
    a zip b map {
      case (lineOfA, lineOfB) ⇒ buildLineOfC(i, lineOfA, lineOfB)
    }

  def buildLineOfC(i: Int, lineOfA: Array[Double], lineOfB: Array[Double]) =
    (lineOfA zip lineOfB zipWithIndex) map {
      case ((a, b), index) ⇒ if (index == i) a else b
    }

  val namespace = Namespace("saltelli")
  val matrixName = Val[String]("matrix", namespace = namespace)
  val matrixIndice = Val[Int]("indice", namespace = namespace)

  def matrix = Seq(matrixName, matrixIndice)

  def apply(samples: FromContext[Int], factors: ScalarOrSequence[_]*) =
    new SaltelliSampling(samples, factors: _*)

}

class SaltelliSampling(val samples: FromContext[Int], val factors: ScalarOrSequence[_]*) extends Sampling {

  override def inputs = factors.flatMap(_.inputs)
  override def prototypes = factors.map { _.prototype }

  override def apply() = FromContext { p ⇒
    import p._
    val s = samples.from(context)
    val vectorSize = factors.map(_.size(context)).sum
    val a = LHS.lhsValues(vectorSize, s, random())
    val b = LHS.lhsValues(vectorSize, s, random())

    val cIndices =
      for {
        f ← factors
        j ← (0 until f.size(context))
      } yield (f, j, f.isScalar)

    def toVariables(
      matrix: Array[Array[Double]],
      m:      String
    ): List[Iterable[Variable[_]]] =
      matrix.zipWithIndex.map {
        case (l, index) ⇒
          def line = ScalarOrSequence.scaled(factors, l).from(context)
          Variable(SaltelliSampling.matrixName, m) :: Variable(SaltelliSampling.matrixIndice, index) :: line
      }.toList

    def aVariables = toVariables(a, "a")
    def bVariables = toVariables(b, "b")

    def cVariables =
      cIndices.zipWithIndex.flatMap {
        case ((f, j, scalar), i) ⇒
          val matrixName =
            if (scalar) f.prototype.withNamespace(Namespace("c")).name
            else f.prototype.withNamespace(Namespace("c", j.toString)).name

          toVariables(
            SaltelliSampling.buildC(i, a, b),
            matrixName
          )
      }

    (aVariables ++ bVariables ++ cVariables).toIterator
  }

}
