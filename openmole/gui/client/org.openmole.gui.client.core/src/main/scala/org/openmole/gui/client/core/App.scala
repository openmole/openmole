package org.openmole.gui.client.core

import scala.scalajs.js.annotation.*
import org.scalajs.dom
import scaladget.bootstrapnative.bsn.*
import scaladget.tools.*
import org.scalajs.dom.{KeyboardEvent, document}

import scala.concurrent.ExecutionContext.Implicits.global
import scaladget.bootstrapnative.Selector.Options
import org.openmole.gui.client.core.files.{FileDisplayer, TabContent, TreeNodeManager, TreeNodePanel, TreeNodeTabs}
import org.openmole.gui.client.tool.OMTags
import org.openmole.gui.shared.data.*
import org.openmole.gui.client.ext.*
import com.raquo.laminar.api.L.*
import org.openmole.gui.client.core.alert.{AlertPanel, BannerAlert}
import org.openmole.gui.client.ext.FileManager
import org.openmole.gui.shared.api.{BasePath, GUIPlugins, PluginServices, ServerAPI}
import scaladget.bootstrapnative.bsn.*

import scala.concurrent.Await
import scala.concurrent.duration.*
import scala.scalajs.js.timers.*

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

class OpenMOLEGUI(using panels: Panels, pluginServices: PluginServices, api: ServerAPI):

  def connection() =
    render(
      dom.document.body,
      div(
        panels.connection.render,
        panels.alertPanel.alertDiv
      )
    )

  def stopped(): Unit =
    given BasePath = BasePath(dom.document.location)
    val stoppedDiv = div(
      omsheet.connectionTabOverlay,
      div(
        div(
          omsheet.centerPage(),
          div(omsheet.shutdown, "The OpenMOLE server has been stopped")
        )
      )
    )

    api.shutdown()
    render(dom.document.body, stoppedDiv)


  def restarted(): Unit =
    given BasePath = BasePath(dom.document.location)
    val restartedDiv = div(
      omsheet.connectionTabOverlay,
      div(
        omsheet.centerPage(),
        div(omsheet.shutdown, "The OpenMOLE server is restarting, please wait.")
      )
    )

    api.restart()

    def checkAlive(): Unit =
      api.isAlive().foreach { x ⇒
        if x
        then CoreUtils.setRoute(s"/${connectionRoute}")
        else setTimeout(5000) { checkAlive() }
      }

    setTimeout(5000) { checkAlive() }

    render(dom.document.body, restartedDiv)


  def resetPassword(): Unit =
    val resetPassword = new ResetPassword
    render(
      dom.document.body,
      div(
        resetPassword.resetPassDiv,
        panels.alertPanel.alertDiv
      )
    )

  def run() =
    given BasePath = BasePath(dom.document.location)

    val containerNode = dom.document.querySelector("#openmole-content")
    //import scala.concurrent.ExecutionContext.Implicits.global
    api.fetchGUIPlugins { plugins ⇒
      given GUIPlugins = plugins
      val maindiv = div()

      val authenticationPanel = AuthenticationPanel.render
      val newProjectPanel = ProjectPanel.render

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
        if k.keyCode == 83 && k.ctrlKey
        then
          k.preventDefault()
          panels.tabContent.tabsUI.tabs.now().foreach { t => panels.tabContent.save(t.t) }
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



//      def toggleMenu(m: MainMenu) =
//        currentSelectedMenu.update(cmm=>
//          if cmm == Some(m)
//          then None
//          else Some(m)
//        )

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
          button(btn_danger, "New project",
            cls.toggle("mainMenuCurrentGlyph") <-- panels.expandablePanel.signal.map{_.map{_.id} == Some(3)},
            onClick --> { _ =>
              Panels.expandTo(newProjectPanel, 3)
            }),
          div(OMTags.glyph_flash, navBarItem, marginLeft := "40px",
            cls.toggle("mainMenuCurrentGlyph") <-- panels.expandablePanel.signal.map{_.map{_.id} == Some(4)},
            onClick --> { _ ⇒
            ExecutionPanel.open
          }).tooltip("Executions"),
          div(glyph_lock, navBarItem,
            cls.toggle("mainMenuCurrentGlyph") <-- panels.expandablePanel.signal.map{_.map{_.id} == Some(2)},
            onClick --> { _ ⇒
            Panels.expandTo(authenticationPanel, 2)
          }).tooltip("Authentications"),
          div(OMTags.glyph_plug, navBarItem,
            cls.toggle("mainMenuCurrentGlyph") <-- panels.expandablePanel.signal.map{_.map{_.id} == Some(1)},
            onClick --> { _ ⇒
            panels.pluginPanel.getPlugins
            Panels.expandTo(panels.pluginPanel.render, 1)
          }).tooltip("Plugins"),
          div(child <-- panels.expandablePanel.signal.map (_.map(ep=> Panels.ExpandablePanel.toString(ep.id)).getOrElse("")), cls := "mainMenuCurrentName")
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
          EventStream.periodic(10000).toObservable --> Observer { _ => panels.tabContent.tabsUI.tabs.now().foreach { t => panels.tabContent.save(t.t) } },
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
              panels.treeNodePanel.fileControler,
              panels.treeNodePanel.fileToolBar.sortingGroup,
              panels.treeNodePanel.treeView
            ),
            div(
              cls := "tab-section",
              theNavBar,
              //openAuthentication.signal.expand(authenticationPanel),
              // treeNodeTabs.render.amend(cls := "tab-section")
              panels.tabContent.render //.amend(cls := "tab-section")
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
            div(cls <-- panels.expandablePanel.signal.map { x =>
              "collapse-bottom " + {
                x match {
                  case Some(ep: Panels.ExpandablePanel) => ""
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
    panels.treeNodePanel.treeNodeManager.invalidCurrentCache
//}


@JSExportTopLevel(name = "openmole_library")
@JSExportAll
object App:
  lazy val fetch = Fetch(panels.bannerAlert.register)
  lazy val api = OpenMOLERESTServerAPI(fetch)

  lazy val pluginServices =
    PluginServices(
      errorManager = (message, stack) => panels.bannerAlert.registerWithStack(message, stack)
    )

  lazy val panels = Panels()

  val gui = OpenMOLEGUI(using panels, pluginServices, api)

  export gui.*

