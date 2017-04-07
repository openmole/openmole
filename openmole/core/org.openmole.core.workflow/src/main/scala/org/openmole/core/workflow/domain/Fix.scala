package org.openmole.core.workflow.domain

import scala.annotation.implicitNotFound

@implicitNotFound("${D} is not a fix variation domain of type ${T}")
trait Fix[-D, +T] {
  def apply(domain: D): Iterable[T]
}
