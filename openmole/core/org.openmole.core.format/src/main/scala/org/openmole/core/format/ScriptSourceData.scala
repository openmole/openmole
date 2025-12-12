package org.openmole.core.format

import org.openmole.core.script.Imports.ImportedFile
import org.openmole.core.script.*
import org.openmole.tool.file.{File, *}

object ScriptSourceData:
  //implicit def defaultData: ScriptSourceData = NoData

  case class ScriptData(workDirectory: File, script: File) extends ScriptSourceData:
    val content = if script.exists() then script.content else ""
    val imports =
      val is = Imports.directImportedFiles(script).map(i => OMRContent.Import(ImportedFile.identifier(i), i.file.content))
      if is.isEmpty then None else Some(is)

  case object NoData extends ScriptSourceData

  def applySource(workDirectory: File, script: File) =
    def tq = "\"\"\""
    s"""${classOf[ScriptSourceData.ScriptData].getCanonicalName}(File($tq$workDirectory$tq), File($tq$script$tq))"""

  def scriptContent(scriptData: ScriptSourceData) =
    scriptData match
      case NoData        => ""
      case s: ScriptData => s.content

  def imports(scriptData: ScriptSourceData) =
    scriptData match
      case NoData => None
      case s: ScriptData => s.imports


sealed trait ScriptSourceData