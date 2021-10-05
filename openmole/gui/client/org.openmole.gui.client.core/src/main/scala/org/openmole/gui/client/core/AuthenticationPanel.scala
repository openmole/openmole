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

import scaladget.bootstrapnative.bsn._
import scaladget.tools._

import org.openmole.gui.ext.client._

import scala.concurrent.Future
import boopickle.Default._
import scala.concurrent.ExecutionContext.Implicits.global
import org.openmole.gui.ext.data._

import com.raquo.laminar.api.L._
import scaladget.bootstrapnative.Selector.{ Options }

class AuthenticationPanel(authenticationFactories: Seq[AuthenticationPluginFactory]) {

  case class TestedAuthentication(auth: AuthenticationPlugin, tests: Future[Seq[Test]])

  val authSetting: Var[Option[AuthenticationPlugin]] = Var(None)
  private lazy val auths: Var[Seq[TestedAuthentication]] = Var(Seq())
  lazy val initialCheck = Var(false)

  def getAuthSelector(currentFactory: AuthenticationPluginFactory) = {
    lazy val authenticationSelector: Options[AuthenticationPluginFactory] = {
      val factories = authenticationFactories

      val currentInd = {
        val ind = factories.map {
          _.name
        }.indexOf(currentFactory.name)
        if (ind == -1) 0 else ind
      }

      factories.options(currentInd, btn_primary, (a: AuthenticationPluginFactory) ⇒ a.name, onclose = () ⇒
        authSetting.set(authenticationSelector.content.now.map {
          _.buildEmpty
        }))
    }
    authenticationSelector
  }

  def getAuthentications =
    authenticationFactories.map { factory ⇒
      val data = factory.getData
      auths.set(Seq())
      data.foreach { d ⇒
        auths.update(as ⇒ as ++ d.map {
          factory.build
        }.map { auth ⇒ TestedAuthentication(auth, auth.test) })
      }
      initialCheck.set(true)
    }

  lazy val authenticationTable = {

    case class Reactive(testedAuth: TestedAuthentication) {

      val errorOn = Var(false)
      val currentStack: Var[String] = Var("")

      def toLabel(test: Test) = {
        val lab = span(
          test.message,
          marginLeft := "10"
        )

        test match {
          case PassedTest(_) ⇒ lab.amend(badge_success)
          case PendingTest() ⇒ lab.amend(badge_warning)
          case _ ⇒ lab.amend(
            badge_danger, cursor.pointer,
            onClick --> { _ ⇒
              currentStack.set(test.error.map(ErrorData.stackTrace).getOrElse(""))
              errorOn.update(!_)
            })
        }
      }

      val tests: Var[Seq[Test]] = Var(Seq(Test.pending))
      testedAuth.tests.foreach { ts ⇒
        tests.set(ts)
      }

      lazy val render = {
        tr(omsheet.docEntry, lineHeight := "35",
          td(
            colSM(4),
            a(testedAuth.auth.data.name, omsheet.docTitleEntry, float.left, color.white, cursor.pointer, onClick --> { _ ⇒
              authSetting.set(Some(testedAuth.auth))
            })
          ),
          td(colBS(4), paddingTop := "5", span(testedAuth.auth.factory.name, badge_primary)),
          td(
            colBS(2),
            children <-- tests.signal.map { ts ⇒
              ts.map {
                toLabel
              }
            }
          ),
          td(
            colSM(2),
            glyphSpan(glyph_trash).amend(omsheet.grey, paddingTop := "9", "glyphitem", onClick --> { _ ⇒ removeAuthentication(testedAuth.auth) }))
        )
      }
    }

    div(
      child <-- authSetting.signal.map {
        _ match {
          case Some(p: AuthenticationPlugin) ⇒ div(paddingTop := "20", p.panel)
          case _ ⇒
            table(
              fixedTable,
              children <-- auths.signal.map { as ⇒
                as.flatMap { a ⇒
                  //for (a ← auths()) yield {
                  val r = Reactive(a)
                  Seq(
                    r.render,
                    tr(
                      td(
                        colBS(12),
                        colSpan := 12,
                        child <-- r.errorOn.signal.combineWith(r.currentStack).map {
                          case (err, curStack) ⇒
                            if (err) textArea(dropdownError, curStack)
                            else div()
                        }
                      //
                      //                        div(Rx {
                      //                          if (r.errorOn()) {
                      //                            textarea(dropdownError)(r.currentStack())
                      //                          }
                      //                          else div()
                      //                        })
                      )
                    )
                  )
                }
              }
            )
        }
      }
    )
  }

  val newButton = button(
    "New",
    btn_primary,
    onClick --> {
      _ ⇒
        authSetting.set(authenticationFactories.headOption.map {
          _.buildEmpty
        })
    })

  val saveButton = button("Save", btn_primary, onClick --> {
    _ ⇒ (save)
  })

  val cancelButton = button("Cancel", btn_secondary, onClick --> {
    _ ⇒
      {
        authSetting.update {
          as ⇒
            as match {
              case None ⇒ authenticationDialog.hide
              case _    ⇒
            }
            None
        }
      }
  })

  val dialogHeader = div(
    child <-- authSetting.signal.map {
      _ match {
        case Some(o) ⇒ getAuthSelector(o.factory).selector
        case _ ⇒ div(
          b("Authentications")
        )
      }
    }
  )

  val dialogBody = authenticationTable

  val dialogFooter = buttonGroup.amend(
    div(
      child <--
        authSetting.signal.map {
          _ match {
            case Some(_) ⇒ div(saveButton, cancelButton)
            case _       ⇒ div(newButton, cancelButton)
          }
        }
    )
  )

  val authenticationDialog: ModalDialog = ModalDialog(
    dialogHeader,
    dialogBody,
    dialogFooter,
    omsheet.panelWidth(52),
    onopen = () ⇒ {
      if (!initialCheck.now) {
        getAuthentications
      }
    },
    onclose = () ⇒ {
      authSetting.set(None)
    }
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
    authSetting.set(None)
  }

}