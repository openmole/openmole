package org.openmole.core.workflow.sampling

import org.openmole.core.context.{ PrototypeSet, Val, Variable }
import org.openmole.core.expansion.{ FromContext, Validate }

object FromContextSampling {
  def apply(samples: FromContext.Parameters ⇒ Iterator[Iterable[Variable[_]]]) = new FromContextSampling(samples, PrototypeSet.empty, Iterable.empty, Validate.success)

  implicit def isSampling: IsSampling[FromContextSampling] = new IsSampling[FromContextSampling] {
    override def validate(s: FromContextSampling): Validate = s.v
    override def inputs(s: FromContextSampling): PrototypeSet = s.i
    override def outputs(s: FromContextSampling): Iterable[Val[_]] = s.o
    override def apply(s: FromContextSampling) = FromContext(s.samples)

    def combine(s1: Iterator[Iterable[Variable[_]]], s2: Sampling) = FromContext { p ⇒
      import p._
      for (x ← s1; y ← s2().from(context ++ x)) yield x ++ y
    }
  }

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
  def outputs(f: Iterable[Val[_]]) = copy(o = o)
  def inputs(i: Iterable[Val[_]]) = copy(i = i)

  def validate(validate: Validate) = copy(v = v ++ validate)

}
