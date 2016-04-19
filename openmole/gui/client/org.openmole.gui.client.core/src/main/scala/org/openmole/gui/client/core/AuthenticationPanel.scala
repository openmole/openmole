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

import org.openmole.gui.client
import org.openmole.gui.ext.dataui.{ PanelWithID, PanelUI }
import org.openmole.gui.shared.Api
import scalatags.JsDom.all._
import org.openmole.gui.misc.js.Select
import fr.iscpif.scaladget.api.{ BootstrapTags ⇒ bs }
import scalatags.JsDom.{ tags ⇒ tags }
import org.openmole.gui.misc.js.JsRxTags._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import org.openmole.gui.misc.utils.{ stylesheet ⇒ omsheet, Utils }
import fr.iscpif.scaladget.stylesheet.{ all ⇒ sheet }
import autowire._
import org.openmole.gui.ext.data._
import sheet._
import rx._
import org.openmole.gui.client.core.authentications._

class AuthenticationPanel extends ModalPanel {
  lazy val modalID = "authenticationsPanelID"
  lazy val setting: Var[Option[PanelUI]] = Var(None)
  private lazy val auths: Var[Option[Seq[AuthPanelWithID]]] = Var(None)

  def onOpen() = {
    getAuthentications
  }

  def onClose() = {
    setting() = None
  }

  def getAuthentications = {
    OMPost[Api].authentications.call().foreach { auth ⇒
      auths() = Some(auth.map { a ⇒ client.core.authentications.panelWithID(a) })
    }
  }

  lazy val authenticationSelector: Select[AuthPanelWithID] = {
    val fs = authentications.factories
    Select(
      //Utils.getUUID,
      fs.map { f ⇒ (f, emptyMod) },
      fs.headOption,
      btn_primary, onclickExtra = () ⇒ newPanel
    )
  }

  def newPanel = authenticationSelector.content().foreach { f ⇒ setting() = Some(f.emptyClone.panel) }

  lazy val authenticationTable = {

    case class Reactive(pwID: AuthPanelWithID) {
      val lineHovered: Var[Boolean] = Var(false)

      lazy val render = {
        div(omsheet.docEntry)(
          onmouseover := { () ⇒
            lineHovered() = true
          },
          onmouseout := { () ⇒
            lineHovered() = false
          }
        )(
            div(colMD(7))(
              tags.a(pwID.data.synthetic, omsheet.docTitleEntry +++ floatLeft +++ omsheet.colorBold("white"), cursor := "pointer", onclick := { () ⇒
                authenticationSelector.content() = Some(pwID.emptyClone)
                setting() = Some(pwID.panel)
              })
            ),
            div(colMD(4) +++ sheet.paddingTop(5))(label(pwID.name, label_primary +++ omsheet.tableTag)),
            span(
              Rx {
                if (lineHovered()) opaque
                else transparent
              },
              bs.glyphSpan(glyph_trash, () ⇒ removeAuthentication(pwID.data))(omsheet.grey +++ sheet.paddingTop(9) +++ "glyphitem" +++ glyph_trash)
            )
          )
      }
    }

    Rx {
      tags.div(
        setting() match {
          case Some(p: PanelUI) ⇒ tags.div(
            authenticationSelector.selector,
            div(sheet.paddingTop(20))(p.view)
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

  val newButton = bs.button("New", btn_primary, () ⇒ newPanel)

  val saveButton = bs.button("Save", btn_primary, () ⇒ {
    save
  })

  lazy val dialog = bs.modalDialog(
    modalID,
    bs.headerDialog(tags.b("Authentications")),
    bs.bodyDialog(authenticationTable),
    bs.footerDialog(Rx {
      bs.buttonGroup()(
        setting() match {
          case Some(_) ⇒ saveButton
          case _       ⇒ newButton
        },
        closeButton
      )
    })
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