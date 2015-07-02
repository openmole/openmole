package org.openmole.gui.client.core

/*
 * Copyright (C) 17/05/15 // mathieu.leclaire@openmole.org
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

import org.openmole.core.workspace.{ AuthenticationProvider, Workspace }
import org.openmole.gui.ext.dataui.{ AuthenticationFactoryUI, PanelUI }
import org.openmole.gui.misc.utils.Utils
import org.openmole.gui.shared.Api
import org.scalajs.dom.raw.HTMLDivElement
import org.scalajs.jquery
import scala.scalajs.js.Date
import scalatags.JsDom.all._
import org.openmole.gui.misc.js.{ BootstrapTags ⇒ bs, Select, Expander }
import org.openmole.gui.misc.js.Expander._
import scalatags.JsDom.{ tags ⇒ tags }
import org.openmole.gui.misc.js.JsRxTags._
import scala.scalajs.js.timers._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import autowire._
import org.openmole.gui.ext.data._
import bs._
import rx._

class AuthenticationPanel extends ModalPanel {
  val modalID = "authenticationsPanelID"
  val setting: Var[Option[PanelUI]] = Var(None)

  def onOpen = () ⇒ {
    println("open authen")
  }

  def onClose = () ⇒ {
    println("close authen")
  }

  private val auths: Var[Seq[AuthenticationData]] = Var(Seq())

  def getAuthentications = {
    OMPost[Api].authentications.call().foreach { a ⇒
      auths() = a
    }
  }

  val factories = ClientService.authenticationFactories

  val authenticationSelector: Select[AuthenticationFactoryUI] = Select("authentications",
    factories,
    factories.headOption,
    btn_primary, onclickExtra = () ⇒ {
      authenticationSelector.content().map { f ⇒ setting() = Some(f.panelUI) }
    })

  lazy val authenticationTable = {

    case class Reactive(a: AuthenticationData) {
      val lineHovered: Var[Boolean] = Var(false)

      val render = Rx {
        bs.tr(row)(
          onmouseover := { () ⇒
            lineHovered() = true
          },
          onmouseout := { () ⇒
            lineHovered() = false
          },
          tags.td(
            tags.a(a.synthetic, `class` := "left", cursor := "pointer", onclick := { () ⇒
              setting() = Some(ClientService.panelUI(a))
            })
          ),
          tags.td(bs.label(ClientService.authenticationUI(a).name, label_primary)),
          tags.td(id := Rx {
            "treeline" + {
              if (lineHovered()) "-hover" else ""
            }
          })(
            glyphSpan(glyph_trash, () ⇒ removeAuthentication(a))(id := "glyphtrash", `class` := "glyphitem grey")
          )
        )
      }
    }

    bs.table(striped)(
      thead,
      Rx {
        tbody({
          setting() match {
            case Some(p: PanelUI) ⇒ tags.div(
              authenticationSelector.selector,
              p.view
            )
            case _ ⇒
              for (a ← auths()) yield {
                //ClientService.authenticationUI(a)
                Seq(Reactive(a).render)
              }
          }
        }
        )
      }
    )
  }

  val newButton = bs.glyphButton(glyph_plus, () ⇒ {
    save
    authenticationSelector.content().map { f ⇒
      setting() = Some(f.panelUI)
    }
  })

  val saveButton = bs.button("Save", btn_primary, () ⇒ {
    save
  })

  val closeButton = bs.button("Close", btn_primary)(data("dismiss") := "modal", onclick := {
    () ⇒
      println("Close")
  }
  )

  val dialog = modalDialog(modalID,
    headerDialog(Rx {
      tags.span("Authentications",
        inputGroup(navbar_right)(
          inputGroupButton(newButton),
          inputGroupButton(setting() match {
            case Some(_) ⇒ saveButton
            case _       ⇒ tags.div()
          }
          )))
    }),
    bodyDialog(authenticationTable),
    footerDialog(
      closeButton
    )
  )

  def removeAuthentication(d: AuthenticationData) = {

  }

  def save = {
    setting().map {
      _.save(() ⇒ getAuthentications)
    }
    setting() = None
  }

  jquery.jQuery(org.scalajs.dom.document).on("hide.bs.modal", "#" + modalID, () ⇒ {
    onClose()
  })
}