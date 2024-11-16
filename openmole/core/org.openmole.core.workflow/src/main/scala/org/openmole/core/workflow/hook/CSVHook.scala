package org.openmole.core.workflow.hook

import java.io.PrintStream

import org.openmole.core.context.{ Val, Variable }
import org.openmole.core.argument.{FromContext, OptionalArgument}
import org.openmole.core.setter.DefinitionScope
import org.openmole.core.workflow.dsl._
import org.openmole.core.format.{ CSVFormat, WritableOutput }

object CSVHook:

  def apply(output: WritableOutput, values: Val[?]*)(implicit name: sourcecode.Name, definitionScope: DefinitionScope): FromContextHook =
    apply(output, values.toVector)

  def apply(
    output:      WritableOutput,
    values:      Seq[Val[?]]                           = Vector.empty,
    exclude:     Seq[Val[?]]                           = Vector.empty,
    header:      OptionalArgument[FromContext[String]] = None,
    unrollArray: Boolean                               = false,
    arrayOnRow:  Boolean                               = false,
    overwrite:   Boolean                               = false,
    postfix:     OptionalArgument[FromContext[String]] = None,
    directory:   Boolean                               = false)(implicit name: sourcecode.Name, definitionScope: DefinitionScope): FromContextHook =

    Hook("CSVHook"): parameters =>
      import parameters._

      def headerLine(variables: Seq[Variable[?]]) = header.map(_.from(context)) getOrElse CSVFormat.header(variables.map(_.prototype), variables.map(_.value), arrayOnRow = arrayOnRow)

      import WritableOutput.*

      output match
        case Store(file) =>
          def writeFile(f: File, variables: Seq[Variable[?]]) =
            val create = overwrite || f.isEmpty
            val h = if (create || f.isEmpty) Some(headerLine(variables)) else None
            if create
            then f.atomicWithPrintStream { ps => CSVFormat.appendVariablesToCSV(ps, h, variables.map(_.value), unrollArray = unrollArray, arrayOnRow = arrayOnRow) }
            else f.withPrintStream(append = true, create = true) { ps => CSVFormat.appendVariablesToCSV(ps, h, variables.map(_.value), unrollArray = unrollArray, arrayOnRow = arrayOnRow) }


          writeFile(file.from(context), context.values.toSeq)
          context
        case Display(ps) =>
          def writeStream(ps: PrintStream, variables: Seq[Variable[?]]) =
            val header = Some(headerLine(variables))
            CSVFormat.appendVariablesToCSV(ps, header, variables.map(_.value), unrollArray = unrollArray, arrayOnRow = arrayOnRow)

          writeStream(ps, context.values.toSeq)

          context

    .withValidate:
      WritableOutput.file(output).toSeq.flatMap(_.validate)  ++
        header.toOption.toSeq.map(_.validate) ++
        postfix.option.toSeq.map(_.validate)
    .set (inputs ++= values)

