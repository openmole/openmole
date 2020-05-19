package org.openmole.plugin.method.sensitivity

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.core.workflow.format.WritableOutput

object SaltelliHook {

  def apply[F](dsl: DSLContainer[Sensitivity.SaltelliParams], output: WritableOutput, format: F = CSVOutputFormat())(implicit name: sourcecode.Name, definitionScope: DefinitionScope, outputFormat: OutputFormat[F, Sensitivity.SaltelliParams]) =
    Hook("SaltelliHook") { p ⇒
      import p._
      import WritableOutput._

      val inputs = ScalarOrSequenceOfDouble.prototypes(dsl.data.inputs)

      import OutputFormat._

      def sections =
        Seq(
          OutputSection("firstOrderIndices", Sensitivity.variableResults(inputs, dsl.data.outputs, Saltelli.firstOrder(_, _)).from(context)),
          OutputSection("totalOrderIndices", Sensitivity.variableResults(inputs, dsl.data.outputs, Saltelli.totalOrder(_, _)).from(context))
        )

      outputFormat.write(executionContext)(format, output, sections, dsl.data).from(context)

      context
    }

}
