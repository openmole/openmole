package org.openmole.gui.misc.utils

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

import scalatags.JsDom.all._
import scalatags.JsDom.styles
import scalatags.JsDom.svgAttrs
import fr.iscpif.scaladget.stylesheet.{ all ⇒ sheet }
import sheet._

package object stylesheet {

  //GENERAL
  lazy val grey: ModifierSeq = Seq(
    color("grey"),
    opacity := 0.8
  )

  lazy val tableTag: ModifierSeq = Seq(
    sheet.marginLeft(3),
    fontSize := 14,
    relativePosition,
    top := 4
  )

  def color(col: String): ModifierSeq = styles.color := col

  def colorBold(col: String): ModifierSeq = Seq(
    color(col),
    fontWeight := "bold"
  )

  lazy val relativePosition: ModifierSeq = position := "relative"

  lazy val absolutePosition: ModifierSeq = position := "absolute"

  lazy val fixedPosition: ModifierSeq = position := "fixed"

  lazy val centerElement: ModifierSeq = Seq(
    display := "table",
    margin := "0 auto"
  )

  // SCRIPT CLIENT
  lazy val connectionTabOverlay: ModifierSeq = Seq(
    background := "white none repeat scroll 0 0",
    color("white"),
    height := "100%",
    styles.left := 0,
    absolutePosition,
    width := "100%",
    zIndex := 998,
    top := 0
  )

  lazy val openmoleLogo: ModifierSeq = Seq(
    fixedPosition,
    width := 600,
    top := -30,
    zIndex := "-1",
    styles.right := "calc(50% - 250px)"
  )

  lazy val displayOff: ModifierSeq = Seq(
    display := "none"
  )

  lazy val fullpanel: ModifierSeq = Seq(
    height := "100%",
    width := "100%"
  )

  lazy val panelReduce: ModifierSeq = Seq(
    styles.left := 330,
    sheet.paddingRight(340),
    fixedPosition,
    top := 60,
    transition := "all 0.1 s ease - out 0 s",
    width := "99%",
    height := "calc (100 % -140 px)"
  )

  lazy val panelOpen: ModifierSeq = Seq(
    styles.left := 0
  )

  lazy val centerpanel: ModifierSeq = Seq(
    height := "89%",
    sheet.paddingLeft(15),
    sheet.paddingRight(15),
    relativePosition,
    top := 30,
    width := "100%"
  )

  lazy val leftpanel: ModifierSeq = Seq(
    background := "#333333 none repeat scroll 0 0",
    height := "100%",
    styles.left := -320,
    opacity := 1,
    overflowY := "auto",
    sheet.paddingLeft(7),
    absolutePosition,
    top := 37,
    transition := "all 0.1 s ease - out 0 s",
    width := 320
  )

  lazy val logoVersion: ModifierSeq = Seq(
    width := 200,
    position := "fixed",
    bottom := 0,
    styles.right := 50,
    zIndex := -2
  )

  lazy val resetBlock: ModifierSeq = Seq(
    absolutePosition,
    top := 10,
    right := 10
  )

  lazy val shutdownButton: ModifierSeq = Seq(
    fontSize := 20,
    verticalAlign := "middle",
    sheet.marginLeft(10)
  )

  lazy val resetPassword: ModifierSeq = Seq(
    fontSize := 12,
    color("grey"),
    cursor := "pointer"
  )

  lazy val connectionBlock: ModifierSeq = Seq(
    display := "inline-block",
    textAlign := "right",
    float := "right",
    sheet.marginRight(15)
  )

  lazy val textVersion: ModifierSeq = Seq(
    fontFamily := "Bevan",
    color("#ff5555"),
    position := "fixed",
    right := -40,
    bottom := 0,
    fontSize := "2em",
    width := "300",
    zIndex := -1,
    bottom := 7,
    height := 30
  )

  //TREENODE PANEL
  lazy val fileInfo: ModifierSeq = Seq(
    sheet.floatRight,
    absolutePosition,
    right := 2,
    sheet.marginTop(1)
  )

  lazy val fileSize: ModifierSeq = Seq(
    color("lightgray"),
    sheet.floatRight,
    fontSize := 12,
    sheet.paddingLeft(3),
    sheet.paddingRight(7),
    sheet.paddingTop(3)
  )

  lazy val file: ModifierSeq = Seq(
    color("white"),
    display := "inline-block",
    sheet.marginTop(2),
    sheet.marginBottom(3),
    textDecoration := "none"
  )

  lazy val dir: ModifierSeq = Seq(
    backgroundColor := "#3086b5",
    color("white"),
    borderRadius := 5,
    display := "inline-block",
    height := 22,
    sheet.marginBottom(5),
    padding := "3 4",
    width := 22,
    borderRadius := "4px"
  )

  lazy val fileNameOverflow: ModifierSeq = Seq(
    color("white"),
    whiteSpace := "nowrap",
    overflow := "hidden",
    width := "13em",
    sheet.paddingTop(1),
    sheet.paddingLeft(3),
    floatLeft,
    pointer,
    textOverflow := "ellipsis"
  )

  lazy val treeprogress: ModifierSeq = Seq(
    sheet.marginTop(20),
    width := "100%"
  )

  lazy val message: ModifierSeq = Seq(
    color("#999"),
    fontStyle := "italic",
    sheet.marginLeft(25),
    sheet.marginTop(38)
  )

  //TREENODE TABS
  lazy val editingElement: ModifierSeq = Seq(
    fontSize := 18,
    sheet.paddingLeft(5),
    position := "fixed",
    styles.right := 80,
    top := 150,
    width := 30,
    zIndex := 800
  )

  lazy val tabContent: ModifierSeq = Seq(
    sheet.marginTop(-1),
    relativePosition,
    width := "100%"
  )

  lazy val playTabOverlay: ModifierSeq = Seq(
    color("white"),
    height := "100%",
    left := 0,
    absolutePosition,
    width := "100%",
    background := "rgba(0, 0, 0, 0.6) none repeat scroll 0 0",
    width := "100%",
    zIndex := 20
  )

  lazy val overlayElement: ModifierSeq = Seq(
    absolutePosition,
    width := "100%",
    color("white"),
    sheet.paddingTop(100),
    fontSize := 25,
    zIndex := 25,
    textAlign := "center"
  )

  lazy val executionElement: ModifierSeq = Seq(
    fixedPosition,
    right := 80,
    top := 150,
    width := 50,
    zIndex := 18
  )

  lazy val monospace: ModifierSeq = fontFamily := "monospace"

  //EDITOR
  lazy val editorContainer: ModifierSeq = Seq(
    padding := 0,
    width := "100%"
  )

  //MARKET PANEL
  lazy val docEntry: ModifierSeq = Seq(
    color("white"),
    backgroundColor := "#333",
    borderRadius := 3,
    sheet.marginTop(3),
    sheet.paddingTop(6),
    verticalAlign := "middle",
    sheet.paddingBottom(4),
    sheet.paddingLeft(5),
    minHeight := 46
  )

  lazy val docTitleEntry: ModifierSeq = Seq(
    fontWeight := "bold",
    cursor := "pointer",
    sheet.paddingTop(6)
  )

  //EXECUTION PANEL
  lazy val executionHeader: ModifierSeq = Seq(
    floatRight,
    width := 250,
    sheet.marginRight(40),
    sheet.marginTop(5)
  )

  lazy val execOutput: ModifierSeq = Seq(
    sheet.marginRight(-10),
    sheet.marginTop(5)
  )

  lazy val execLevel: ModifierSeq = Seq(
    sheet.marginRight(-25),
    sheet.marginTop(-3)
  )

  //OPTON DIVS
  lazy val optionsdiv: ModifierSeq = Seq(
    relativePosition,
    sheet.marginRight(10),
    top := -3
  )

  // SELECTS
  lazy val selectFilter: ModifierSeq = Seq(
    sheet.marginTop(6),
    fontSize := 14,
    sheet.paddingLeft(5),
    borderBottomRightRadius := 0,
    borderBottomLeftRadius := 0
  )

  // ALERT PANELS

  lazy val alertOverlay: ModifierSeq = Seq(
    background := "#333 none repeat scroll 0 0",
    opacity := 0.95,
    color("white"),
    height := "100%",
    absolutePosition,
    width := "100%",
    zIndex := 1200,
    textAlign := "right",
    top := 0
  )

  // POSTIONING
  lazy val fullPageZone: ModifierSeq = Seq(
    top := 0,
    height := "100%",
    width := "100%"
  )

  lazy val fileZone: ModifierSeq = Seq(
    top := 10,
    height := "100%",
    width := 320
  )

  lazy val topZone: ModifierSeq = Seq(
    top := 0,
    height := 100,
    width := 100
  )

  lazy val centerPage: ModifierSeq = Seq(
    position := "fixed",
    top := "45%",
    styles.left := "50%",
    minWidth := 250,
    svgAttrs.transform := "translate (-50%,-50%)"
  )

  lazy val relativeCenter: ModifierSeq = Seq(
    relativePosition,
    top := "50%",
    textAlign := "center"
  )

  lazy val rightPage: ModifierSeq = Seq(
    styles.right := 0,
    margin := "10 20"
  )

  // AUTHENTICATION PANEL
  lazy val certificate: ModifierSeq = Seq(
    width := 360,
    textAlign := "center",
    sheet.marginTop(40)
  )

  // ENVIRONMENT ERROR PANEL
  lazy val environmentPanelError: ModifierSeq = Seq(
    backgroundColor := "white",
    margin := "10 10 0",
    padding := 10
  )

  //MODEL WIZARD PANEL
  lazy val modelNameInput: ModifierSeq = Seq(
    height := 34,
    width := 150,
    borderRadius := 1
  )

  lazy val rightBlock: ModifierSeq = Seq(
    width := "70%",
    floatRight
  )

  lazy val onecolumn: ModifierSeq = Seq(
    floatLeft,
    width := "100%"
  )

  lazy val twocolumns: ModifierSeq = Seq(
    floatLeft,
    width := "50%"
  )

  lazy val modelIO: ModifierSeq = Seq(
    relativePosition,
    left := "50%",
    sheet.marginLeft(-60),
    sheet.paddingBottom(20)
  )

  // PLUGIN PANEL
  lazy val uploadPlugin: ModifierSeq = Seq(
    sheet.marginRight(110),
    sheet.paddingBottom(30)
  )

  lazy val pluginRight: ModifierSeq = Seq(
    right := 20,
    sheet.marginTop(-8)
  )

  //FILE TOOL BAR
  lazy val selectedTool: ModifierSeq = Seq(
    opacity := 1,
    svgAttrs.transform := "scale(1.2)",
    color("#3086b5;")
  )

  lazy val borderRightFlat: ModifierSeq = Seq(
    borderBottomRightRadius := 0,
    borderTopRightRadius := 0
  )

  lazy val smallInput: ModifierSeq = Seq(
    width := 60,
    sheet.paddingTop(5),
    sheet.paddingLeft(3),
    textAlign := "center",
    height := (28),
    fontSize := 14
  )

  lazy val filterElement: ModifierSeq = Seq(
    sheet.floatLeft,
    height := 30,
    sheet.paddingLeft(4),
    fontSize := 12
  )

  lazy val selectedButton: ModifierSeq = Seq(
    sheet.btn,
    backgroundColor := "#e3dbdb;"
  )

}
