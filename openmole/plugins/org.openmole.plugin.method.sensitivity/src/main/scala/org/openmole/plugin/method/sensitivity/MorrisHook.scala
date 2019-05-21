package org.openmole.plugin.method.sensitivity

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._

object MorrisHook {

  def apply(dsl: DSLContainer[Sensitivity.MorrisParams], output: WritableOutput)(implicit name: sourcecode.Name, definitionScope: DefinitionScope) =
    Hook("MorrisHook") { p ⇒
      import p._

      val inputs = ScalarOrSequenceOfDouble.prototypes(dsl.data.inputs)

      output match {
        case WritableOutput.FileValue(dirFC) ⇒
          val dir = dirFC.from(context)
          dir.mkdirs()

          (dir / "mu.csv").withPrintStream(overwrite = true) { ps ⇒
            Sensitivity.writeResults(ps, inputs, dsl.data.outputs, Morris.mu(_, _)).from(context)
          }

          (dir / "muStar.csv").withPrintStream(overwrite = true) { ps ⇒
            Sensitivity.writeResults(ps, inputs, dsl.data.outputs, Morris.muStar(_, _)).from(context)
          }

          (dir / "sigma.csv").withPrintStream(overwrite = true) { ps ⇒
            Sensitivity.writeResults(ps, inputs, dsl.data.outputs, Morris.sigma(_, _)).from(context)
          }
        case WritableOutput.PrintStreamValue(ps) ⇒
          ps.println("mu")
          Sensitivity.writeResults(ps, inputs, dsl.data.outputs, Morris.mu(_, _)).from(context)

          ps.println("mu star")
          Sensitivity.writeResults(ps, inputs, dsl.data.outputs, Morris.muStar(_, _)).from(context)

          ps.println("sigma")
          Sensitivity.writeResults(ps, inputs, dsl.data.outputs, Morris.sigma(_, _)).from(context)

      }
      context
    }

}
