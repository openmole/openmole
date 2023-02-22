package org.openmole.gui.client.core

import org.openmole.gui.shared.data.{FileType, Resources, SafePath}
import com.raquo.laminar.api.L.*
import org.openmole.gui.client.tool.TagBadge
import org.openmole.gui.client.tool.OMTags.*

import scala.concurrent.ExecutionContext.Implicits.global
import scaladget.bootstrapnative.bsn.*
import org.openmole.gui.shared.data.*
import Waiter.*
import org.openmole.gui.client.core.Panels.closeExpandable
import org.openmole.gui.client.ext.*
import org.openmole.gui.shared.api.*

object ProjectPanel {

  val currentOption: Var[Option[Int]] = Var(None)

  def buttonStyle(text: String, id: Int) = {
    button(text, display.flex, alignItems.center,
      cls <-- currentOption.signal.map { co =>
        if (co == Some(id)) "btn btn-openmole"
        else "btn newButton"
      },
      onClick --> { _ => currentOption.set(Some(id)) }
    )
  }

  def render(using api: ServerAPI, basePath: BasePath, panels: Panels, plugins: GUIPlugins) = {

    // 1- Empty project
    def emptyProject = {
      val fileName = "newProject.oms"
      CoreUtils.createFile(panels.treeNodePanel.treeNodeManager.dirNodeLine.now(), fileName, onCreated = () ⇒ {
        val toDisplay = panels.treeNodePanel.treeNodeManager.dirNodeLine.now() ++ fileName
        api.download(
          toDisplay,
          hash = true,
          onLoadEnd = (content, hash) ⇒ {
            panels.treeNodePanel.treeNodeManager.invalidCurrentCache
            panels.fileDisplayer.display(toDisplay, content, hash.get, FileExtension.OMS)
          }
        )
        Panels.closeExpandable
      })
    }
    val emptyProjectButton = button(btn_purple, "Build", onClick --> {_=> emptyProject})

    // 2- Model wizard
    val wizardPanel = ModelWizardPanel.render
    
    // 3- Market place
    val marketPanel = MarketPanel.render

    lazy val theTabs = Tabs.tabs(tabStyle = navbar_pills, isClosable = false)
      .add(Tab("empty", span("Empty project"), emptyProjectButton))
      .add(Tab("wizard", span("From your sources"), wizardPanel))
      .add(Tab("market", span("From the market place"), marketPanel))

    
    div(margin := "20px", flexRow, alignItems.flexStart,
      div(cls := "close-button bi-x", backgroundColor := "#bdadc4", borderRadius := "20px", onClick --> { _ ⇒ Panels.closeExpandable }),
      theTabs.build.render.amend(marginLeft := "40", width := "100%")
//      div(
//        div("Start from", padding := "10px"),
//        button("Empty project", btn_primary, onClick --> { _ =>
//          emptyProject
//          Panels.closeExpandable
//        }),
//        div(display.flex, justifyContent.spaceAround, width := "600px",
//          buttonStyle("Your model", 1),
//          buttonStyle("The market Place", 2),
//          buttonStyle("A url project", 3)
//        ),
//        div(cls := "close-button bi-chevron-down", onClick --> { _ ⇒ Panels.closeExpandable })
//      ),
//      div(
//        child <-- currentOption.signal.map {
//          _ match {
//            case Some(1) => wizardPanel
//            case Some(2) => div("marke p")
//            case Some(3) => div("from u")
//            case _=> emptyNode
//          }
//        }
//      )
    )
  }

}
