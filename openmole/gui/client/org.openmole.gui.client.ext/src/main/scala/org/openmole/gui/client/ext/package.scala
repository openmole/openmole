package org.openmole.gui.client

/*
 * Copyright (C) 01/04/16 // mathieu.leclaire@openmole.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY, without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import scaladget.bootstrapnative.bsn.*
import scaladget.tools.*
import org.openmole.gui.shared.data.{ExecutionState, RelativePath}
import org.scalajs.dom
import com.raquo.laminar.api.L.*
import endpoints4s.fetch
import endpoints4s.fetch.EndpointsSettings
import org.openmole.gui.shared.api.CoreAPI

package object ext {

  type ClientSettings = EndpointsSettings
  trait APIClient extends fetch.future.Endpoints with fetch.JsonEntitiesFromCodecs

  class CoreAPIClientImpl(val settings: ClientSettings)
    extends CoreAPI with APIClient

  def coreAPIClient(endpointsSettings: ClientSettings) =
    new CoreAPIClientImpl(endpointsSettings)

  extension (f: org.scalajs.dom.File)
    def path: RelativePath = RelativePath(if f.webkitRelativePath.isEmpty then Seq(f.name) else f.webkitRelativePath.split('/').toSeq)

  lazy val omsheet = this

  //GENERAL
  lazy val grey = Seq(
    color := "grey"
  )

  lazy val BLUE = "#3086b5"

  lazy val RED = "#c83737"

  lazy val DARK_GREY = "#222"

  lazy val LIGHT_GREY = "#9d9d9d"

  lazy val VERY_LIGHT_GREY = "#e7e7e7"

  lazy val BS_GREY = "#808080"

  lazy val FUN_GREY = "#cccccc"

  lazy val WHITE = "white"

  lazy val textCenter = cls("text-center")
  lazy val textLeft = cls("text-left")
  lazy val textRight = cls("text-right")
  lazy val textJustify = cls("text-justify")
  lazy val textNoWrap = cls("text-nowrap")

  lazy val tableTag = Seq(
    marginLeft := "3",
    fontSize := "14",
    relativePosition,
    top := "4"
  )

  val bold = Seq(
    fontWeight := "bold"
  )

  val giFontFamily = fontFamily := "gi"

  lazy val relativePosition = position := "relative"

  lazy val absolutePosition = position := "absolute"

  lazy val fixedPosition = position := "fixed"

  lazy val flexColumn = cls := "flex-column"

  lazy val flexRow = cls := "flex-row"

  lazy val centerInDiv = cls := "center-in-div"

  lazy val fileItemWarning = cls := "file-item-warning"

  lazy val fileItemCancel = cls := "file-item-cancel"

  lazy val fileActions = cls := "file-actions"

  lazy val fileActionItems = cls := "file-action-items"

  lazy val verticalLine = cls := "file-action-border"

  lazy val navBarItem = cls := "navbar-item"

  lazy val toolBoxColor = "#795c85"

  lazy val centerElement = Seq(
    display := "flex",
    justifyContent := "center",
    //margin := "0 auto",
    color := LIGHT_GREY
  )

  lazy val centerFileToolBar = Seq(
    display := "table",
    margin := "0 auto",
    color := LIGHT_GREY
  )

  lazy val shutdown = Seq(
    color := DARK_GREY,
    fontStyle := "italic",
    paddingTop := "40",
    marginLeft := "-25"
  )
  // SCRIPT CLIENT
  lazy val mainNav0 = Seq(
    paddingLeft := "0",
    borderColor := DARK_GREY,
    zIndex := "10",
    paddingRight := "40"
  )

  lazy val mainNav370 = Seq(
    paddingLeft := "370",
    borderColor := DARK_GREY,
    zIndex := "10",
    paddingRight := "40"
  )

  lazy val mainNav = Seq(
    left := "370",
    fontSize := "20"
  )

  lazy val displayOff = Seq(
    display := "none"
  )

  lazy val fullpanel = Seq(
    height := "100%",
    width := "100%"
  )

  lazy val panelReduce = Seq(
    left := "330",
    paddingRight := "340",
    fixedPosition,
    top := "70",
    transition := "all 0.1 s ease - out 0 s",
    width := "99%",
    height := "calc (100 % -140 px)"
  )

  lazy val panelOpen = Seq(
    left := "0"
  )

  lazy val centerpanel = Seq(
    height := "89%",
    paddingLeft := "15",
    paddingRight := "15",
    relativePosition,
    top := "30",
    width := "100%"
  )

  lazy val leftpanel = Seq(
    background := s"$DARK_GREY none repeat scroll 0 0",
    height := "100%",
    left := "-320",
    opacity := 1,
    overflowY := "auto",
    paddingLeft := "7",
    absolutePosition,
    top := "37",
    transition := "all 0.1 s ease - out 0 s",
    width := "320"
  )

  lazy val logoVersion = Seq(
    width := "200",
    position := "fixed",
    bottom := "0",
    right := "50",
    zIndex := "-2"
  )

  lazy val resetBlock = Seq(
    cursor.pointer,
    relativePosition,
    float.right,
    right := "-30",
    top := "20",
    zIndex := 1101,
    fontSize := "22",
    color := BLUE
  )

  lazy val settingsBlock = Seq(
    paddingTop := "8",
    color := BLUE
  )

  lazy val closeBanner = Seq(
    float.right,
    cursor.pointer,
    relativePosition,
    top := "18",
    zIndex := "10",
    paddingRight := "15",
    fontSize := "20"
  )

  lazy val fixed = Seq(
    fixedPosition,
    width := "100%"
  )

  lazy val absoluteFullWidth = Seq(
    absolutePosition,
    width := "100%"
  )

  lazy val shutdownButton = Seq(
    fontSize := "12",
    verticalAlign := "middle",
    marginLeft := "10",
    zIndex := 1101
  )

  lazy val settingsItemStyle = Seq(
    fontSize := "12",
    color := DARK_GREY,
    cursor.pointer
  )

//  lazy val connectionBlock = Seq(
//    display := "inline-block",
//    textAlign := "right",
//    float := "right",
//    marginRight := "15"
//  )

  lazy val textVersion = Seq(
    color := "grey",
    position := "fixed",
    textAlign := "center",
    right := "27",
    zIndex := -1,
    bottom := "5"
  )

  //TREENODE PANEL
  lazy val fileInfo = Seq(
    absolutePosition,
    width := "120",
    right := "2",
    marginTop := "6",
    textAlign := "right"
  )

  lazy val fileSize = Seq(
    color := "lightgray",
    fontSize := "10"
  )

  lazy val file = Seq(
    color := WHITE,
    display := "inline-block",
    height := "20",
    textDecoration := "none"
  )

  lazy val divAlertPosition = Seq(
    float.right,
    marginRight := "70",
    marginTop := "20"
  )

  lazy val treeprogress = Seq(
    marginTop := "20",
    width := "100%"
  )

  lazy val message = Seq(
    color := "#999",
    fontStyle := "italic",
    marginLeft := "25",
    marginTop := "38"
  )

  lazy val fileSelectionOverlay = Seq(
    right := "0",
    cursor.pointer,
    width := "360",
    height := "24",
    marginLeft := "-5",
    marginBottom := "1",
    borderRadius := "2px"
  )

  lazy val fileSelected = Seq(
    backgroundColor := "#a6bf26"
  ) ++ fileSelectionOverlay

  lazy val fileSelectedForDeletion = Seq(
    backgroundColor := "#d9534f"
  ) ++ fileSelectionOverlay

  lazy val fileSelectionMessage = Seq(
    float.right,
    padding := "4",
    fontSize := "13",
    right := "6",
    color := WHITE
  )

  lazy val pasteLabel = Seq(
    relativePosition,
    top := "30",
    cursor.pointer,
    paddingBottom := "7"
  )

  lazy val moreEntries = Seq(
    backgroundColor := WHITE,
    margin := "10",
    borderRadius := "3px",
    textAlign := "center"
  )

  lazy val moreEntriesText = Seq(
    color := "#444",
    fontSize := "12",
    fontStyle := "italic",
    padding := "7"
  )

  //TREENODE TABS

  lazy val tabContent = Seq(
    marginTop := "-1",
    relativePosition,
    width := "100%"
  )

  lazy val playTabOverlay = Seq(
    color := WHITE,
    height := "100%",
    absolutePosition,
    width := "100%",
    background := BLUE,
    opacity := 0.9,
    width := "100%",
    zIndex := 10,
    bottom := "3",
    borderBottomLeftRadius := "5px",
    borderBottomRightRadius := "5px",
    borderTopRightRadius := "5px"
  )

  lazy val overlayElement = Seq(
    absolutePosition,
    width := "100%",
    color := WHITE,
    paddingTop := "100",
    fontSize := "25",
    zIndex := 25,
    textAlign := "center"
  )

  lazy val executionTable = Seq(
    backgroundColor := DARK_GREY,
    color := WHITE
  )

  lazy val monospace = fontFamily := "monospace"

  def fileList = {
    Seq(
      height := (dom.window.innerHeight - 230).toString, //nbElements * 21, //  <-- Select the height of the body
      absolutePosition,
      fontSize := "14",
      listStyleType := "none",
      marginTop := "50",
      paddingTop := "5",
      marginLeft := "-7",
      paddingLeft := "10",
      width := "370",
      overflowY := "auto",
      overflowX := "hidden"
    )
  }

  lazy val tabClose = Seq(
    relativePosition,
    fontSize := "17",
    right := "-7"
  )

  //EDITOR
  lazy val editorContainer = Seq(
    padding := "0",
    relativePosition,
    height := "100%",
    width := "100%"
  )

  lazy val activeTab = Seq(
    backgroundColor := FUN_GREY,
    bold
  )

  lazy val unActiveTab = Seq(
    border := s"1px solid $VERY_LIGHT_GREY"
  )

  //PANELS
  def panelWidth(ratio: Int) = Seq(
    width := s"${ratio.toString}%",
    maxWidth := "1250"
  )

  //EXECUTION PANEL
  lazy val execOutput = Seq(
    marginRight := "-10",
    marginTop := "5"
  )

  lazy val execLevel = Seq(
    marginRight := "-25",
    marginTop := "-3"
  )

  lazy val errorTable = Seq(
    lineHeight := "30px",
    borderWidth := "0.1em",
    borderStyle := "solid",
    borderColor := "#ccc",
    borderLeft := "0",
    borderRight := "0"
  )

  lazy val executionVisible = Seq(
    color := BLUE
  )

  def executionState(state: ExecutionState) = Seq(
    state match {
      case _: ExecutionState.Failed   ⇒ color := "#CC3A36"
      case _: ExecutionState.Running  ⇒ color := "yellow"
      case _: ExecutionState.Finished ⇒ color := "#a6bf26"
      case _: ExecutionState.Canceled ⇒ color := "orange"
      case _                         ⇒ color := "#fff"
    },
    fontWeight := "bold"
  )

  lazy val rowLayout = Seq(
    display := "table",
    tableLayout := "fixed"
  //borderSpacing := 5
  )

  lazy val columnLayout = Seq(
    display := "table-cell"
  )

  lazy val closeDetails = Seq(
    cursor.pointer,
    color := DARK_GREY,
    fontSize := "22",
    verticalAlign := "middle",
    opacity := 0.4
  )

  lazy val environmentErrorBadge = Seq(
    color := DARK_GREY,
    backgroundColor := WHITE,
    width := "auto",
    height := "15",
    marginLeft := "8",
    padding := "1px 5px 5px 5px"
  )

  //OPTON DIVS
  lazy val optionsdiv = Seq(
    relativePosition,
    marginRight := "10",
    top := "-3"
  )

  // SELECTS
  lazy val selectFilter = Seq(
    marginTop := "6",
    fontSize := "14",
    paddingLeft := "5",
    borderBottomRightRadius := "0",
    borderBottomLeftRadius := "0"
  )

  // ALERT PANELS

  lazy val alertOverlay = Seq(
    background := s"$DARK_GREY none repeat scroll 0 0",
    opacity := 0.95,
    color := WHITE,
    height := "100%",
    absolutePosition,
    width := "100%",
    zIndex := 1200,
    textAlign := "right",
    top := "0"
  )

  // POSTIONING
  lazy val fullPageZone = Seq(
    top := "0",
    height := "100%",
    width := "100%"
  )

  lazy val fileZone = Seq(
    top := "10",
    height := "100%",
    width := "370"
  )

  lazy val topZone = Seq(
    top := "0",
    height := "100",
    width := "100"
  )

  def centerPage(topPosition: String = "45%") = Seq(
    //cf screen-center css rule
    backgroundColor := "red",
  //    position := "fixed",
  //    top := topPosition,
  //    left := "50%",
  //    minWidth := "250",
  // svg.transform := "translate (-50%,-50%)"
  )

  lazy val relativeCenter = Seq(
    relativePosition,
    top := "50%",
    textAlign := "center"
  )

  lazy val rightPage = Seq(
    right := "0",
    margin := "10 20"
  )

  // ENVIRONMENT ERROR PANEL
  lazy val environmentPanelError = Seq(
    backgroundColor := WHITE,
    margin := "10 10 0",
    padding := "10"
  )

  //MODEL WIZARD PANEL
  lazy val modelNameInput = Seq(
    height := "34",
    width := "150",
    borderRadius := "1px"
  )

  lazy val rightBlock = Seq(
    width := "70%",
    float.right
  )

  lazy val onecolumn = Seq(
    float.left,
    width := "100%"
  )

  lazy val twocolumns = Seq(
    float.left,
    width := "50%"
  )

  lazy val modelIO = Seq(
    relativePosition,
    left := "50%",
    marginLeft := "-60",
    paddingBottom := "20"
  )

  lazy val modelHelp = Seq(
    height := "auto",
    backgroundColor := "rgb(255, 221, 85)",
    padding := "10",
    marginTop := "20",
    top := "20",
    borderRadius := "5px",
    color := "#444"
  )

  lazy val columnCSS = Seq(
    width := "50%",
    display := "inline-block",
    padding := "15"
  )

  // PLUGIN PANEL
  lazy val uploadPlugin = Seq(
    marginRight := "50",
    paddingBottom := "30",
    width := "45",
    height := "30"
  )

  lazy val pluginRight = Seq(
    right := "-5",
    marginTop := "-9"
  )

  lazy val spinnerStyle = Seq(
    backgroundColor := DARK_GREY,
    textAlign := "center",
    borderRadius := "4px"
  )

  lazy val dropdownError = Seq(
    height := "300",
    width := "100%",
    color := DARK_GREY,
    relativePosition,
    borderRadius := "5",
    fontSize := "12",
    border := "1px solid #ccc",
    padding := "10",
    lineHeight := "0.5cm",
    wordWrap := "break-word"
  )

  lazy val fixedTable = Seq(
    tableLayout := "fixed",
    width := "100%"
  )

  //FILE TOOL BAR
  lazy val selectedTool = Seq(
    opacity := 1,
    svg.transform := "scale(1.2)",
    color := BLUE
  )

  lazy val borderRightFlat = Seq(
    borderBottomRightRadius := "0",
    borderTopRightRadius := "0"
  )

  lazy val smallInput = Seq(
    width := "60",
    paddingTop := "5",
    paddingLeft := "3",
    textAlign := "center",
    height := "28",
    fontSize := "14"
  )

  lazy val filterElement = Seq(
    float.left,
    height := "30",
    paddingLeft := "4",
    fontSize := "12"
  )

  lazy val selectedButton = Seq(
    btn,
    backgroundColor := "#e3dbdb"
  )

  lazy val sortingBar = Seq(
    relativePosition,
    height := "20",
    float.right,
    top := "-130"
  )

  lazy val tdStyle = Seq(
    colBS(2),
    marginBottom := "8",
    paddingLeft := "5",
    width := "auto",
    height := "25"
  )

  //TOOLTIP
  lazy val warningTooltip = Seq(
    absolutePosition,
    display := "inline-block",
    width := "auto",
    maxWidth := "200",
    height := "auto",
    padding := "8",
    borderRadius := "4px",
    backgroundColor := "pink",
    color := "red",
    boxShadow := "0 8px 6px -6px black"
  )

  //DOC 
  lazy val greenBold = Seq(
    color := "#a6bf26",
    fontWeight := "bold"
  )

  lazy val labelInLine = Seq(
    relativePosition,
    display := "inline"
  )

  lazy val glyphText = Seq(
    color := "#a6bf26",
    fontSize := "22",
    paddingRight := "5"
  )

  lazy val docContent = Seq(
    opacity := 0.95,
    padding := "5 50"
  )

  //BANNER ALERT
  lazy val bannerAlert = Seq(
    width := "100%",
    absolutePosition,
    height := "70",
    zIndex := 5
  )

  lazy val bannerAlertInner = Seq(
    width := "100%",
    relativePosition,
    color := WHITE,
    opacity := 0.9,
    paddingTop := "10",
    paddingLeft := "15",
    paddingRight := "15",
    paddingBottom := "15"
  )

  //GENERAL SETTINGS

  lazy val generalSettings = Seq(
    height := "250",
    marginBottom := "20"
  )

}
