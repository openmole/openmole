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

import scalatags.JsDom.all._
import scaladget.bootstrapnative.bsn._
import scaladget.tools._

import scalatags.JsDom.tags
import org.openmole.gui.ext.tool.client._

import scala.concurrent.Future
import boopickle.Default._
import scala.concurrent.ExecutionContext.Implicits.global
import org.openmole.gui.ext.data._

import rx._
import scaladget.bootstrapnative.Selector.{ Dropdown, Options }

class AuthenticationPanel {

  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

  case class TestedAuthentication(auth: AuthenticationPlugin, tests: Future[Seq[Test]])

  val authSetting: Var[Option[AuthenticationPlugin]] = Var(None)
  private lazy val auths: Var[Seq[TestedAuthentication]] = Var(Seq())
  lazy val initialCheck = Var(false)

  def getAuthSelector(currentFactory: AuthenticationPluginFactory) = {
    lazy val authenticationSelector: Options[AuthenticationPluginFactory] = {
      val factories = Plugins.authenticationFactories.now
      val currentInd = {
        val ind = factories.map {
          _.name
        }.indexOf(currentFactory.name)
        if (ind == -1) 0 else ind
      }

      factories.options(currentInd, btn_primary, (a: AuthenticationPluginFactory) ⇒ a.name, onclose = () ⇒
        authSetting() = authenticationSelector.content.now.map {
          _.buildEmpty
        })
    }
    authenticationSelector
  }

  def getAuthentications =
    Plugins.authenticationFactories.now.map { factory ⇒
      val data = factory.getData
      auths() = Seq()
      data.foreach { d ⇒
        auths() = (auths.now ++ d.map {
          factory.build
        }.map { auth ⇒ TestedAuthentication(auth, auth.test) })
      }
      initialCheck() = true
    }

  lazy val authenticationTable = {

    case class Reactive(testedAuth: TestedAuthentication) {

      val errorOn = Var(false)
      val currentStack: Var[String] = Var("")

      def toLabel(test: Test) = {
        val lab = label(
          test.message,
          scalatags.JsDom.all.marginLeft := 10
        )
        test match {
          case PassedTest(_) ⇒ lab(label_success).render
          case PendingTest() ⇒ lab(label_warning).render
          case _ ⇒ lab(label_danger +++ pointer)(onclick := { () ⇒
            currentStack() = test.errorStack.stackTrace
            errorOn() = !errorOn.now
          }).render
        }
      }

      lazy val render = {
        tr(omsheet.docEntry +++ (lineHeight := "35px"))(
          td(colMD(4))(
            tags.a(testedAuth.auth.data.name, omsheet.docTitleEntry +++ floatLeft +++ omsheet.color(omsheet.WHITE), cursor := "pointer", onclick := { () ⇒
              authSetting() = Some(testedAuth.auth)
            })
          ),
          td(colMD(4) +++ (paddingTop := 5))(label(testedAuth.auth.factory.name, label_primary)),
          td(colMD(2))({
            val tests: Var[Seq[Test]] = Var(Seq(Test.pending))
            testedAuth.tests.foreach { ts ⇒
              tests() = ts
            }
            Rx {
              tests().map {
                toLabel
              }
            }
          }),
          td(colMD(2))(
            glyphSpan(glyph_trash, () ⇒ removeAuthentication(testedAuth.auth))(omsheet.grey +++ (paddingTop := 9) +++ "glyphitem" +++ glyph_trash)
          )
        )
      }
    }

    Rx {
      authSetting() match {
        case Some(p: AuthenticationPlugin) ⇒ div(paddingTop := 20)(p.panel)
        case _ ⇒
          tags.table(fixedTable)(
            thead,
            for (a ← auths()) yield {
              val r = Reactive(a)
              Seq(
                r.render,
                tr(
                  td(colMD(12))(
                    colspan := 12,
                    tags.div(Rx {
                      if (r.errorOn()) {
                        tags.textarea(dropdownError)(r.currentStack())
                      }
                      else tags.div()
                    })
                  )
                )
              )
            }
          )
      }
    }
  }

  val newButton = button("New", btn_primary, onclick := { () ⇒
    authSetting() = Plugins.authenticationFactories.now.headOption.map {
      _.buildEmpty
    }
  })

  val saveButton = button("Save", btn_primary, onclick := { () ⇒
    {
      save
    }
  })

  val cancelButton = button("Cancel", btn_default, onclick := { () ⇒
    {
      authSetting.now match {
        case None ⇒ dialog.hide
        case _    ⇒ authSetting() = None
      }
    }
  })

  val dialog: ModalDialog =
    ModalDialog(
      omsheet.panelWidth(52),
      onopen = () ⇒ {
        if (!initialCheck.now) {
          getAuthentications
        }
      },
      onclose = () ⇒ {
        authSetting() = None
      }
    )

  dialog.header(
    div(
      Rx {
        div(
          authSetting() match {
            case Some(o) ⇒ getAuthSelector(o.factory).selector
            case _ ⇒ div(
              b("Authentications")
            )
          }
        )
      }
    )
  )

  dialog body (div(authenticationTable))

  dialog.footer(
    tags.div(
      Rx {
        buttonGroup()(
          authSetting() match {
            case Some(_) ⇒ saveButton
            case _       ⇒ newButton
          },
          cancelButton
        )
      }
    )
  )

  def removeAuthentication(ad: AuthenticationPlugin) = {
    ad.remove(() ⇒ getAuthentications)
  }

  def save = {
    authSetting.now.map {
      _.save(() ⇒ {
        getAuthentications
      })
    }
    authSetting() = None
  }

}