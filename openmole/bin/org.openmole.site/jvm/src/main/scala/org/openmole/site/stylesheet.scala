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

  lazy val DARK_GREY = "#222"

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

  lazy val mainDiv = Seq(
    paddingTop := 150,
    paddingBottom := 450
  ) ++ center(50)

  def detailButtons(topValue: Int) = Seq(
    float := "left",
    fixedPosition,
    top := topValue,
    marginLeft := -320,
    textAlign := "right",
    minWidth := 180
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
  )

  val headerImg = Seq(
    padding := 10,
    width := 130
  )

  lazy val previousDoc = Seq(
    float := "left",
    left := 300
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
    textAlign := "center",
    width := "60%"
  ) ++ center(50)

  lazy val footer = Seq(
    position := "absolute",
    right := 0,
    bottom := 0,
    left := 0,
    padding := "1rem"
  )

  val memberStyle = Seq(
    color := DARK_GREY,
    fontSize := "25px",
    fontWeight := "bold",
    paddingTop := 5
  )

  val communityStyle = Seq(
    fontSize := "30px",
    color := DARK_GREY,
    backgroundColor := "red"
  )

  val partners = Seq(
    width := 270,
    padding := 50
  )

  val h1Like = Seq(
    color := "#444",
    fontSize := "32px",
    fontWeight := 800,
    margin := "0 0 24px",
    textTransform := "uppercase",
    paddingTop := 100
  )

  val svgRunButton = Seq(
    position := "absolute",
    marginTop := -70
  )
}