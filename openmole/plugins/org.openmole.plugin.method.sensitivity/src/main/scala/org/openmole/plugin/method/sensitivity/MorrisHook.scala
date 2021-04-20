package org.openmole.plugin.method.sensitivity

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.core.workflow.format.WritableOutput

object MorrisHook {

  def apply[F](dsl: DSLContainer[Sensitivity.MorrisParams], output: WritableOutput, format: F = CSVOutputFormat())(implicit name: sourcecode.Name, definitionScope: DefinitionScope, outputFormat: OutputFormat[F, Sensitivity.MorrisParams]) =
    Hook("MorrisHook") { p ⇒
      import p._
      import WritableOutput._

      val inputs = ScalarOrSequenceOfDouble.prototypes(dsl.method.inputs)

      import OutputFormat._

      def sections =
        Seq(
          OutputSection("mu", Sensitivity.variableResults(inputs, dsl.method.outputs, Morris.mu(_, _)).from(context)),
          OutputSection("muStar", Sensitivity.variableResults(inputs, dsl.method.outputs, Morris.muStar(_, _)).from(context)),
          OutputSection("sigma", Sensitivity.variableResults(inputs, dsl.method.outputs, Morris.sigma(_, _)).from(context))
        )

      outputFormat.write(executionContext)(format, output, sections, dsl.method).from(context)

      context
    }

}
