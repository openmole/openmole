package org.openmole.plugin.sampling

import org.openmole.core.workflow.domain.Finite
import org.openmole.core.workflow.sampling.Factor

package onefactor {

  trait OneFactorDSL {
    implicit class SamplingIsNominalFactor[D, _](f: Factor[D, Any])(implicit domain: Finite[D, Any]) {
      def nominal[T](t: T) = NominalFactor(f, t, domain)
    }
  }

  object OneFactorDSL {
    def factorToNominal[D, T](f: Factor[D, T], t: T)(implicit domain: Finite[D, T]): NominalFactor[D, T] = NominalFactor(f, t, domain)
  }

}

package object onefactor extends OneFactorDSL
