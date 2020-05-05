package org.openmole.plugin.method.sensitivity

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._

object MorrisHook {

  def apply[F](dsl: DSLContainer[Sensitivity.MorrisParams], output: WritableOutput, format: F = CSVOutputFormat())(implicit name: sourcecode.Name, definitionScope: DefinitionScope, outputFormat: OutputFormat[F, Sensitivity.MorrisParams]) =
    Hook("MorrisHook") { p ⇒
      import p._
      import WritableOutput._

      val inputs = ScalarOrSequenceOfDouble.prototypes(dsl.data.inputs)

      output match {
        case FileValue(dirFC) ⇒
          Sensitivity.writeResults(format, dsl.data, FileValue(dirFC / s"mu${outputFormat.extension}"), inputs, dsl.data.outputs, Morris.mu(_, _)).from(context)
          Sensitivity.writeResults(format, dsl.data, FileValue(dirFC / s"muStar${outputFormat.extension}"), inputs, dsl.data.outputs, Morris.muStar(_, _)).from(context)
          Sensitivity.writeResults(format, dsl.data, FileValue(dirFC / s"sigma${outputFormat.extension}"), inputs, dsl.data.outputs, Morris.sigma(_, _)).from(context)
        case StreamValue(ps, prelude) ⇒
          Sensitivity.writeResults(format, dsl.data, StreamValue(ps, Some(prelude.getOrElse("") + "mu\n")), inputs, dsl.data.outputs, Morris.mu(_, _)).from(context)
          Sensitivity.writeResults(format, dsl.data, StreamValue(ps, Some("muStar\n")), inputs, dsl.data.outputs, Morris.muStar(_, _)).from(context)
          Sensitivity.writeResults(format, dsl.data, StreamValue(ps, Some("sigma\n")), inputs, dsl.data.outputs, Morris.sigma(_, _)).from(context)
      }
      context
    }

}
