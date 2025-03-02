package org.openmole.gui.client.core.files

import org.openmole.gui.shared.data.*
import scaladget.bootstrapnative.bsn.*
import com.raquo.laminar.api.L.*
import com.raquo.laminar.DomApi
import org.openmole.gui.client.core.files.PlotContent.*
import org.openmole.gui.client.core.{CoreFetch, Panels}
import org.openmole.gui.client.core.files.TabContent.TabData
import org.openmole.gui.client.ext.*
import org.openmole.gui.shared.api.*

object OMRContent:

  def buildTab(
    safePath: SafePath,
    guiOMRContent: GUIOMRContent,
    contentStates: ContentStates = ContentStates(),
    currentState: ContentState = TableState(false),
    currentIndex: Option[Int] = None
    )(using panels: Panels, api: ServerAPI, basePath: BasePath, guiPlugins: GUIPlugins): (TabData, HtmlElement) =

    val pcSections = guiOMRContent.section.map: s =>
      val rowData = ResultData.fromOMR(s)
      ContentSection(s.name.getOrElse("section"), guiOMRContent.raw, rowData, "initialHash")

    val scriptText =
      guiOMRContent.script match
        case Some(gos: GUIOMRScript)=>
          if gos.`import`.isEmpty
          then gos.content
          else
            def importedScript(imp: GUIOMRImport) =
              s"""<i><b>Imported file ${imp.`import`}:</b></i>
                 |${imp.content}""".stripMargin

            def imported = gos.`import`.map(importedScript).mkString("\n\n")

            def script =
              s"""<i><b>Script:</b></i>
                 |${gos.content}""".stripMargin

            s"""$imported
               |
               |$script
               |""".stripMargin


        case _=> "<i>Script not available</i>"

    def replaceWithHTML(s: String): HtmlElement =
      val html = s"<div>${s.replace(" ", "&nbsp;").replace("\n", "<br/>")}</div>"
      foreignHtmlElement(DomApi.unsafeParseHtmlString(html))



    PlotContent.buildTab(
      safePath,
      FileContentType.OpenMOLEResult,
      pcSections,
      pcSections.map(_.section).head,
      contentStates,
      currentState,
      omrMetadata = Some(OMRMetadata(replaceWithHTML(scriptText), guiOMRContent.openMoleVersion, guiOMRContent.timeStart, guiOMRContent.index.isDefined)),
      currentIndex = currentIndex
    )