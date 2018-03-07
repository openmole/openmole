package org.openmole.core.workflow.domain

import scala.annotation.implicitNotFound

@implicitNotFound("${D} is not a sized variation domain")
trait Sized[-D] {
  def apply(domain: D): Int
}
