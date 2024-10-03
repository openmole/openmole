package org.openmole.gui.client.core.git

import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveElement.isActive
import org.openmole.gui.client.core.CoreUtils.*
import org.openmole.gui.client.core.Panels
import org.openmole.gui.client.ext.*
import org.openmole.gui.shared.api.*
import org.openmole.gui.shared.data.*
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
        api.cloneRepository(urlInput.ref.value, sp).foreach: c=>
          panels.treeNodePanel.refresh
          panels.closeExpandable
      }
    )

    div(flexColumn,
      urlInput,
      cloneButton
    )
    
    