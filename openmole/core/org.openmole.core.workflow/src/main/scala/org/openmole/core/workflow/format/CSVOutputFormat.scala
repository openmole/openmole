package org.openmole.core.workflow.format

import java.io.PrintStream

import org.openmole.core.context.{ Val, Variable }
import org.openmole.core.csv
import org.openmole.core.expansion.FromContext
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.hook.HookExecutionContext
import org.openmole.core.workflow.tools.OptionalArgument

object CSVOutputFormat {

  implicit def format: OutputFormat[CSVOutputFormat, Any] = new OutputFormat[CSVOutputFormat, Any] {
    override def write(executionContext: HookExecutionContext)(format: CSVOutputFormat, output: WritableOutput, content: OutputFormat.OutputContent, method: Any): FromContext[Unit] = FromContext { p ⇒
      import p._

      def headerLine(variables: Seq[Variable[_]]) = format.header.map(_.from(context)) getOrElse csv.header(variables.map(_.prototype), variables.map(_.value), arrayOnRow = format.arrayOnRow)

      def writeFile(f: File, variables: Seq[Variable[_]]) = {
        val create = !format.append || f.isEmpty
        val h = if (create || f.isEmpty) Some(headerLine(variables)) else None
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

      import OutputFormat.*
      import WritableOutput.*


      (output, content) match {
        case (Store(file), PlainContent(variables)) ⇒
          val f =
            format.name.option match
              case Some(name) => file.from(context) / s"${name}.csv"
              case None => file.from(context)
          writeFile(f, variables)
        case (Store(file), s: SectionContent) ⇒
          val directory = 
            format.name.option match
              case None => file.from(context)
              case Some(name) => file.from(context) / name.from(context)
          for { section ← s.content } writeFile(directory / s"${section.name}.csv", section.variables)
        case (Display(ps), PlainContent(variables)) ⇒
          writeStream(ps, format.name.map(_.from(context)), variables)
        case (Display(ps), s: SectionContent) ⇒
          for { section ← s.content } writeStream(ps, Some(section.name), section.variables)
      }
    }

    override def validate(format: CSVOutputFormat) = format.header.toOption.toSeq.map(_.validate)
  }

}

case class CSVOutputFormat(
  header:      OptionalArgument[FromContext[String]]  = None,
  unrollArray: Boolean                                = false,
  arrayOnRow:  Boolean                                = false,
  append:      Boolean                                = false,
  name:        OptionalArgument[FromContext[String]]  = None)