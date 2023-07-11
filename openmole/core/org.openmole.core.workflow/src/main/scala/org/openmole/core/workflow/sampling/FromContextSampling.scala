package org.openmole.core.workflow.sampling

import org.openmole.core.context.{ PrototypeSet, Val, Variable }
import org.openmole.core.fromcontext.{ FromContext, Validate }

object FromContextSampling {
  def apply(samples: FromContext.Parameters ⇒ Iterator[Iterable[Variable[_]]]) = new FromContextSampling(samples, PrototypeSet.empty, Iterable.empty, Validate.success)

  implicit def isSampling: IsSampling[FromContextSampling] = s ⇒
    Sampling(
      FromContext(s.samples),
      s.o,
      s.i,
      s.v
    )
}

/**
 * Generic class to write samplings, to be used for extension plugins
 *
 * @param samples the sampling function in itself
 * @param i prototype set of inputs
 * @param f sampled prototypes
 * @param v function to validate parameters
 */
case class FromContextSampling(samples: FromContext.Parameters ⇒ Iterator[Iterable[Variable[_]]], i: PrototypeSet, o: Iterable[Val[_]], v: Validate) {

  def prototypes(f: Iterable[Val[_]]) = outputs(f)
  def outputs(f: Iterable[Val[_]]) = copy(o = o ++ f)
  def inputs(i: Iterable[Val[_]]) = copy(i = i)

  def validate(validate: Validate) = copy(v = v ++ validate)

}
