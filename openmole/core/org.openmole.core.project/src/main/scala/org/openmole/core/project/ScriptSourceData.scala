package org.openmole.core.project

import org.openmole.core.project.Imports.{ ImportedFile }
import org.openmole.tool.file._

object ScriptSourceData {
  implicit def defaultData = NoData

  case class ImportData(`import`: String, script: File)
  case class ScriptData(workDirectory: File, script: File, imports: Seq[ImportData]) extends ScriptSourceData {
    val content = if (script.exists()) script.content else ""
  }

  case object NoData extends ScriptSourceData

  def applySource(workDirectory: File, script: File, imports: Seq[ImportedFile]) = {
    def tq = "\"\"\""

    def importData(i: ImportedFile) = s"""${classOf[ScriptSourceData.ImportData].getCanonicalName}($tq${ImportedFile.identifier(i)}$tq, File($tq${i.file}$tq))"""
    s"""${classOf[ScriptSourceData.ScriptData].getCanonicalName}(File($tq$workDirectory$tq), File($tq$script$tq), Seq(${imports.map(importData).mkString(",")}))"""
  }

  def importsContent(scriptData: ScriptSourceData) =
    scriptData match {
      case NoData        ⇒ Seq()
      case s: ScriptData ⇒ s.imports
    }

  def scriptContent(scriptData: ScriptSourceData) =
    scriptData match {
      case NoData        ⇒ ""
      case s: ScriptData ⇒ s.content
    }

}

sealed trait ScriptSourceData