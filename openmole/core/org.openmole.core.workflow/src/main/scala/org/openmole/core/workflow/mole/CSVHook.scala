package org.openmole.core.workflow.mole

import org.openmole.core.csv
import org.openmole.core.workflow.mole
import org.openmole.core.workflow.tools.{ OptionalArgument, WritableOutput }
import org.openmole.core.context._
import org.openmole.core.expansion.FromContext
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.dsl._

object CSVHook {

  def apply(output: WritableOutput, values: Val[_]*)(implicit name: sourcecode.Name, definitionScope: DefinitionScope): mole.FromContextHook =
    apply(output, values.toVector)

  def apply(
    output:     WritableOutput,
    values:     Seq[Val[_]]                           = Vector.empty,
    exclude:    Seq[Val[_]]                           = Vector.empty,
    header:     OptionalArgument[FromContext[String]] = None,
    arrayOnRow: Boolean                               = false,
    overwrite:  Boolean                               = false)(implicit name: sourcecode.Name, definitionScope: DefinitionScope): mole.FromContextHook =
    FormattedFileHook(
      format = CSVOutputFormat(header = header, arrayOnRow = arrayOnRow, overwrite = overwrite),
      output = output,
      values = values,
      exclude = exclude,
      name = Some("CSVHook")
    )

  object CSVOutputFormat {

    implicit def format: OutputFormat[CSVOutputFormat] = new OutputFormat[CSVOutputFormat] {
      override def write(format: CSVOutputFormat, output: WritableOutput, variables: Seq[Variable[_]]): FromContext[Unit] = FromContext { p ⇒
        import p._

        def headerLine = format.header.map(_.from(context)) getOrElse csv.header(variables.map(_.prototype), variables, format.arrayOnRow)

        output match {
          case WritableOutput.FileValue(file) ⇒
            val f = file.from(context)
            val create = format.overwrite || f.isEmpty

            val h = if (f.isEmpty) Some(headerLine) else None

            if (create) f.atomicWithPrintStream { ps ⇒ csv.writeVariablesToCSV(ps, h, variables.map(_.value), format.arrayOnRow) }
            else f.withPrintStream(append = true, create = true) { ps ⇒ csv.writeVariablesToCSV(ps, h, variables.map(_.value), format.arrayOnRow) }

          case WritableOutput.PrintStreamValue(ps) ⇒
            val header = Some(headerLine)
            csv.writeVariablesToCSV(ps, header, variables, format.arrayOnRow)
        }
      }

      override def validate(format: CSVOutputFormat) = { p ⇒
        import p._
        format.header.option.toSeq.flatMap(_.validate(inputs))
      }

      override def extension: String = ".csv"
    }
  }

  case class CSVOutputFormat(
    header:     OptionalArgument[FromContext[String]] = None,
    arrayOnRow: Boolean                               = false,
    overwrite:  Boolean                               = false)

}
