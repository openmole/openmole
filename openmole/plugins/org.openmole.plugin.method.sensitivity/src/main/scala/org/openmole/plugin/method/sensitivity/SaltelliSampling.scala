package org.openmole.plugin.method.sensitivity

import org.openmole.core.context._
import org.openmole.core.expansion.FromContext
import org.openmole.core.workflow.sampling.Sampling
import org.openmole.core.workflow.tools._
import org.openmole.plugin.sampling.lhs._

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
      case ((a, b), index) ⇒ if (index == i) b else a
    }

  val namespace = Namespace("saltelli")
  val matrixName = Val[String]("matrix", namespace = namespace)
  val matrixIndex = Val[Int]("index", namespace = namespace)

  def matrix = Seq(matrixName, matrixIndex)

  def apply(samples: FromContext[Int], factors: ScalarOrSequenceOfDouble[_]*) =
    new SaltelliSampling(samples, factors: _*)

}

class SaltelliSampling(val samples: FromContext[Int], val factors: ScalarOrSequenceOfDouble[_]*) extends Sampling {

  override def inputs = factors.flatMap(_.inputs)
  override def prototypes = factors.map { _.prototype } ++ SaltelliSampling.matrix

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
      m:      Namespace
    ): List[Iterable[Variable[_]]] =
      matrix.zipWithIndex.map {
        case (l, index) ⇒
          def line = ScalarOrSequenceOfDouble.unflatten(factors, l).from(context)
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

    (aVariables ++ bVariables ++ cVariables).toIterator
  }
}
