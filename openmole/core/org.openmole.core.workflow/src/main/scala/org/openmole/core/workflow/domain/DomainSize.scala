package org.openmole.core.workflow.domain

import scala.annotation.implicitNotFound

/**
 * Property of having a size for a domain
 * @tparam D
 */
@implicitNotFound("${D} is not a sized variation domain")
trait DomainSize[-D] :
  def apply(domain: D): Int
