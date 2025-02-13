package org.openmole.core.workflow.domain

import scala.annotation.implicitNotFound
import org.openmole.core.context.Val

object FixDomain:
  def apply[D, T](f: D => Domain[Iterable[T]]) =
    new FixDomain[D, T]:
      def apply(d: D) = f(d)

  given [K, D, T](using inner: InnerDomain[K, D], b: FixDomain[D, T]): FixDomain[K, T] =
    FixDomain: d =>
      b(inner(d))

/**
 * Explicit fixed domain
 * @tparam D
 * @tparam T
 */
@implicitNotFound("${D} is not a fix variation domain of type ${T}")
trait FixDomain[-D, +T]:
  def apply(domain: D): Domain[Iterable[T]]
