package org.openmole.core.workflow.mole

import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.mole
import org.openmole.core.workflow.tools.WritableOutput
import org.openmole.core.context._
import org.openmole.core.expansion.FromContext
import org.openmole.core.workflow.builder._

trait OutputFormat[T] {
  def write(format: T, output: WritableOutput, ps: Seq[Val[_]]): FromContext[Unit]
  def validate(format: T): FromContextHook.ValidateParameters ⇒ Seq[Throwable]
}

object FormattedFileHook {

  def apply[T: OutputFormat](
    format:  T,
    output:  WritableOutput,
    values:  Seq[Val[_]]    = Vector.empty,
    exclude: Seq[Val[_]]    = Vector.empty,
    name:    Option[String] = None)(implicit valName: sourcecode.Name, definitionScope: DefinitionScope, fileFormat: OutputFormat[T]): mole.FromContextHook =

    Hook(name getOrElse "FileFormatHook") { parameters ⇒
      import parameters._

      val excludeSet = exclude.map(_.name).toSet
      val ps = { if (values.isEmpty) context.values.map { _.prototype }.toVector else values }.filter { v ⇒ !excludeSet.contains(v.name) }

      fileFormat.write(format, output, ps).from(context)

      context
    } validate { p ⇒
      import p._
      WritableOutput.file(output).toSeq.flatMap(_.validate(inputs)) ++ fileFormat.validate(format).apply(p)
    } set (inputs += (values: _*))

}
