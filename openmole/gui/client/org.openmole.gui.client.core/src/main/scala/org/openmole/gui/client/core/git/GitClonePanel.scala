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
import org.openmole.gui.client.core.Waiter
import org.openmole.gui.client.core.ProjectPanel


object GitClonePanel:

  def render(using api: ServerAPI, basePath: BasePath, panels: Panels): Div =
    
    val manager = panels.treeNodePanel.treeNodeManager
    lazy val urlInput = inputTag().amend(placeholder := "Git repository URL", width := "400px", marginTop := "20")
    val processing: Var[Boolean] = Var(false)

    def finalize =
      processing.set(false)
      panels.treeNodePanel.refresh
      panels.closeExpandable

    val cloneButton = 
    div(
      child <-- processing.signal.map: p=> 
        p match
          case true=> Waiter.waiter("#794985")
          case false=>       
            button(
            "Clone",
            cls := "btn btn-purple",
            height := "38", width := "150", marginTop := "20",
            onClick --> { _ =>
              val clonePanel = panels.expandablePanel.now()
              processing.set(true)
              val sp = manager.directory.now()
              api.cloneRepository(urlInput.ref.value, sp, false).andThen:
                case util.Success(None) => finalize
                case util.Success(Some(exist)) =>
                  processing.set(false)
                  panels.closeExpandable
                  panels.treeNodePanel.confirmationDiv.set:
                    Some:
                      panels.treeNodePanel.confirmation(s"A file named \"${exist.name}\" exists, overwrite it?", "Overwrite", () =>
                        panels.treeNodePanel.confirmationDiv.set(None)
                        panels.expandablePanel.set(clonePanel)
                        processing.set(true)
                        api.cloneRepository(urlInput.ref.value, sp, true).andThen: _ =>
                          finalize
                      )
                case _ =>
            }
          )
        )

    div(flexColumn,
      urlInput,
      cloneButton
    )
    
    