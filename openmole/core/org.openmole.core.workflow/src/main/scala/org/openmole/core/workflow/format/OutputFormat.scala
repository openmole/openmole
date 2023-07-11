package org.openmole.core.workflow.format

import org.openmole.core.context._
import org.openmole.core.fromcontext._
import org.openmole.core.workflow.hook.{ FromContextHook, HookExecutionContext }

object OutputFormat:
  object Section:
    given Conversion[(String, Seq[Variable[_]]), Section] = s => Section(Some(s._1), s._2)
    given Conversion[Seq[Variable[_]], Section] = s => Section(None, s)

  case class Section(name: Option[String], variables: Seq[Variable[_]])
  case class OutputContent(section: Section*)

trait OutputFormat[T, -M]:
  def write(executionContext: HookExecutionContext)(format: T, output: WritableOutput, content: OutputFormat.OutputContent, method: M, append: Boolean = false): FromContext[Unit]
  def validate(format: T): Validate