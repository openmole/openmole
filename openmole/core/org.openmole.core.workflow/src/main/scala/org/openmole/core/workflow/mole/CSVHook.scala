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
    output:      WritableOutput,
    values:      Seq[Val[_]]                           = Vector.empty,
    exclude:     Seq[Val[_]]                           = Vector.empty,
    header:      OptionalArgument[FromContext[String]] = None,
    unrollArray: Boolean                               = false,
    overwrite:   Boolean                               = false)(implicit name: sourcecode.Name, definitionScope: DefinitionScope): mole.FromContextHook =
    FormattedFileHook(
      format = CSVOutputFormat(header = header, unrollArray = unrollArray, append = !overwrite),
      output = output,
      values = values,
      exclude = exclude,
      name = Some("CSVHook")
    )

  object CSVOutputFormat {

    implicit def format: OutputFormat[CSVOutputFormat, Any] = new OutputFormat[CSVOutputFormat, Any] {
      override def write(format: CSVOutputFormat, output: WritableOutput, variables: Seq[Variable[_]], method: Any): FromContext[Unit] = FromContext { p ⇒
        import p._

        def headerLine = format.header.map(_.from(context)) getOrElse csv.header(variables.map(_.prototype), variables.map(_.value), arrayOnRow = format.arrayOnRow)

        output match {
          case WritableOutput.FileValue(file) ⇒
            val f = file.from(context)
            val create = !format.append || f.isEmpty

            val h = if (f.isEmpty) Some(headerLine) else None

            if (create) f.atomicWithPrintStream { ps ⇒ csv.writeVariablesToCSV(ps, h, variables.map(_.value), unrollArray = format.unrollArray, arrayOnRow = format.arrayOnRow) }
            else f.withPrintStream(append = true, create = true) { ps ⇒ csv.writeVariablesToCSV(ps, h, variables.map(_.value), unrollArray = format.unrollArray, arrayOnRow = format.arrayOnRow) }

          case WritableOutput.StreamValue(ps, prelude) ⇒
            prelude.foreach(ps.print)
            val header = Some(headerLine)
            csv.writeVariablesToCSV(ps, header, variables, unrollArray = format.unrollArray, arrayOnRow = format.arrayOnRow)
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
    header:      OptionalArgument[FromContext[String]] = None,
    unrollArray: Boolean                               = false,
    arrayOnRow:  Boolean                               = false,
    append:      Boolean                               = false)

}
