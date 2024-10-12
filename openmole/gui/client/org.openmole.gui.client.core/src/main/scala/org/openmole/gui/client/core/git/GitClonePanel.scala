package org.openmole.gui.client.core.git

import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveElement.isActive
import org.openmole.gui.client.core.CoreUtils.*
import org.openmole.gui.client.core.Panels
import org.openmole.gui.client.ext.*
import org.openmole.gui.shared.api.*
import org.openmole.gui.shared.data.{ProcessState, *}
import scaladget.bootstrapnative.bsn.*
import scaladget.tools.*

import scala.concurrent.ExecutionContext.Implicits.global


object GitClonePanel:

  def render(using api: ServerAPI, basePath: BasePath, panels: Panels) =
    
    val manager = panels.treeNodePanel.treeNodeManager
    lazy val urlInput = inputTag().amend(placeholder := "Git repository URL", width := "400px", marginTop := "20")
    
    val cloneButton = button(
      "Clone",
      cls := "btn btn-purple",
      height := "38", width := "150", marginTop := "20",
      onClick --> { _ ⇒
        val sp = manager.directory.now()
        //panels.treeNodePanel.transferring.set(Processing())
        panels.closeExpandable
        api.cloneRepository(urlInput.ref.value, sp, false).andThen:
          case util.Success(None) =>
            //panels.treeNodePanel.transferring.set(Processed())
            panels.treeNodePanel.refresh
          case util.Success(Some(exist)) =>
            panels.treeNodePanel.confirmationDiv.set:
              Some:
                panels.treeNodePanel.confirmation(s"A file named \"${exist.name}\" exists, overwrite it?", "Overwrite", () ⇒
                  panels.treeNodePanel.confirmationDiv.set(None)
                  api.cloneRepository(urlInput.ref.value, sp, true).andThen: _ =>
                    //panels.treeNodePanel.transferring.set(Processed())
                    panels.treeNodePanel.refresh
                )
          case _ =>
            //panels.treeNodePanel.transferring.set(Processed())
      }
    )

    div(flexColumn,
      urlInput,
      cloneButton
    )
    
    