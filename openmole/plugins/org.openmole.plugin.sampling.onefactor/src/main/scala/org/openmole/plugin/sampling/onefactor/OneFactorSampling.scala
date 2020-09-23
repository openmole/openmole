
package org.openmole.plugin.sampling.onefactor

import org.openmole.core.context.{ PrototypeSet, Val, Variable }
import org.openmole.core.expansion.FromContext
import org.openmole.core.workflow.domain.FiniteFromContext
import org.openmole.core.workflow.sampling.{ ExplicitSampling, Factor, Sampling }

case class NominalFactor[D, T](factor: Factor[D, T], nominalValue: T, values: FiniteFromContext[D, T]) {
  def prototype: Val[T] = factor.value
  def nominalVal: Sampling = ExplicitSampling(prototype, Seq(nominalValue))
}

object OneFactorSampling {
  def apply(factors: NominalFactor[_, _]*) = new OneFactorSampling(factors: _*)
}

case class OneFactorSampling(factors: NominalFactor[_, _]*) extends Sampling {

  override def inputs: PrototypeSet = PrototypeSet.empty

  override def prototypes: Iterable[Val[_]] = factors.map { _.prototype }

  override def apply() = FromContext {
    p ⇒
      import p._
      if (factors.isEmpty) Iterator.empty
      else factors.toIterator.flatMap { n: NominalFactor[_, _] ⇒ oneFactorSampling(n).from(context) }
  }

  /**
   * Sampling along one of parameters
   * @param n
   * @return
   */
  def oneFactorSampling[D, T](n: NominalFactor[D, T]): FromContext[Iterator[Iterable[Variable[_]]]] = FromContext {
    p ⇒
      import p._
      val fullsampling: Sampling = ExplicitSampling(n.prototype, n.values.computeValues(n.factor.domain).from(context))
      complete(Seq(fullsampling) ++ factors.filter(!_.equals(n)).map { n ⇒ n.nominalVal }).from(context)
  }

  /**
   * complete sampling
   * @param samplings
   * @return
   */
  def complete(samplings: Seq[Sampling]): FromContext[Iterator[Iterable[Variable[_]]]] = FromContext { p ⇒
    import p._
    samplings.tail.foldLeft(samplings.head().from(context)) {
      (a, b) ⇒ combine(a, b).from(context)
    }
  }

  /**
   * combination for the complete sampling
   * @param s1
   * @param s2
   * @return
   */
  def combine(s1: Iterator[Iterable[Variable[_]]], s2: Sampling): FromContext[Iterator[Iterable[Variable[_]]]] = FromContext { p ⇒
    import p._
    for (x ← s1; y ← s2().from(context ++ x)) yield x ++ y
  }

}
