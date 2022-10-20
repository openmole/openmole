package org.openmole.core.workflow.format

import org.openmole.core.context._
import org.openmole.core.expansion._
import org.openmole.core.workflow.hook.{ FromContextHook, HookExecutionContext }

object OutputFormat:
  sealed trait OutputContent
  case class Section(name: String, variables: Seq[Variable[_]])
  case class SectionContent(content: Seq[Section]) extends OutputContent
  case class PlainContent(variables: Seq[Variable[_]]) extends OutputContent

trait OutputFormat[T, -M]:
  def write(executionContext: HookExecutionContext)(format: T, output: WritableOutput, content: OutputFormat.OutputContent, method: M): FromContext[Unit]
  def validate(format: T): Validate