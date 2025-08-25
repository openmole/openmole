package org.openmole.gui.client.core.files

import org.openmole.gui.shared.data.*
import scaladget.bootstrapnative.bsn.*
import com.raquo.laminar.api.L.*
import org.openmole.gui.client.core.files.PlotContent.ContentSection
import org.openmole.gui.client.core.{Panels}
import org.openmole.gui.client.core.files.TabContent.TabData
import org.openmole.gui.client.ext.*
import org.openmole.gui.shared.api.*

object CSVContent:

  def buildTab(safePath: SafePath, initialContent: String, initialHash: String)(using panels: Panels, api: ServerAPI, basePath: BasePath, guiPlugins: GUIPlugins) =
    val rowData: RowData = ResultData.fromCSV(initialContent)
    PlotContent.buildTab(safePath, FileContentType.CSV, Seq(ContentSection("CSV", initialContent, rowData, initialHash)), "CSV")
