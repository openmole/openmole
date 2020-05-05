package org.openmole.plugin.method.sensitivity

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._

object SaltelliHook {

  def apply[F](dsl: DSLContainer[Sensitivity.SaltelliParams], output: WritableOutput, format: F = CSVOutputFormat())(implicit name: sourcecode.Name, definitionScope: DefinitionScope, outputFormat: OutputFormat[F, Sensitivity.SaltelliParams]) =
    Hook("SaltelliHook") { p ⇒
      import p._
      import WritableOutput._

      val inputs = ScalarOrSequenceOfDouble.prototypes(dsl.data.inputs)

      output match {
        case FileValue(dirFC) ⇒
          Sensitivity.writeResults(format, dsl.data, FileValue(dirFC / s"firstOrderIndices${outputFormat.extension}"), inputs, dsl.data.outputs, Saltelli.firstOrder(_, _)).from(context)
          Sensitivity.writeResults(format, dsl.data, FileValue(dirFC / s"totalOrderIndices${outputFormat.extension}"), inputs, dsl.data.outputs, Saltelli.totalOrder(_, _)).from(context)
        case StreamValue(ps, prelude) ⇒
          Sensitivity.writeResults(format, dsl.data, StreamValue(ps, Some(prelude.getOrElse("") + "first order\n")), inputs, dsl.data.outputs, Saltelli.firstOrder(_, _)).from(context)
          Sensitivity.writeResults(format, dsl.data, StreamValue(ps, Some("total order\n")), inputs, dsl.data.outputs, Saltelli.totalOrder(_, _)).from(context)
      }

      context
    }

}
