package org.openmole.gui.misc.js

/*
 * Copyright (C) 13/01/15 // mathieu.leclaire@openmole.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import fr.iscpif.scaladget.api.{BootstrapTags => bs, ClassKeyAggregator}
import bs._
import org.scalajs.dom.raw.{HTMLDivElement, HTMLInputElement}
import rx._
import scalatags.JsDom.all._
import org.openmole.gui.misc.js.JsRxTags._
import scalatags.JsDom.{tags ⇒ tags}

object Select {
  implicit def seqToSeqOfEmptyPairs[T <: Displayable](s: Seq[T]): Seq[(T, ClassKeyAggregator)] = s.map {
    (_, emptyCK)
  }

  implicit def seqOfTupleToSeqOfT[T <: Displayable](s: Seq[(T, _)]): Seq[T] = s.map {
    _._1
  }

  def apply[T <: Displayable](autoID: String,
                              contents: Seq[(T, ClassKeyAggregator)],
                              default: Option[T],
                              key: ClassKeyAggregator = emptyCK,
                              onclickExtra: () ⇒ Unit = () ⇒ {}) = new Select(autoID, Var(contents), default, key, onclickExtra)
}

import Select._

class Select[T <: Displayable](autoID: String,
                               private val contents: Var[Seq[(T, ClassKeyAggregator)]],
                               default: Option[T] = None,
                               key: ClassKeyAggregator = emptyCK,
                               onclickExtra: () ⇒ Unit = () ⇒ {}) {

  val content: Var[Option[T]] = Var(contents().size match {
    case 0 ⇒ None
    case _ ⇒ default match {
      case None ⇒ Some(contents()(0)._1)
      case _ ⇒
        val ind = contents().map {
          _._1
        }.indexOf(default.get)
        if (ind != -1) Some(contents()(ind)._1) else Some(contents()(0)._1)
    }
  })

  val hasFilter = Var(false)
  val filtered: Var[Seq[T]] = Var(contents())
  resetFilter

  lazy val inputFilter: HTMLInputElement = bs.input("", "selectFilter")(placeholder := "Filter", oninput := { () =>
    filtered() = contents().filter {
      _._1.name.toUpperCase.contains(inputFilter.value.toUpperCase)
    }
  }).render

  val glyphMap = Var(contents().toMap)

  def isInContents(t: T) = {
    val oo = contents().map{c=> Some(c._1)}.contains(t)
    println(content() + " ? " + oo)
    oo
  }

  def resetFilter = {
    filtered() = contents().take(100)
    content() = None
  }

  def setContents(cts: Seq[T]) = {
    contents() = cts
    content() = cts.headOption
    resetFilter
    glyphMap() = contents().toMap
    inputFilter.value = ""
  }

  def emptyContents = {
    contents() = Seq()
    content() = None
  }

  def isContentsEmpty = contents().isEmpty

  lazy val selector = {
    lazy val bg: HTMLDivElement = bs.div("dropdown")(
      tags.span(
        `class` := "btn " + key.key + " dropdown-toggle", `type` := "button", "data-toggle".attr := "dropdown", cursor := "pointer")(
          Rx {
            content().map { c ⇒
              bs.glyph(glyphMap()(c))
            }
          },
          Rx {
            content().map {
              _.name
            }.getOrElse(contents()(0)._1.name) + " "
          },
          bs.span("caret")
        ).render,
      ul(`class` := "dropdown-menu", id := autoID)(
        if (hasFilter())
          scalatags.JsDom.tags.li(
            tags.form(inputFilter)(`type` := "submit", onsubmit := { () =>
              content() = filtered().headOption
              bg.click()
              false
            }))
        else tags.div,
        Rx {
          tags.div(
            if (filtered().size < 100) {
              for (c ← filtered()) yield {
                scalatags.JsDom.tags.li(`class` := "selectElement", cursor := "pointer", role := "presentation", onclick := { () ⇒
                  content() = contents().filter {
                    _._1 == c
                  }.headOption.map {
                    _._1
                  }
                  onclickExtra()
                })(c.name)
              }
            } else scalatags.JsDom.tags.li("To many results, filter more !")
          )
        }
      )
    ).render
    bg
  }

  lazy val selectorWithFilter = {
    // hasFilter() = if(contents().size > 9) true else false
    hasFilter() = true
    selector
  }
}