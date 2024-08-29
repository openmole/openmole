package org.openmole.core.format

import org.openmole.core.context.*
import org.openmole.core.argument.*
import org.openmole.core.timeservice.TimeService
import org.openmole.core.workspace.TmpDirectory

object OutputFormat:
  case class FormatExecutionContext(
    moleLaunchTime: Long,
    jobId: Long,
    moleExecutionId: String)(
   implicit val tmpDirectory: TmpDirectory,
   val timeService:       TimeService
  )

  object SectionContent:
    given Conversion[(String, Seq[Variable[_]]), SectionContent] = s => SectionContent(Some(s._1), s._2)
    given Conversion[Seq[Variable[_]], SectionContent] = s => SectionContent(None, s)

  case class SectionContent(name: Option[String], variables: Seq[Variable[_]], indexes: Seq[String] = Seq())
  case class OutputContent(section: SectionContent*)
