package org.openmole.core.workflow.domain

import scala.annotation.implicitNotFound


object DomainSize:
  def apply[D](f: D => Int) =
    new DomainSize[D]:
      def apply(d: D) = f(d)

  given [K, D](using inner: InnerDomain[K, D], b: DomainSize[D]): DomainSize[K] =
    DomainSize: d =>
      b(inner(d))

/**
 * Property of having a size for a domain
 * @tparam D
 */
@implicitNotFound("${D} is not a sized variation domain")
trait DomainSize[-D] :
  def apply(domain: D): Int
