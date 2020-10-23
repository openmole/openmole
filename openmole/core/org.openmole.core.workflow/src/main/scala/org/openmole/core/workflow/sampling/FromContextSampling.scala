package org.openmole.core.workflow.sampling

import org.openmole.core.context.{ PrototypeSet, Val, Variable }
import org.openmole.core.expansion.{ FromContext, Validate }

object FromContextSampling {
  def apply(samples: FromContext.Parameters ⇒ Iterator[Iterable[Variable[_]]]) = new FromContextSampling(samples, PrototypeSet.empty, Iterable.empty, _ ⇒ Validate.success)
}

/**
 * Generic class to write samplings, to be used for extension plugins
 *
 * @param samples the sampling function in itself
 * @param i prototype set of inputs
 * @param f sampled prototypes
 * @param v function to validate parameters
 */
case class FromContextSampling(samples: FromContext.Parameters ⇒ Iterator[Iterable[Variable[_]]], i: PrototypeSet, f: Iterable[Val[_]], v: Seq[Val[_]] ⇒ Validate) extends Sampling {
  override def prototypes: Iterable[Val[_]] = f
  override def inputs: PrototypeSet = i
  override def apply(): FromContext[Iterator[Iterable[Variable[_]]]] = FromContext(samples) withValidate (v)

  def prototypes(f2: Iterable[Val[_]]) = new FromContextSampling(samples, i, f2, v)
  def inputs(i2: Iterable[Val[_]]) = new FromContextSampling(samples, i2, f, v)

  def withValidate(validate: Seq[Val[_]] ⇒ Validate) = {
    def nv(inputs: Seq[Val[_]]) = v(inputs) ++ validate(inputs)
    copy(v = nv)
  }

}
