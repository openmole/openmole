package org.openmole.core.project

import org.openmole.core.project.Imports.{ ImportedFile }
import org.openmole.tool.file._

object ScriptSourceData {
  implicit def defaultData = NoData

  case class ScriptData(workDirectory: File, script: File) extends ScriptSourceData {
    val content = if (script.exists()) script.content else ""
  }

  case object NoData extends ScriptSourceData

  def applySource(workDirectory: File, script: File) = {
    def tq = "\"\"\""
    s"""${classOf[ScriptSourceData.ScriptData].getCanonicalName}(File($tq$workDirectory$tq), File($tq$script$tq))"""
  }

  def scriptContent(scriptData: ScriptSourceData) =
    scriptData match {
      case NoData        ⇒ ""
      case s: ScriptData ⇒ s.content
    }

}

sealed trait ScriptSourceData