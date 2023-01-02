package org.openmole.gui.client.core.files

import org.openmole.gui.client.core._
import org.openmole.gui.client.core.alert.AbsolutePositioning._

import scala.concurrent.ExecutionContext.Implicits.global
import scaladget.bootstrapnative.bsn._
import org.openmole.gui.client.core.staticPanels._
import org.openmole.gui.client.tool.OMTags
import org.openmole.gui.ext.client
import org.openmole.gui.ext.data._
import org.openmole.gui.ext.client._
import com.raquo.laminar.api.L._

class FileToolBox(initSafePath: SafePath, showExecution: () ⇒ Unit, treeNodeTabs: TreeNodeTabs, pluginState: PluginState, treeNodePanel: TreeNodePanel) {

  def iconAction(icon: HESetters, text: String, todo: () ⇒ Unit) =
    div(fileActionItems, icon, text, onClick --> { _ ⇒ todo() })

  def closeToolBox = treeNodePanel.currentLine.set(-1)

  def download = withSafePath { sp ⇒
    closeToolBox
    org.scalajs.dom.document.location.href = routes.downloadFile(client.Utils.toURI(sp.path))
  }

  def trash(using panels: Panels) = withSafePath { safePath ⇒
    closeToolBox
    CoreUtils.trashNodes(treeNodePanel, Seq(safePath)) {
      () ⇒
        panels.tabContent.removeTab(safePath)
        panels.tabContent.checkTabs
        staticPanels.pluginPanel.getPlugins
        treeNodeManager.invalidCurrentCache
    }
  }

  def duplicate = withSafePath { sp ⇒
    val newName = {
      val prefix = sp.path.last
      if (prefix.contains(".")) prefix.replaceFirst("[.]", "_1.")
      else prefix + "_1"
    }
    closeToolBox
    CoreUtils.duplicate(sp, newName)
  }

  def extract = withSafePath { sp ⇒
    Fetch.future(_.extract(sp).future).foreach {
      error ⇒
        error match {
          case Some(e: org.openmole.gui.ext.data.ErrorData) ⇒
            staticPanels.alertPanel.detail("An error occurred during extraction", ErrorData.stackTrace(e), transform = RelativeCenterPosition, zone = FileZone)
          case _ ⇒treeNodeManager.invalidCurrentCache
        }
    }
    closeToolBox
  }

  def execute = {
    import scala.concurrent.duration._
    withSafePath { sp ⇒
      Fetch.future(_.runScript(ScriptData(sp), true).future, timeout = 120 seconds, warningTimeout = 60 seconds).foreach { execInfo ⇒
        showExecution()
      }
      closeToolBox
    }
  }

  def toScript =
    withSafePath { sp ⇒
      closeToolBox
      Plugins.fetch { p ⇒
//FIXME
        //        val wizardPanel = panels.modelWizardPanel(p.wizardFactories)
//        wizardPanel.dialog.show
//        wizardPanel.fromSafePath(sp)
      }
    }

  def testRename(safePath: SafePath, to: String)(using panels: Panels) =
    val newSafePath = safePath.parent ++ to

    Fetch.future(_.exists(newSafePath).future).foreach {
      exists ⇒
        if exists
        then
          actionEdit.set(None)
          actionConfirmation.set(Some(confirmation(s"Overwrite ${safePath.name} ?", () ⇒ rename(safePath, to, () ⇒ closeToolBox))))
        else
          rename(safePath, to, () ⇒ closeToolBox)
          actionEdit.set(None)
          actionConfirmation.set(None)
    }

  def rename(safePath: SafePath, to: String, replacing: () ⇒ Unit)(using panels: Panels) = {
    val newNode = safePath.parent ++ to
    Fetch.future(_.move(safePath, safePath.parent ++ to).future).foreach { _ ⇒
      panels.tabContent.rename(safePath, newNode)
      treeNodeManager.invalidCurrentCache
      panels.tabContent.checkTabs
      treeNodePanel.currentSafePath.set(Some(newNode))
      replacing()
    }
  }

  def plugOrUnplug(safePath: SafePath, pluginState: PluginState)(using panels: Panels) = {
    pluginState.isPlugged match {
      case true ⇒
        CoreUtils.removePlugin(safePath).foreach { _ ⇒
          staticPanels.pluginPanel.getPlugins
          treeNodeManager.invalidCurrentCache
        }
          //        OMPost()[Api].unplug(safePath).call().foreach { _ ⇒
//          panels.pluginPanel.getPlugins
//          treeNodeManager.invalidCurrentCache
//        }
      case false ⇒
        CoreUtils.addPlugin(safePath).foreach { errors ⇒
          for e <- errors
          do staticPanels.alertPanel.detail("An error occurred while adding plugin", ErrorData.stackTrace(e), transform = RelativeCenterPosition, zone = FileZone)
          staticPanels.pluginPanel.getPlugins
          treeNodeManager.invalidCurrentCache
        }
//        OMPost()[Api].appendToPluggedIfPlugin(safePath).call().foreach {
//          _ ⇒
//            panels.pluginPanel.getPlugins
//            treeNodeManager.invalidCurrentCache
//        }
    }
  }

  def withSafePath(action: SafePath ⇒ Unit) = {
    treeNodePanel.currentSafePath.now().foreach { sp ⇒
      action(sp)
    }
  }

  def glyphItemize(icon: HESetter) = icon.appended(cls := "glyphitem popover-item")

  val actionConfirmation: Var[Option[Div]] = Var(None)
  val actionEdit: Var[Option[Div]] = Var(None)

  def editForm(sp: SafePath)(using panels: Panels): Div = {
    val renameInput = inputTag(sp.name).amend(
      placeholder := "File name",
      onMountFocus
    )

    div(
      child <-- actionConfirmation.signal.map { ac ⇒
        ac match {
          case Some(c) ⇒ c
          case None ⇒
            form(
              renameInput,
              onSubmit.preventDefault --> { _ ⇒
                withSafePath { sp ⇒
                  testRename(sp, renameInput.ref.value)
                }
              }
            )
        }
      }
    )
  }

  def confirmation(text: String, todo: () ⇒ Unit) =
    div(
      fileActions,
      div(text, width := "50%", margin := "10px"),
      div(fileItemCancel, "Cancel", onClick --> {
        _ ⇒ actionConfirmation.set(None)
      }),
      div(fileItemWarning, "OK", onClick --> {
        _ ⇒
          todo()
          actionConfirmation.set(None)
      })
    )

  def contentRoot(using panels: Panels) = {
    div(
      height := "80px",
      child <-- actionConfirmation.signal.combineWith(actionEdit.signal).map {
        a ⇒
          a match {
            case (Some(ac), _) ⇒ ac
            case (_, Some(ae)) ⇒ ae
            case (None, None) ⇒
              div(
                fileActions,
                iconAction(glyphItemize(OMTags.glyph_arrow_left_right), "duplicate", () ⇒ duplicate),
                iconAction(glyphItemize(glyph_edit), "rename", () ⇒ actionEdit.set(Some(editForm(initSafePath)))),
                iconAction(glyphItemize(glyph_download), "download", () ⇒ download),
                iconAction(glyphItemize(glyph_trash), "delete", () ⇒ actionConfirmation.set(Some(confirmation(s"Delete ${
                  initSafePath.name
                } ?", () ⇒ trash)))),
                FileExtension(initSafePath.name) match {
                  case FileExtension.TGZ | FileExtension.TAR | FileExtension.ZIP | FileExtension.TXZ ⇒
                    iconAction(glyphItemize(OMTags.glyph_extract), "extract", () ⇒ extract)
                  case _ ⇒ emptyMod
                },
                FileExtension(initSafePath.name) match {
                  case FileExtension.OMS ⇒
                    iconAction(glyphItemize(OMTags.glyph_flash), "run", () ⇒ execute)
                  case _ ⇒ emptyMod
                },
                FileExtension(initSafePath.name) match {
                  case FileExtension.JAR | FileExtension.NETLOGO | FileExtension.R | FileExtension.TGZ ⇒
                    iconAction(glyphItemize(OMTags.glyph_share), "to OMS", () ⇒ toScript)
                  case _ ⇒ emptyMod
                },
                pluginState.isPlugin match {
                  case true ⇒
                    val (icon, text) = pluginState.isPlugged match {
                      case true  ⇒ (OMTags.glyph_unpuzzle, "unplug")
                      case false ⇒ (OMTags.glyph_puzzle, "plug")
                    }
                    iconAction(glyphItemize(icon), text, () ⇒ plugOrUnplug(initSafePath, pluginState))
                  case false ⇒ emptyMod
                }
              )
          }
      }

    )
  }

}