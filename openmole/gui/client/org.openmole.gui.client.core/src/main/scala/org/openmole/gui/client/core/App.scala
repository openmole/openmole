package org.openmole.gui.client.core

import org.openmole.gui.client.core.panels._

import scala.scalajs.js.annotation._
import org.scalajs.dom
import scaladget.bootstrapnative.bsn._
import scaladget.tools._
import org.scalajs.dom.KeyboardEvent

import scala.concurrent.ExecutionContext.Implicits.global
import scaladget.bootstrapnative.Selector.Options
import org.openmole.gui.client.core.files.{TabContent, TreeNodePanel}
import org.openmole.gui.client.tool.OMTags
import org.openmole.gui.ext.api.Api
import org.openmole.gui.ext.data._
import org.openmole.gui.ext.client.FileManager
import org.openmole.gui.ext.client._
import com.raquo.laminar.api.L._
import scaladget.bootstrapnative.bsn._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.scalajs.js.timers._

/*
 * Copyright (C) 15/04/15 // mathieu.leclaire@openmole.org
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

@JSExportTopLevel(name = "openmole_library")
@JSExportAll
object App {

  def connection() = {
    render(
      dom.document.body,
      div(
        panels.connection.render,
        panels.alertPanel.alertDiv
      )
    )
  }

  def stopped(): Unit = {

    val stoppedDiv = div(
      omsheet.connectionTabOverlay,
      div(
        div(
          omsheet.centerPage(),
          div(omsheet.shutdown, "The OpenMOLE server has been stopped")
        )
      )
    )

    Fetch.future(_.shutdown(()).future)
    render(dom.document.body, stoppedDiv)
  }

  def restarted(): Unit = {
    val timer: Var[Option[SetIntervalHandle]] = Var(None)

    def setTimer = {
      timer.set(Some(setInterval(5000) {
        Fetch.future(_.isAlive(()).future, 3 seconds, 5 minutes).foreach { x ⇒
          if (x) {
            CoreUtils.setRoute(routes.slashConnectionRoute)
            timer.now().foreach {
              clearInterval
            }
          }
        }
      })
      )
    }

    setTimer
    val restartedDiv = div(
      omsheet.connectionTabOverlay,
      div(
        omsheet.centerPage(),
        div(omsheet.shutdown, "The OpenMOLE server is restarting, please wait.")
      )
    )

    Fetch.future(_.restart(()).future)
    render(dom.document.body, restartedDiv)
  }

  def resetPassword(): Unit = {
    val resetPassword = new ResetPassword
    render(
      dom.document.body,
      div(
        resetPassword.resetPassDiv,
        panels.alertPanel.alertDiv
      )
    )
  }

  def run() = {
    val containerNode = dom.document.querySelector("#openmole-content")

    //import scala.concurrent.ExecutionContext.Implicits.global
    Plugins.fetch { plugins ⇒
      val maindiv = div()

      val authenticationPanel = AuthenticationPanel.render(plugins.authenticationFactories)
      val newProjectPanel = ProjectPanel.render(plugins.wizardFactories)

      val openFileTree = Var(true)
      //   val openAuthentication = Var(false)

      //      val settingsItem = navItem(div(
      //        //panels.settingsViewApp,
      //        itemStyle), () ⇒ {}).right
      //
      //      val actionItem = navItem(div(
      //        child <-- treeNodeTabs.temporaryControl.signal
      //      ))

      dom.window.onkeydown = (k: KeyboardEvent) ⇒ {
        if (k.keyCode == 83 && k.ctrlKey) {
          k.preventDefault()
          false
        }
      }

      //      case class MenuAction(name: String, action: () ⇒ Unit)
      //
      //      lazy val newEmpty = MenuAction("Empty project", () ⇒ {
      //        val fileName = "newProject.oms"
      //        CoreUtils.addFile(panels.treeNodeManager.dirNodeLine.now, fileName, () ⇒ {
      //          val toDisplay = panels.treeNodeManager.dirNodeLine.now ++ fileName
      //          FileManager.download(
      //            toDisplay,
      //            hash = true,
      //            onLoaded = (content, hash) ⇒ {
      //              treeNodeManager.invalidCurrentCache
      //              fileDisplayer.display(toDisplay, content, hash.get, FileExtension.OMS, panels.pluginServices)
      //            }
      //          )
      //        })
      //      })

      //START BUTTON
      lazy val theNavBar = div(
        cls := "nav-container",
        child <-- openFileTree.signal.map { oft ⇒
          div(
            navBarItem,
            if (oft) div(glyph_chevron_left) else div(glyph_chevron_right),
            onClick --> { _ ⇒
              openFileTree.update(!_)
            }
          )
        },
        //   menuActions.selector,
        div(row, justifyContent.flexStart, marginLeft := "20px",
          button(btn_danger, "New project", onClick --> { _ => panels.expandTo(newProjectPanel, 3) }),
          div(OMTags.glyph_flash, navBarItem, onClick --> { _ ⇒ openExecutionPanel }).tooltip("Executions"),
          div(glyph_lock, navBarItem, onClick --> { _ ⇒ panels.expandTo(authenticationPanel, 2) }).tooltip("Authentications"),
          div(OMTags.glyph_plug, navBarItem, onClick --> { _ ⇒ panels.expandTo(panels.pluginPanel.render, 1) }).tooltip("Plugins")
        )
        //            settingsItem
      )

      //      lazy val importModel = MenuAction("Import your model", () ⇒ {
      //        panels.expandTo(modelWizardPanel(plugins.wizardFactories).render, 3)
      //      })

      //      lazy val fromURLProject = MenuAction("From URL", () ⇒ {
      //        urlImportPanel.urlDialog.show
      //      })
      //
      //      lazy val marketPlaceProject = MenuAction("From market place", () ⇒ {
      //        marketPanel.modalDialog.show
      //      })

      // lazy val elements = Seq(newEmpty, importModel, marketPlaceProject, fromURLProject)

      //      lazy val menuActions: Options[MenuAction] = elements.options(
      //        key = btn_danger,
      //        naming = (m: MenuAction) ⇒ m.name,
      //        onclose = () ⇒ menuActions.content.now.foreach {
      //          _.action()
      //        },
      //        fixedTitle = Some("New project")
      //      )

      // Define the option sequence
      //Fetch(_.omSettings(())) { sets ⇒
      render(
        containerNode,
        div(
          cls := "app-container",
          // panels.bannerAlert.banner,
          //theNavBar,
          div(
            cls := "main-container",
            div(
              cls <-- openFileTree.signal.map { oft ⇒
                "file-section" + {
                  if (oft) "" else " closed"
                }
              },
              div(img(src := "img/openmole_dark.png", height := "70px"), cls := "nav-container"),
              treeNodePanel.fileControler,
              treeNodePanel.fileToolBar.sortingGroup,
              treeNodePanel.treeView
            ),
            div(
              cls := "tab-section",
              theNavBar,
              //openAuthentication.signal.expand(authenticationPanel),
              // treeNodeTabs.render.amend(cls := "tab-section")
              TabContent.render //.amend(cls := "tab-section")
            )
            //                cls <-- openFileTree.signal.combineWith(panels.bannerAlert.isOpen).map {
            //                  case (oft, io) ⇒
            //                   // "centerpanel "
            ////                    +
            ////                      CoreUtils.ifOrNothing(oft, "reduce") +
            ////                      CoreUtils.ifOrNothing(io, " banneropen")
            //                },
            //                  div(
            //                    omsheet.textVersion,
            //                    div(
            //                      fontSize := "1em", s"${sets.version} ${sets.versionName}"),
            //                    div(fontSize := "0.8em", s"built the ${sets.buildTime}")
            //                  )
          ),
          //            openExpandablePanel.signal.expand(
          //              div(
          //                cls := "collapse-bottom",
          //                div(cls := "splitter"),
          //                child <-- panels.expandablePanel.signal.map { p ⇒
          //                  p.map {
          //                    _.element
          //                  }.getOrElse(div())
          //                }
          //              )
          //            ),

            div(
              div(cls <-- expandablePanel.signal.map { x =>
                "collapse-bottom " + {
                  x match {
                    case Some(ep: ExpandablePanel) => ""
                    case _ => "close"
                  }
                }
              },
                div(cls := "splitter"),
                child <-- panels.expandablePanel.signal.map { p ⇒
                  p.map {
                    _.element
                  }.getOrElse(div(top := "1000px", color.white))
                }
              )
            ),

          panels.alertPanel.alertDiv
        )
      )
    }
    panels.treeNodeManager.invalidCurrentCache
    //}
  }

}
