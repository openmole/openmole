package org.openmole.plugin.sampling.onefactor

import org.openmole.core.workflow.sampling.Factor
import org.openmole.core.workflow.domain.DiscreteFromContextDomain

implicit class SamplingIsNominalFactor[D, T](f: Factor[D, T])(using domain: DiscreteFromContextDomain[D, T]):
  def nominal(t: T) = NominalFactor(f, t, domain)


