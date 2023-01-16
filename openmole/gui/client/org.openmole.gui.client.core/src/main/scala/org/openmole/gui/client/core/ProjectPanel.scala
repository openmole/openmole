package org.openmole.gui.client.core

import org.openmole.gui.ext.data.{FileType, Resources, SafePath, WizardPluginFactory}
import com.raquo.laminar.api.L._
import org.openmole.gui.client.tool.TagBadge

import scala.concurrent.ExecutionContext.Implicits.global
import scaladget.bootstrapnative.bsn._
import org.openmole.gui.ext.data._
import Waiter._
import org.openmole.gui.ext.client.FileManager

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

  def render(wizards: Seq[WizardPluginFactory])(using api: ServerAPI, panels: Panels, fetch: Fetch, pluginServices: PluginServices) = {

    // 1- Empty project
    def emptyProject = {
      val fileName = "newProject.oms"
      CoreUtils.createFile(panels.treeNodePanel.treeNodeManager.dirNodeLine.now(), fileName, onCreated = () ⇒ {
        val toDisplay = panels.treeNodePanel.treeNodeManager.dirNodeLine.now() ++ fileName
        FileManager.download(
          toDisplay,
          hash = true,
          onLoaded = (content, hash) ⇒ {
            panels.treeNodePanel.treeNodeManager.invalidCurrentCache
            panels.fileDisplayer.display(toDisplay, content, hash.get, FileExtension.OMS, pluginServices)
          }
        )
      })
    }

    // 2- Model wizard
    val wizardPanel = ModelWizardPanel.render(wizards)

    div(
      div(
        cls := "expandable-title",
        div("Start from", padding := "10px"),
        button("Empty project", cls := "btn newButton", onClick --> { _ =>
          emptyProject
          Panels.closeExpandable
        }),
        div(display.flex, justifyContent.spaceAround, width := "600px",
          buttonStyle("Your model", 1),
          buttonStyle("The market Place", 2),
          buttonStyle("A url project", 3)
        ),
        div(cls := "close-button bi-chevron-down", onClick --> { _ ⇒ Panels.closeExpandable })
      ),
      div(
        child <-- currentOption.signal.map {
          _ match {
            case Some(1) => wizardPanel
            case Some(2) => div("marke p")
            case Some(3) => div("from u")
            case _=> emptyNode
          }
        }
      )
    )
  }

}
