package org.openmole.gui.client.core

import org.openmole.gui.ext.data.{FileType, Resources, SafePath, WizardPluginFactory}
import com.raquo.laminar.api.L._
import org.openmole.gui.client.core.panels.{fileDisplayer, treeNodeManager}
import org.openmole.gui.client.tool.TagBadge
import autowire._

import scala.concurrent.ExecutionContext.Implicits.global
import boopickle.Default._
import scaladget.bootstrapnative.bsn._
import org.openmole.gui.ext.data._
import Waiter._
import org.openmole.gui.ext.api.Api
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

  def render(wizards: Seq[WizardPluginFactory]) = {

    // 1- Empty project
    def emptyProject = {
      val fileName = "newProject.oms"
      CoreUtils.addFile(panels.treeNodeManager.dirNodeLine.now, fileName, () ⇒ {
        val toDisplay = panels.treeNodeManager.dirNodeLine.now ++ fileName
        FileManager.download(
          toDisplay,
          hash = true,
          onLoaded = (content, hash) ⇒ {
            treeNodeManager.invalidCurrentCache
            fileDisplayer.display(toDisplay, content, hash.get, FileExtension.OMS, panels.pluginServices)
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
          panels.closeExpandable
        }),
        div(display.flex, justifyContent.spaceAround, width := "600px",
          buttonStyle("Your model", 1),
          buttonStyle("The market Place", 2),
          buttonStyle("A url project", 3)
        ),
        div(cls := "close-button bi-chevron-down", onClick --> { _ ⇒ panels.closeExpandable })
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
