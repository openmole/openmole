
package org.openmole.plugin.sampling.onefactor

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.core.workflow.domain.DiscreteFromContext
import org.openmole.plugin.domain.collection._
import org.openmole.plugin.sampling.combine._

case class NominalFactor[D, T](factor: Factor[D, T], nominalValue: T, values: DiscreteFromContext[D, T]) {
  def nominalSampling: Sampling = factor.value in Seq(nominalValue)
  def prototype = factor.value
}

object OneFactorSampling {
  def apply(factors: NominalFactor[_, _]*) = new OneFactorSampling(factors: _*)

  implicit def isSampling = new IsSampling[OneFactorSampling] {
    override def validate(s: OneFactorSampling): Validate = Validate.success
    override def inputs(s: OneFactorSampling): PrototypeSet = Seq()
    override def outputs(s: OneFactorSampling): Iterable[Val[_]] = s.factors.map { _.prototype }
    override def apply(s: OneFactorSampling): FromContext[Iterator[Iterable[Variable[_]]]] = FromContext {
      p ⇒
        import p._
        if (s.factors.isEmpty) Iterator.empty
        else s.factors.iterator.flatMap { n ⇒ oneFactorSampling(n, s.factors).from(context) }
    }
  }

  /**
   * Sampling along one of parameters
   * @param n
   * @return
   */
  def oneFactorSampling[D, T](n: NominalFactor[D, T], factors: Seq[NominalFactor[_, _]]): FromContext[Iterator[Iterable[Variable[_]]]] = FromContext { p ⇒
    import p._
    val exploreSampling: Sampling = n.prototype in n.values.iterator(n.factor.domain).from(context).toSeq
    val nominalSampling: Seq[Sampling] = factors.filter(!_.equals(n)).map { n ⇒ n.nominalSampling }

    CompleteSampling(Seq(exploreSampling) ++ nominalSampling: _*)().from(context)
  }

}

case class OneFactorSampling(factors: NominalFactor[_, _]*)