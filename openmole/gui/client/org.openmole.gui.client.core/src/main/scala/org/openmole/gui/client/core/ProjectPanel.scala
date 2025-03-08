package org.openmole.gui.client.core

import com.raquo.laminar.api.L.*
import org.openmole.gui.client.tool.TagBadge
import org.openmole.gui.client.tool.OMTags.*

import scala.concurrent.ExecutionContext.Implicits.global
import scaladget.bootstrapnative.bsn.*
import org.openmole.gui.shared.data.*
import Waiter.*
import org.openmole.gui.client.core.git.GitClonePanel
import org.openmole.gui.client.ext.*
import org.openmole.gui.shared.api.*

object ProjectPanel:

  val currentOption: Var[Option[Int]] = Var(None)

  def buttonStyle(text: String, id: Int) = {
    button(text, display.flex, alignItems.center,
      cls <-- currentOption.signal.map: co =>
        if co.contains(id)
        then "btn btn-openmole"
        else "btn newButton",
      onClick --> { _ => currentOption.set(Some(id)) }
    )
  }

  def render(using api: ServerAPI, basePath: BasePath, panels: Panels, plugins: GUIPlugins) =

    // 1- Empty project
    def emptyProject =
      val fileName = "newProject.oms"
      CoreUtils.createFile(panels.treeNodePanel.treeNodeManager.directory.now(), fileName).foreach: _ =>
        val toDisplay = panels.treeNodePanel.treeNodeManager.directory.now() ++ fileName
        panels.treeNodePanel.refresh
        files.FileDisplayer.display(toDisplay)

        panels.closeExpandable


    val emptyProjectButton = button(btn_purple, marginTop:= "40", "Build", onClick --> {_=> emptyProject})

    // 2- Git repo
    val gitClonePanel = GitClonePanel.render
    
    // 3- Model wizard
    val wizardPanel = ModelWizardPanel.render
    
    // 4- Market place
    val marketPanel = MarketPanel.render

    // 5- From URL
    val urlPanel = URLImportPanel.render

    lazy val theTabs = Tabs.tabs(tabStyle = navbar_pills, isClosable = false)
      .add(Tab("empty", span("Empty project"), emptyProjectButton))
      .add(Tab("wizard", span("From your model"), wizardPanel))
      .add(Tab("git", span("From git repository"), gitClonePanel))
      .add(Tab("market", span("From examples"), marketPanel))
      .add(Tab("url", span("From URL"), urlPanel ))

    
    div(margin := "20px", flexRow, alignItems.flexStart,
      div(cls := "close-button bi-x", backgroundColor := "#bdadc4", borderRadius := "20px", onClick --> { _ => panels.closeExpandable }),
      theTabs.build.render.amend(marginLeft := "40", width := "100%")
    )

