
package org.openmole.plugin.sampling.onefactor

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.core.workflow.domain.DiscreteFromContextDomain
import org.openmole.plugin.domain.collection.{*, given}
import org.openmole.plugin.sampling.combine.{*, given}

case class NominalFactor[D, T](factor: Factor[D, T], nominalValue: T, values: DiscreteFromContextDomain[D, T]) {
  def nominalSampling: Sampling = factor.value in Seq(nominalValue)
  def prototype = factor.value
}

object OneFactorSampling {
  def apply(factors: NominalFactor[_, _]*) = new OneFactorSampling(factors *)

  implicit def isSampling: IsSampling[OneFactorSampling] = s ⇒ {
    def validate: Validate = Validate.success
    def inputs: PrototypeSet = Seq()
    def outputs: Iterable[Val[?]] = s.factors.map { _.prototype }
    def apply: FromContext[Iterator[Iterable[Variable[?]]]] = FromContext {
      p ⇒
        import p._
        if (s.factors.isEmpty) Iterator.empty
        else s.factors.iterator.flatMap { n ⇒ oneFactorSampling(n, s.factors).from(context) }
    }

    Sampling(
      apply,
      outputs,
      inputs = inputs,
      validate = validate
    )
  }

  /**
   * Sampling along one of parameters
   * @param n
   * @return
   */
  def oneFactorSampling[D, T](n: NominalFactor[D, T], factors: Seq[NominalFactor[_, _]]): FromContext[Iterator[Iterable[Variable[?]]]] = FromContext { p ⇒
    import p._
    val exploreSampling: Sampling = n.prototype in n.values(n.factor.domain).domain.from(context).toSeq
    val nominalSampling: Seq[Sampling] = factors.filter(!_.equals(n)).map { n ⇒ n.nominalSampling }

    CompleteSampling(Seq(exploreSampling) ++ nominalSampling *)().from(context)
  }

}

case class OneFactorSampling(factors: NominalFactor[_, _]*)