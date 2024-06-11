package org.openmole.gui.client.core.files

import org.openmole.gui.shared.data.*
import scaladget.bootstrapnative.bsn.*
import com.raquo.laminar.api.L.*
import org.openmole.gui.client.core.files.PlotContent.{OMRMetadata, PlotContentSection}
import org.openmole.gui.client.core.{CoreFetch, Panels}
import org.openmole.gui.client.core.files.TabContent.TabData
import org.openmole.gui.client.ext.*
import org.openmole.gui.shared.api.*

object OMRContent:

  def buildTab(safePath: SafePath, guiOMRContent: GUIOMRContent)(using panels: Panels, api: ServerAPI, basePath: BasePath, guiPlugins: GUIPlugins) =
    val (rowData: RowData, rawContent: String) = ResultData.fromOMR(guiOMRContent.section)
    val pcSections = guiOMRContent.section.map: s =>
      PlotContentSection(s.name.getOrElse("section"), rawContent, rowData, "initialHash")
    val scriptText =
      guiOMRContent.script match
        case Some(gos: GUIOMRScript)=>
          if gos.`import`.isEmpty
          then gos.content
          else
            def importedScript(imp: GUIOMRImport) =
              s"""Imported file ${imp.`import`}:
                 |${imp.content}
                 |${"-"* 20}""".stripMargin

            def imported = gos.`import`.map(importedScript).mkString("\n\n")

            def script =
              s"""Script:
                 |${gos.content}""".stripMargin

            s"""$imported
               |
               |$script
               |""".stripMargin
        case _=> "Script not available"

    PlotContent.buildTab(
      safePath,
      FileContentType.OpenMOLEResult,
      pcSections,
      Some(OMRMetadata(scriptText, guiOMRContent.openMoleVersion, guiOMRContent.timeStart))
    )
