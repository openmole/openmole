package org.openmole.plugin.method.sensitivity

import org.openmole.plugin.sampling.lhs._
import org.openmole.plugin.sampling.quasirandom._
import org.openmole.core.dsl.extension._

object SaltelliSampling {

  val namespace = Namespace("saltelli")
  val matrixName = org.openmole.core.context.Val[String]("matrix", namespace = namespace)
  val matrixIndex = org.openmole.core.context.Val[Int]("index", namespace = namespace)

  def matrix = Seq(matrixName, matrixIndex)

  def apply(samples: FromContext[Int], sobolSampling: FromContext[Boolean], factors: ScalarOrSequenceOfDouble[_]*) =
    Sampling { p ⇒
      import p._
      val s = samples.from(context)
      val vectorSize = factors.map(_.size(context)).sum
      val isSobol = sobolSampling.from(context)

      val (a, b) = if (isSobol) {
        val ab = SobolSampling.sobolValues(2 * vectorSize, s).map(_.toArray).toArray
        (ab.transpose.take(vectorSize).transpose, ab.transpose.takeRight(vectorSize).transpose)
      }
      else
        (LHS.lhsValues(vectorSize, s, random()), LHS.lhsValues(vectorSize, s, random()))

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
    } validate { samples } inputs { factors.flatMap(_.inputs) } prototypes { factors.map(_.prototype) }

  def apply(samples: FromContext[Int], factors: ScalarOrSequenceOfDouble[_]*): FromContextSampling =
    SaltelliSampling(samples, true, factors: _*)

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

}
