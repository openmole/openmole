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

import scalatags.JsDom.all._
import fr.iscpif.scaladget.api.{ BootstrapTags ⇒ bs }

import scalatags.JsDom.tags
import org.openmole.gui.client.tool._
import JsRxTags._

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import fr.iscpif.scaladget.stylesheet.{ all ⇒ sheet }
import autowire._
import org.openmole.gui.ext.data._
import sheet._
import rx._
import bs._
import org.openmole.gui.client.core.authentications._
import org.openmole.gui.client.tool.OMPost
import org.openmole.gui.ext.api.Api

import scalatags.JsDom

class AuthenticationPanel extends ModalPanel {

  lazy val modalID = "authenticationsPanelID"
  lazy val setting: Var[Option[PanelUI]] = Var(None)
  private lazy val auths: Var[Option[Seq[AuthPanelWithID]]] = Var(None)
  lazy val initialCheck = Var(false)

  def onOpen() = {
    if (!initialCheck.now) {
      getAuthentications
    }
  }

  def onClose() = {
    setting() = None
  }

  def getAuthentications = {
    OMPost()[Api].authentications.call().foreach { auth ⇒
      auths() = Some(auth.map { a ⇒ client.core.authentications.panelWithID(a) })
      testAuthentications
    }
  }

  lazy val authenticationSelector = {
    val fs = authentications.factories
    fs.select(fs.headOption, (auth: AuthPanelWithID) ⇒ auth.name, btn_primary, onclickExtra = () ⇒ newPanel)
  }

  def newPanel: Unit = authenticationSelector.content.now.foreach { f ⇒ setting() = Some(f.value.emptyClone.panel) }

  lazy val authenticationTable = {

    case class Reactive(pwID: AuthPanelWithID) {
      val lineHovered: Var[Boolean] = Var(false)

      def toLabel(message: String, test: AuthenticationTest) =
        label(
          message,
          scalatags.JsDom.all.marginLeft := 10,
          test.passed match {
            case true  ⇒ label_success
            case false ⇒ label_danger +++ pointer
          },
          onclick := { () ⇒
            if (!test.passed) {
              panels.stackPanel.content() = test.errorStack.stackTrace
              panels.environmentStackTriggerer.open
            }
          }
        )

      lazy val render = {
        tr(omsheet.docEntry +++ (lineHeight := "35px"))(
          onmouseover := { () ⇒
            lineHovered() = true
          },
          onmouseout := { () ⇒
            lineHovered() = false
          }
        )(
            td(colMD(2))(
              tags.a(pwID.data.synthetic, omsheet.docTitleEntry +++ floatLeft +++ omsheet.colorBold("white"), cursor := "pointer", onclick := { () ⇒
                authenticationSelector.content() = Some(pwID.emptyClone)
                setting() = Some(pwID.panel)
              })
            ),
            td(colMD(6) +++ sheet.paddingTop(5))(label(pwID.name, label_primary)),
            td(colMD(2))(
              for {
                test ← pwID.authenticationTests
              } yield {
                test match {
                  case egi: EGIAuthenticationTest ⇒ toLabel(egi.message, egi)
                  case ssh: SSHAuthenticationTest ⇒ toLabel(ssh.message, ssh)
                  case _                          ⇒ label("pending", label_warning)
                }
              }
            ),
            td(
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
            tags.table(
              auths().map { aux ⇒
                for (a ← aux) yield {
                  Seq(Reactive(a).render)
                }
              }
            )
        }
      )
    }
  }

  val newButton = bs.button("New", btn_primary, () ⇒ newPanel)

  val saveButton = bs.button("Save", btn_primary, () ⇒ {
    save
  })

  val vosToBeTested = bs.labeledInput("Test EGI credential on", "", "VO names (vo1,vo2,...)", labelStyle = JsDom.all.color := "#000")
  OMPost()[Api].getConfigurationValue(VOTest).call().foreach {
    _.map { c ⇒
      vosToBeTested.setDefault(c)
    }
  }

  val settingsDiv = tags.div(width := 200)(
    vosToBeTested.render
  )

  val settingsButton = tags.span(
    btn_default +++ glyph_settings +++ omsheet.settingsButton
  )(tags.span(caret))

  lazy val dialog = bs.modalDialog(
    modalID,
    bs.headerDialog(
      div(height := 55)(
        b("Authentications"),
        div(omsheet.panelHeaderSettings)(
          settingsButton
        ).popup(
          settingsDiv,
          onclose = () ⇒ OMPost()[Api].setConfigurationValue(VOTest, vosToBeTested.value).call().foreach { x ⇒
          getAuthentications
        },
          popupStyle = whitePopupWithBorder
        )
      )
    ),
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
    OMPost()[Api].removeAuthentication(ad).call().foreach { r ⇒
      ad match {
        case pk: PrivateKey ⇒ pk.privateKey.map { k ⇒
          OMPost()[Api].deleteAuthenticationKey(k).call().foreach { df ⇒
            getAuthentications
          }
        }
        case _ ⇒ getAuthentications
      }
    }
  }

  def save = {
    setting.now.map {
      _.save(() ⇒ getAuthentications)
    }
    setting() = None
  }

  def testAuthentications = {
    auths.now.foreach { aux ⇒
      for (a ← aux) yield {
        val vos = {
          if (vosToBeTested.value.isEmpty) Seq()
          else vosToBeTested.value.split(",").toSeq
        }
        OMPost()[Api].testAuthentication(a.data, vos).call().foreach { t ⇒
          auths() = auths.now.map {
            _.updated(auths.now.map {
              _.indexOf(a)
            }.getOrElse(-1), a.copy(authenticationTests = t))
          }
          initialCheck() = true
        }
      }
    }
  }

}