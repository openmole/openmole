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

  lazy val center = Seq(
    width := "50%",
    margin := "0 auto"
  )

  lazy val mainDiv = Seq(
    paddingTop := 150,
    paddingBottom := 350
  ) ++ center

  lazy val detailButtons = Seq(
    float := "left",
    fixedPosition,
    top := 500,
    marginLeft := -200
  )

  lazy val navigateDoc = Seq(
    float := "left",
    fixedPosition,
    top := 200,
    fontSize := "32px",
    color := "black",
    textDecoration := "none"
  )

  lazy val stepHeader = Seq(
    width := "22%",
    fontSize := "22px",
    fontWeight := "bold",
    margin := "0 auto",
    width := "50%"
  )

  lazy val previousDoc = Seq(
    marginLeft := -200
  ) ++ navigateDoc

  lazy val nextDoc = Seq(
    paddingLeft := "calc(50% + 200px)"
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
  ) ++ center

  lazy val footer = Seq(
    position := "absolute",
    right := 0,
    bottom := 0,
    left := 0,
    padding := "1rem"
  )
}