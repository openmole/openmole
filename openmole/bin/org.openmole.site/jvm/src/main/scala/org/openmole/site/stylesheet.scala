package org.openmole.site

import scalatags.Text.all._
import tools._

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

package object stylesheet {

  lazy val GREEN = "#a6bf26"

  lazy val DARK_GREY = "#555"

  lazy val LIGHT_GREY = "#e7e7e7"
  //
  //  lazy val VERY_LIGHT_GREY = "#e7e7e7"
  //
  //  lazy val BS_GREY = "#808080"
  //
  //  lazy val FUN_GREY = "#cccccc"

  def center(percentage: Int) = Seq(
    width := s"$percentage%",
    margin := "0 auto",
    display := "block"
  )

  def centerW(w: Int) = Seq(
    width := s"${w}px",
    margin := "0 auto",
    display := "block"
  )

  /* lazy val mainDiv = Seq(
    paddingTop := 100,
    paddingBottom := 50,
    minHeight := 800
  ) ++ center(50)*/

  def rightDetailButtons(topValue: Int) = Seq(
    top := topValue,
    minWidth := 230,
    lineHeight := "1em"
  )

  def leftDetailButtons(topValue: Int) = Seq(
    top := topValue,
    paddingRight := 50,
    minWidth := 230,
    lineHeight := "1em"
  )

  lazy val navigateDoc = Seq(
    fixedPosition,
    top := 200,
    fontSize := "32px",
    color := "black",
    textDecoration := "none",
    width := 30
  )

  lazy val stepHeader = Seq(
    width := "22%",
    fontSize := "22px",
    fontWeight := "bold",
    margin := "0 auto",
    minHeight := 85,
    width := "95%"
  ) ++ center(90)

  val headerImg = Seq(
    paddingRight := 20,
    marginBottom := 30,
    height := 80
  )

  lazy val previousDoc = Seq(
    float := "left",
    left := 230
  ) ++ navigateDoc

  lazy val nextDoc = Seq(
    right := 300,
    float := "right"
  ) ++ navigateDoc

  lazy val mainTitle = Seq(
    color := DARK_GREY,
    fontWeight := "bold",
    fontSize := "35px",
    padding := 10
  )

  lazy val mainText = Seq(
    color := DARK_GREY
  )

  lazy val centerBox = Seq(
    textAlign := "center"
  ) ++ center(70)

  lazy val centerBox100 = Seq(
    textAlign := "center"
  ) ++ center(100)

  lazy val footer = Seq(
    position := "relative",
    clear := "both",
    backgroundColor := "#222",
    color := "white"
  )

  val leftMole = Seq(
    float := "left",
    marginLeft := 20,
    textAlign := "right",
    maxHeight := 100
  )

  val memberStyle = Seq(
    color := DARK_GREY,
    fontSize := "25px",
    paddingTop := 5
  )

  val partners = Seq(
    width := 270,
    padding := 50
  )

  val smallPartners = Seq(
    width := 120,
    padding := 30
  )

  val h1Like = Seq(
    color := "#444",
    fontSize := "35px",
    fontWeight := "bold",
    textTransform := "uppercase",
    lineHeight := "50px"
  )

  def svgRunButton(top: Int) = Seq(
    position := "absolute",
    marginTop := top
  )

  def centerJustify(ratio: Int) = Seq(
    width := s"$ratio%",
    paddingTop := 10,
    textAlign := "justify",
    marginLeft := "auto",
    marginRight := "auto"
  )

  val suggest = Seq(
    padding := 25,
    float := "right"
  )
}
