package org.openmole.core.workflow.format

import org.openmole.core.context._
import org.openmole.core.expansion._
import org.openmole.core.workflow.hook.{ FromContextHook, HookExecutionContext }

object OutputFormat {
  object OutputContent {
    implicit def sectionToContent(s: Seq[OutputSection]) = SectionContent(s)
    implicit def variablesToPlainContent(v: Seq[Variable[_]]) = PlainContent(v)
  }

  sealed trait OutputContent
  case class SectionContent(sections: Seq[OutputSection]) extends OutputContent
  case class PlainContent(variables: Seq[Variable[_]], name: Option[FromContext[String]] = None) extends OutputContent

  case class OutputSection(name: FromContext[String], variables: Seq[Variable[_]])
}

trait OutputFormat[T, -M] {
  def write(executionContext: HookExecutionContext)(format: T, output: WritableOutput, content: OutputFormat.OutputContent, method: M): FromContext[Unit]
  def validate(format: T, inputs: Seq[Val[_]]): Validate
}