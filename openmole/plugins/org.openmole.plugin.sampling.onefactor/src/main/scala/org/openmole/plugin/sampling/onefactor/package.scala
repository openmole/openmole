package org.openmole.plugin.sampling

import org.openmole.core.workflow.domain.Finite
import org.openmole.core.workflow.sampling.Factor

package onefactor {

  trait OneFactorDSL {
    implicit class SamplingIsNominalFactor[D, T](f: Factor[D, T])(implicit domain: Finite[D, T]) {
      def nominal(t: T) = NominalFactor(f, t, domain)
    }
  }

}

package object onefactor extends OneFactorDSL
