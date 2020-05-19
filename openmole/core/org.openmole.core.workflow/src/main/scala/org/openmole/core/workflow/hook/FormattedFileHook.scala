package org.openmole.core.workflow.hook

import org.openmole.core.context._
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.expansion.FromContext
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.format._

object FormattedFileHook {

  def apply[T, M](
    format:  T,
    output:  WritableOutput,
    values:  Seq[Val[_]]    = Vector.empty,
    exclude: Seq[Val[_]]    = Vector.empty,
    method:  M              = None,
    name:    Option[String] = None)(implicit valName: sourcecode.Name, definitionScope: DefinitionScope, fileFormat: OutputFormat[T, M]): FromContextHook =

    Hook(name getOrElse "FileFormatHook") { parameters ⇒
      import parameters._

      val excludeSet = exclude.map(_.name).toSet
      val ps = { if (values.isEmpty) context.values.map { _.prototype }.toVector else values }.filter { v ⇒ !excludeSet.contains(v.name) }
      val variables = ps.map(p ⇒ context.variable(p).getOrElse(throw new UserBadDataError(s"Variable $p not found in hook $this")))

      fileFormat.write(executionContext)(format, output, variables, method).from(context)

      context
    } validate { p ⇒
      import p._
      WritableOutput.file(output).toSeq.flatMap(_.validate(inputs)) ++ fileFormat.validate(format).apply(p)
    } set (inputs += (values: _*))

}
