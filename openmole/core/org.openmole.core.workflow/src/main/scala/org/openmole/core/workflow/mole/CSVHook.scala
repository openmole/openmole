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
    arrayOnRow: Boolean                               = false)(implicit name: sourcecode.Name, definitionScope: DefinitionScope): mole.FromContextHook =
    Hook("CSVHook") { parameters ⇒
      import parameters._

      val excludeSet = exclude.map(_.name).toSet
      val ps =
        { if (values.isEmpty) context.values.map { _.prototype }.toVector else values }.filter { v ⇒ !excludeSet.contains(v.name) }
      val vs = ps.map(context(_))

      def headerLine = header.map(_.from(context)) getOrElse csv.header(ps, vs, arrayOnRow)

      output match {
        case WritableOutput.FileValue(file) ⇒
          val f = file.from(context)
          val h = if (f.isEmpty) Some(headerLine) else None
          f.withPrintStream(append = true, create = true) { ps ⇒ csv.writeVariablesToCSV(ps, h, vs, arrayOnRow) }
        case WritableOutput.PrintStreamValue(ps) ⇒
          val header = Some(headerLine)
          csv.writeVariablesToCSV(ps, header, vs, arrayOnRow)
      }

      context
    } validate { p ⇒
      import p._
      WritableOutput.file(output).toSeq.flatMap(_.validate(inputs)) ++
        header.option.toSeq.flatMap(_.validate(inputs))
    } set (inputs += (values: _*))

}
