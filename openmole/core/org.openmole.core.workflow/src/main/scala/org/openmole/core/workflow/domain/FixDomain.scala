package org.openmole.core.workflow.domain

import scala.annotation.implicitNotFound


object FixDomain:
  def apply[D, T](f: D => Domain[Iterable[T]]) =
    new FixDomain[D, T]:
      def apply(d: D) = f(d)

/**
 * Explicit fixed domain
 * @tparam D
 * @tparam T
 */
@implicitNotFound("${D} is not a fix variation domain of type ${T}")
trait FixDomain[-D, +T]:
  def apply(domain: D): Domain[Iterable[T]]
