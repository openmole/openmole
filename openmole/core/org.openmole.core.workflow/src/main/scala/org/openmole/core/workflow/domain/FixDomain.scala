package org.openmole.core.workflow.domain

import scala.annotation.implicitNotFound

/**
 * Explicit fixed domain
 * @tparam D
 * @tparam T
 */
@implicitNotFound("${D} is not a fix variation domain of type ${T}")
trait FixDomain[-D, +T] {
  def apply(domain: D): Iterable[T]
}
