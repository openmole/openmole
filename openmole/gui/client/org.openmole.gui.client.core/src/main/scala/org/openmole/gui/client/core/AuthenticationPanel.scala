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

import org.openmole.gui.ext.dataui.{ PanelWithID, AuthenticationFactoryUI, PanelUI }
import org.openmole.gui.shared.Api
import scalatags.JsDom.all._
import org.openmole.gui.misc.js.Select
import fr.iscpif.scaladget.api.{ BootstrapTags ⇒ bs }
import scalatags.JsDom.{ tags ⇒ tags }
import org.openmole.gui.misc.js.JsRxTags._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import autowire._
import org.openmole.gui.ext.data._
import bs._
import rx._

class AuthenticationPanel(onresetpassword: () ⇒ Unit) extends ModalPanel {
  lazy val modalID = "authenticationsPanelID"
  val setting: Var[Option[PanelUI]] = Var(None)
  private val auths: Var[Option[Seq[AuthenticationData]]] = Var(None)

  def onOpen() = {
    getAuthentications
  }

  def onClose() = {
    setting() = None
  }

  def getAuthentications = {
    OMPost[Api].authentications.call().foreach { a ⇒
      auths() = Some(a)
    }
  }

  val authenticationSelector: Select[PanelWithID] =
    Select("authentications",
      authentications.all.map { f ⇒ (f, emptyCK) },
      authentications.all.headOption,
      btn_primary, onclickExtra = () ⇒ {
        authenticationSelector.content().map { f ⇒ setting() = Some(f.panel) }
      })

  lazy val authenticationTable = {

    case class Reactive(a: AuthenticationData) {
      val lineHovered: Var[Boolean] = Var(false)

      val render = Rx {
        bs.div("docEntry")(
          onmouseover := { () ⇒
            lineHovered() = true
          },
          onmouseout := { () ⇒
            lineHovered() = false
          }
        )(
            bs.div(col_md_7)(
              tags.a(a.synthetic, `class` := "left docTitleEntry whiteBold", cursor := "pointer", onclick := { () ⇒
                authenticationSelector.content() = Some(authentications.panelWithID(a))
                setting() = Some(authentications.panel(a))
              })
            ),
            bs.div(col_md_4 + " spacer5")(bs.label(a.synthetic, label_primary + " marketTag")),
            tags.span(
              id := Rx {
                "treeline" + {
                  if (lineHovered()) "-hover" else ""
                }
              },
              glyphSpan(glyph_trash, () ⇒ removeAuthentication(a))(id := "glyphtrash", `class` := "glyphitem grey spacer9")
            )
          )
      }
    }

    Rx {
      tags.div(
        setting() match {
          case Some(p: PanelUI) ⇒ tags.div(
            authenticationSelector.selector,
            bs.div(spacer20)(p.view)
          )
          case _ ⇒
            auths().map { aux ⇒
              for (a ← aux) yield {
                Seq(Reactive(a).render)
              }
            }
        }
      )
    }
  }

  val newButton = bs.glyphButton(glyph_plus, () ⇒ {
    authenticationSelector.content().map { f ⇒
      setting() = Some(f.panel)
    }
  })

  val saveButton = bs.button("Save", btn_primary + key("authSave"), () ⇒ {
    save
  })

  val dialog = modalDialog(
    modalID,
    headerDialog(Rx {
      tags.span(
        tags.b("Authentications"),
        inputGroup(navbar_right)(
          setting() match {
            case Some(_) ⇒ saveButton
            case _       ⇒ newButton
          }
        )
      )
    }),
    bodyDialog(authenticationTable),
    footerDialog(
      tags.div(
        tags.div(`class` := "left")(
          tags.a("Reset password", cursor := "pointer", onclick := { () ⇒
            close
            onresetpassword()
          })
        ),
        tags.br,
        tags.i(`class` := "left", "Caution: all your preferences will be erased!")
      ),
      closeButton
    )
  )

  def removeAuthentication(ad: AuthenticationData) = {
    OMPost[Api].removeAuthentication(ad).call().foreach { r ⇒
      ad match {
        case pk: PrivateKey ⇒ pk.privateKey.map { k ⇒
          OMPost[Api].deleteAuthenticationKey(k).call().foreach { df ⇒
            getAuthentications
          }
        }
        case _ ⇒ getAuthentications
      }
    }
  }

  def save = {
    setting().map {
      _.save(() ⇒ getAuthentications)
    }
    setting() = None
  }
}