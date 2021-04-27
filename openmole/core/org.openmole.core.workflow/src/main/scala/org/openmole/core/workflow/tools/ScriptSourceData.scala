package org.openmole.core.workflow.tools

import org.openmole.tool.file._

object ScriptSourceData {
  implicit def defaultData = NoData
  case class ScriptData(workDirectory: File, script: File) extends ScriptSourceData {
    val content = if (script.exists()) script.content else ""
  }
  case object NoData extends ScriptSourceData

  def scriptContent(scriptData: ScriptSourceData) =
    scriptData match {
      case NoData        ⇒ ""
      case s: ScriptData ⇒ s.content
    }

}

sealed trait ScriptSourceData