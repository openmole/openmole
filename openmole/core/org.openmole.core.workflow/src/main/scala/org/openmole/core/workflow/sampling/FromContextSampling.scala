package org.openmole.core.workflow.sampling

import org.openmole.core.context.{ PrototypeSet, Val, Variable }
import org.openmole.core.expansion.FromContext

object FromContextSampling {
  def apply(samples: FromContext.Parameters ⇒ Iterator[Iterable[Variable[_]]]) = new FromContextSampling(samples, PrototypeSet.empty, Iterable.empty, _ ⇒ Seq.empty)
}

class FromContextSampling(samples: FromContext.Parameters ⇒ Iterator[Iterable[Variable[_]]], i: PrototypeSet, f: Iterable[Val[_]], v: FromContext.ValidationParameters ⇒ Seq[Throwable]) extends Sampling {
  override def prototypes: Iterable[Val[_]] = f
  override def inputs: PrototypeSet = i
  override def apply(): FromContext[Iterator[Iterable[Variable[_]]]] = FromContext(samples) validate (v)

  def prototypes(f2: Iterable[Val[_]]) = new FromContextSampling(samples, i, f2, v)
  def inputs(i2: Seq[Val[_]]) = new FromContextSampling(samples, i2, f, v)

  def validate(v2: FromContext.ValidationParameters ⇒ Seq[Throwable]) = {
    def nv(p: FromContext.ValidationParameters) = v(p) ++ v2(p)
    new FromContextSampling(samples, i, f, nv)
  }

}
