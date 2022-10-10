package org.openmole.core.workflow.format

import org.openmole.core.context._
import org.openmole.core.expansion._
import org.openmole.core.workflow.hook.{ FromContextHook, HookExecutionContext }

object OutputFormat {
  sealed trait OutputContent
  case class SectionContent(sections: OutputSection*) extends OutputContent
  case class NamedContent(name: FromContext[String], variables: Seq[Variable[_]]) extends OutputContent


  case class OutputSection(name: FromContext[String], variables: Seq[Variable[_]])
}

trait OutputFormat[T, -M] {
  def write(executionContext: HookExecutionContext)(format: T, output: WritableOutput, content: OutputFormat.OutputContent, method: M): FromContext[Unit]
  def validate(format: T): Validate
  def appendable(format: T) = false
}