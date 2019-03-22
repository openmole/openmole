package org.openmole.plugin.sampling

package onefactor {

  import org.openmole.core.workflow.domain.Finite
  import org.openmole.core.workflow.sampling.Factor

  trait OneFactorDSL {
    implicit class SamplingIsNominalFactor[D, _](f: Factor[D, Any])(implicit domain: Finite[D,Any]) {
      def nominal[T](t: T) = NominalFactor(f, t, domain)
    }
  }

}

package object onefactor extends OneFactorDSL
