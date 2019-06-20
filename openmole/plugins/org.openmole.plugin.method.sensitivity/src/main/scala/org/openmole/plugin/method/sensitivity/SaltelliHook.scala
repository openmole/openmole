package org.openmole.plugin.method.sensitivity

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._

object SaltelliHook {

  def apply(dsl: DSLContainer[Sensitivity.SaltelliParams], output: WritableOutput)(implicit name: sourcecode.Name, definitionScope: DefinitionScope) =
    Hook("SaltelliHook") { p ⇒
      import p._

      val inputs = ScalarOrSequenceOfDouble.prototypes(dsl.data.inputs)

      output match {
        case WritableOutput.FileValue(dirFC) ⇒
          val dir = dirFC.from(context)

          (dir / "firstOrderIndices.csv").withPrintStream(overwrite = true, create = true) { ps ⇒
            Sensitivity.writeResults(ps, inputs, dsl.data.outputs, Saltelli.firstOrder(_, _)).from(context)
          }

          (dir / "totalOrderIndices.csv").withPrintStream(overwrite = true) { ps ⇒
            Sensitivity.writeResults(ps, inputs, dsl.data.outputs, Saltelli.totalOrder(_, _)).from(context)
          }
        case WritableOutput.PrintStreamValue(ps) ⇒
          ps.println("first order")
          Sensitivity.writeResults(ps, inputs, dsl.data.outputs, Saltelli.firstOrder(_, _)).from(context)

          ps.println("total order")
          Sensitivity.writeResults(ps, inputs, dsl.data.outputs, Saltelli.totalOrder(_, _)).from(context)

      }

      context
    }

}
