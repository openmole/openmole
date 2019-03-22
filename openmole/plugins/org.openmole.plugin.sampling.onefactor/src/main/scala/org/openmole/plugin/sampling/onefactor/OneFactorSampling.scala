
package org.openmole.plugin.sampling.onefactor

import org.openmole.core.context.{ PrototypeSet, Val, Variable }
import org.openmole.core.expansion.FromContext
import org.openmole.core.workflow.domain.Finite
import org.openmole.core.workflow.sampling.{ ExplicitSampling, Factor, FactorSampling, Sampling }

case class NominalFactor[D, T](factor: Factor[D, T], nominalValue: T, values: Finite[D, T]) {

  def inputs: PrototypeSet = Seq(factor.value)
  def prototype: Val[T] = factor.value

  def toValuesSampling: Sampling = FactorSampling[D, T](factor)(values)

  def toNominalSampling: Sampling = ExplicitSampling[T](prototype, Seq(nominalValue))

}

object OneFactorSampling {
  def apply(factors: NominalFactor[_, _]*) = new OneFactorSampling(factors: _*)
}

case class OneFactorSampling(factors: NominalFactor[_, _]*) extends Sampling {

  override def inputs: PrototypeSet = PrototypeSet.empty ++ factors.toSeq.flatMap { _.inputs }

  override def prototypes: Iterable[Val[_]] = factors.map { _.prototype }

  override def apply(): FromContext[Iterator[Iterable[Variable[_]]]] = FromContext { p ⇒
    import p._
    if (factors.isEmpty) Iterator.empty
    else
      factors.toIterator.flatMap(oneFactorSampling(_).from(context))
  }

  /**
    * Sampling along one of parameters
    *   (the FromContext is needed as NominalFactor function return Sampling, since they use core sampling primitive)
    * @param n
    * @return
    */
  def oneFactorSampling(n: NominalFactor[_, _]): FromContext[Iterator[Iterable[Variable[_]]]] = FromContext {
    p =>
    import p._
    complete(Seq(n.toValuesSampling().from(context)) ++ factors.filter(!_.equals(n)).map(_.toNominalSampling().from(context)))
  }

  /**
    * complete sampling
    * @param samplings
    * @return
    */
  def complete(samplings: Seq[Iterator[Iterable[Variable[_]]]]): Iterator[Iterable[Variable[_]]] = samplings.reduceLeft(combine)

  /**
    * combination for the complete sampling
    * @param s1
    * @param s2
    * @return
    */
  def combine(s1: Iterator[Iterable[Variable[_]]], s2:  Iterator[Iterable[Variable[_]]]): Iterator[Iterable[Variable[_]]] = for (x ← s1; y ← s2) yield x ++ y

}
