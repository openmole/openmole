package org.openmole.gui.client.core

import org.scalajs.dom.raw.HTMLDivElement

import scalatags.JsDom.TypedTag
import scalatags.JsDom.{ tags ⇒ tags }
import org.openmole.gui.misc.js.JsRxTags._
import org.openmole.gui.misc.js.{ BootstrapTags ⇒ bs, ClassKeyAggregator }
import scalatags.JsDom.all._
import bs._
import rx._

/*
 * Copyright (C) 27/08/15 // mathieu.leclaire@openmole.org
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

object GUIDoc {

  case class GUIDocEntry(glyph: ClassKeyAggregator, title: String, content: TypedTag[HTMLDivElement])

  val selectedEntry: Var[Option[GUIDocEntry]] = Var(None)

  val entries = Seq(
    GUIDocEntry(bs.glyph_file, "Manage the resources", tags.div("manage file")),
    GUIDocEntry(bs.glyph_settings, "Execute scripts", tags.div("Execute")),
    GUIDocEntry(bs.glyph_lock, "Manage authentications", tags.div("authentications")),
    GUIDocEntry(bs.glyph_market, "The Market place", tags.div("market place")),
    GUIDocEntry(bs.glyph_plug, "Plugins", tags.div("plugins"))
  )

  val doc: TypedTag[HTMLDivElement] = {
    tags.div(`class` := "docText",
      "This help provides informations to use the OpenMOLE web application. ",
      tags.div(
        "This application enables to manage OpenMOLE scripts, to edit them and to run them." +
          " It does not explain how to build a workflow by means of the ",
        tags.a(href := "http://www.openmole.org/current/Documentation_Language.html", target := "_blank")(
          "OpenMOLE language"),
        ", which is explained in detail on the OpenMOLE website."),
      for (entry ← entries) yield {
        Rx {
          val isSelected = selectedEntry() == Some(entry)
          tags.div(
            `class` := "docEntry" + { if (isSelected) " docEntrySelected" else "" },
            tags.span(
              `class` := "docTitleEntry",
              onclick := { () ⇒
                {
                  selectedEntry() = {
                    if (isSelected) None
                    else Some(entry)
                  }
                }
              },
              glyph(entry.glyph + "glyphText"),
              entry.title
            ),
            if (isSelected) tags.div(`class` := "docContent", entry.content)
            else tags.div

          )
        }
      }
    )

  }

}