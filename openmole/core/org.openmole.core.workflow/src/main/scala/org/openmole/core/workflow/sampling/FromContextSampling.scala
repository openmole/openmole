package org.openmole.core.workflow.sampling

import org.openmole.core.context.{ PrototypeSet, Val, Variable }
import org.openmole.core.argument.{ FromContext, Validate }

object FromContextSampling:
  def apply(samples: FromContext.Parameters => Iterator[Iterable[Variable[?]]]) = new FromContextSampling(samples, PrototypeSet.empty, Iterable.empty, Validate.success)

  implicit def isSampling: IsSampling[FromContextSampling] =
    new IsSampling[FromContextSampling]:
      def apply(s: FromContextSampling) =
        Sampling(
          FromContext(s.samples),
          s.o,
          s.i,
          s.v
        )

/**
 * Generic class to write samplings, to be used for extension plugins
 *
 * @param samples the sampling function in itself
 * @param i prototype set of inputs
 * @param f sampled prototypes
 * @param v function to validate parameters
 */
case class FromContextSampling(samples: FromContext.Parameters => Iterator[Iterable[Variable[?]]], i: PrototypeSet, o: Iterable[Val[?]], v: Validate):

  def prototypes(f: Iterable[Val[?]]) = outputs(f)
  def outputs(f: Iterable[Val[?]]) = copy(o = o ++ f)
  def inputs(i: Iterable[Val[?]]) = copy(i = i)

  def validate(validate: Validate) = copy(v = v ++ validate)


