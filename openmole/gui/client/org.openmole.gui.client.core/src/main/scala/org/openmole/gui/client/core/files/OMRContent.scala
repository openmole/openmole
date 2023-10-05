package org.openmole.gui.client.core.files

import org.openmole.gui.shared.data.*
import scaladget.bootstrapnative.bsn.*
import com.raquo.laminar.api.L.*
import org.openmole.gui.client.core.files.PlotContent.PlotContentSection
import org.openmole.gui.client.core.{CoreFetch, Panels}
import org.openmole.gui.client.core.files.TabContent.TabData
import org.openmole.gui.client.ext.*
import org.openmole.gui.shared.api.*

object OMRContent:

  def buildTab(safePath: SafePath, sections: Seq[GUIOMRSectionContent])(using panels: Panels, api: ServerAPI, basePath: BasePath, guiPlugins: GUIPlugins) =
    val (rowData: RowData, rawContent: String) = ResultData.fromOMR(sections)
    val pcSections = sections.map: s =>
      PlotContentSection(s.name.getOrElse("section"), rawContent, rowData, "initialHash")
    PlotContent.buildTab(safePath, FileContentType.OpenMOLEResult, pcSections)
