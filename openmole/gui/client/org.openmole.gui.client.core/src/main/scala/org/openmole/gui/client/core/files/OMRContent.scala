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
        case Some(gos: GUIOMRScript)=> s"""${gos.`import`.map(_.content + "\n")} \n\n ${gos.content}"""
        case _=> "Scrit unvailable"
    PlotContent.buildTab(safePath, FileContentType.OpenMOLEResult, pcSections, Some(OMRMetadata(scriptText, guiOMRContent.openMoleVersion)))
