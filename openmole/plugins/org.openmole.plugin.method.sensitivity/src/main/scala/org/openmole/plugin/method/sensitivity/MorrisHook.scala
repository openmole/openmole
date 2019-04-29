package org.openmole.plugin.method.sensitivity

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._

object MorrisHook {

  def apply(method: Sensitivity.MorrisMethodContainer, dir: FromContext[File])(implicit name: sourcecode.Name, definitionScope: DefinitionScope) =
    Hook("MorrisHook") { p ⇒
      import p._

      val inputs = ScalarOrSequenceOfDouble.prototypes(method.inputs)

      val muFile = dir / "mu.csv"
      Sensitivity.writeFile(muFile.from(context), inputs, method.outputs, Morris.mu(_, _)).from(context)

      val muStarFile = dir / "muStar.csv"
      Sensitivity.writeFile(muStarFile.from(context), inputs, method.outputs, Morris.muStar(_, _)).from(context)

      val sigmaFile = dir / "sigma.csv"
      Sensitivity.writeFile(sigmaFile.from(context), inputs, method.outputs, Morris.sigma(_, _)).from(context)

      context
    }

}
