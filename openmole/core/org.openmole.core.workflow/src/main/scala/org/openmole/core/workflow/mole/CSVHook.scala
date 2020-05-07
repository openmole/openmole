package org.openmole.core.workflow.mole

import java.io.PrintStream

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
      override def write(format: CSVOutputFormat, output: WritableOutput, content: OutputFormat.OutputContent, method: Any): FromContext[Unit] = FromContext { p ⇒
        import p._

        def headerLine(variables: Seq[Variable[_]]) = format.header.map(_.from(context)) getOrElse csv.header(variables.map(_.prototype), variables.map(_.value), arrayOnRow = format.arrayOnRow)

        def writeFile(f: File, variables: Seq[Variable[_]]) = {
          val create = !format.append || f.isEmpty
          val h = if (f.isEmpty) Some(headerLine(variables)) else None
          if (create) f.atomicWithPrintStream { ps ⇒ csv.writeVariablesToCSV(ps, h, variables.map(_.value), unrollArray = format.unrollArray, arrayOnRow = format.arrayOnRow) }
          else f.withPrintStream(append = true, create = true) { ps ⇒ csv.writeVariablesToCSV(ps, h, variables.map(_.value), unrollArray = format.unrollArray, arrayOnRow = format.arrayOnRow) }
        }

        def writeStream(ps: PrintStream, section: Option[String], variables: Seq[Variable[_]]) =
          section match {
            case None ⇒
              val header = Some(headerLine(variables))
              csv.writeVariablesToCSV(ps, header, variables.map(_.value), unrollArray = format.unrollArray, arrayOnRow = format.arrayOnRow)
            case Some(section) ⇒
              ps.println(section + ":")
              val header = Some(headerLine(variables))
              csv.writeVariablesToCSV(ps, header, variables.map(_.value), unrollArray = format.unrollArray, arrayOnRow = format.arrayOnRow, margin = "  ")
          }

        import WritableOutput._
        import OutputFormat._

        (output, content) match {
          case (FileValue(file), PlainContent(variables, name)) ⇒
            val f =
              name match {
                case None       ⇒ file.from(context)
                case Some(name) ⇒ file.from(context) / s"${name.from(context)}.csv"
              }

            writeFile(f, variables)
          case (FileValue(file), SectionContent(sections)) ⇒
            val directory = file.from(context)
            for { section ← sections } writeFile(directory / s"${section.name.from(context)}.csv", section.variables)
          case (StreamValue(ps), PlainContent(variables, name)) ⇒
            writeStream(ps, name.map(_.from(context)), variables)
          case (StreamValue(ps), SectionContent(sections)) ⇒
            for { section ← sections } writeStream(ps, Some(section.name.from(context)), section.variables)
        }
      }

      override def validate(format: CSVOutputFormat) = { p ⇒
        import p._
        format.header.option.toSeq.flatMap(_.validate(inputs))
      }
    }
  }

  case class CSVOutputFormat(
    header:      OptionalArgument[FromContext[String]] = None,
    unrollArray: Boolean                               = false,
    arrayOnRow:  Boolean                               = false,
    append:      Boolean                               = false)

}
