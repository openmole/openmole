package org.openmole.plugin.sampling

import org.openmole.core.workflow.sampling.Factor

package onefactor {

  import org.openmole.core.workflow.domain.DiscreteFromContextDomain

  trait OneFactorDSL {
    implicit class SamplingIsNominalFactor[D, T](f: Factor[D, T])(implicit domain: DiscreteFromContextDomain[D, T]) {
      def nominal(t: T) = NominalFactor(f, t, domain)
    }
  }

}

package object onefactor extends OneFactorDSL
