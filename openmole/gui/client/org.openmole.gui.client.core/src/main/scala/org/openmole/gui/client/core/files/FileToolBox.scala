package org.openmole.gui.client.core.files

import org.openmole.gui.client.core._
import autowire._
import boopickle.Default._
import com.raquo.domtypes.jsdom.defs.events.TypedTargetMouseEvent
import org.openmole.gui.client.core.alert.AbsolutePositioning._
import org.openmole.gui.client.core.alert.AlertPanel

import scala.concurrent.ExecutionContext.Implicits.global
import org.openmole.gui.ext.api.Api
import scaladget.bootstrapnative.bsn._
import org.openmole.gui.client.core.panels._
import org.openmole.gui.client.tool.OMTags
import org.openmole.gui.ext.client
import org.openmole.gui.ext.data._
import org.openmole.gui.ext.client._
import org.scalajs.dom.raw._
import com.raquo.laminar.api.L._
import com.raquo.laminar.modifiers.EventListener

import scala.annotation.tailrec

object FileToolBox {
  type FileAction = String

  object fileaction {
    val trash: FileAction = "trash"
    val confirmTrash: FileAction = "co-trash"
    val cancelTrash: FileAction = "ca-trash"

    val rename: FileAction = "rename"
    val editInput: FileAction = "edit-input"
    val confirmRename: FileAction = "co-rename"
    val confirmOverwrite: FileAction = "co-overwrite"
    val cancelRename: FileAction = "ca-rename"

    val download: FileAction = "download"

    val extract: FileAction = "extract"
    val duplicate: FileAction = "duplicate"
    val execute: FileAction = "execute"
    val toScript: FileAction = "to-script"
  }

}

import FileToolBox._

class FileToolBox(initSafePath: SafePath, showExecution: () ⇒ Unit, treeNodeTabs: TreeNodeTabs) {

  import scaladget.tools._

  val baseGlyph: HESetters = Seq(cls := "glyphitem", color := DARK_GREY, padding := "5")
  // val trash: HESetters = Seq(baseGlyph, glyph_trash)
  val edit: HESetters = Seq(baseGlyph, glyph_edit)
  val archive: HESetters = Seq(baseGlyph, glyph_archive)

  def iconAction(icon: HESetters, text: String, todo: () ⇒ Unit) =
    div(fileActionItems, icon, text, onClick --> { _ ⇒ todo() })

  //val trashTrigger = iconAction(fileaction.trash, trash, "delete")
  val confirmTrashTrigger = button(btn_danger, "Delete file", idAttr := fileaction.confirmTrash)
  val cancelTrashTrigger = button(btn_secondary, "Cancel", idAttr := fileaction.cancelTrash)
  val confirmationGroup = buttonGroup.amend(confirmTrashTrigger, cancelTrashTrigger)

  val renameTrigger = span(edit, idAttr := fileaction.rename)

  def closeToolBox = treeNodePanel.currentLine.set(-1)

  def download = withSafePath { sp ⇒
    closeToolBox
    org.scalajs.dom.document.location.href = routes.downloadFile(client.Utils.toURI(sp.path))
  }

  def trash = withSafePath { safePath ⇒
    closeToolBox
    CoreUtils.trashNode(safePath) {
      () ⇒
        treeNodeTabs remove safePath
        treeNodeTabs.checkTabs
        treeNodePanel.invalidCacheAndDraw
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
    Post()[Api].extractTGZ(sp).call().foreach {
      r ⇒
        r.error match {
          case Some(e: org.openmole.gui.ext.data.ErrorData) ⇒
            panels.alertPanel.detail("An error occurred during extraction", ErrorData.stackTrace(e), transform = RelativeCenterPosition, zone = FileZone)
          case _ ⇒ treeNodePanel.invalidCacheAndDraw
        }
    }
    closeToolBox
  }

  def execute = {
    import scala.concurrent.duration._
    withSafePath { sp ⇒
      Post(timeout = 120 seconds, warningTimeout = 60 seconds)[Api].runScript(ScriptData(sp), true).call().foreach { execInfo ⇒
        showExecution()
      }
      closeToolBox
    }
  }

  def toScript =
    withSafePath { sp ⇒
      closeToolBox
      Plugins.fetch { p ⇒
        val wizardPanel = panels.modelWizardPanel(p.wizardFactories)
        wizardPanel.dialog.show
        wizardPanel.fromSafePath(sp)
      }
    }

  //  def actions(element0: HTMLElement): Boolean = {
  //    if (element0 != null) {
  //      val parent0 = element0.parentNode
  //      if (parent0 != null) {
  //        val (testID, element, parent) =
  //          if (element0.id.isEmpty) (element0.parentElement.id, parent0, parent0.parentNode)
  //          else (element0.id, element0, parent0)
  //        testID match {
  //          case fileaction.trash ⇒
  //            parent.parentNode.replaceChild(confirmationGroup.ref, parent)
  //            true
  //          case fileaction.rename ⇒
  //            withSafePath { sp ⇒
  //              editTitle.ref.value = sp.name
  //              parent.parentNode.replaceChild(editDiv(fileaction.confirmRename, "Rename").ref, parent)
  //            }
  //            true
  //          case fileaction.editInput ⇒ true
  //          case fileaction.confirmRename ⇒
  //            withSafePath { sp ⇒
  //              testRename(sp, parent, parent, element.parentNode)
  //            }
  //            true
  //          case fileaction.confirmOverwrite ⇒
  //            withSafePath { sp ⇒
  //              rename(sp, () ⇒ {})
  //              parentNode(parent, 3).replaceChild(buildTitleRoot(sp.name).ref, parentNode(parent, 2))
  //              //Popover.hide
  //            }
  //            true
  //          case fileaction.cancelRename ⇒
  //            withSafePath { sp ⇒
  //              parent.parentNode.parentNode.replaceChild(buildTitleRoot(sp.name).ref, parent.parentNode)
  //            }
  //            true
  //          case fileaction.toScript ⇒
  //            withSafePath { sp ⇒
  //              Plugins.fetch { p ⇒
  //                val wizardPanel = panels.modelWizardPanel(p.wizardFactories)
  //                wizardPanel.dialog.show
  //                wizardPanel.fromSafePath(sp)
  //                //Popover.hide
  //              }
  //            }
  //            true
  //          case _ ⇒ false
  //        }
  //      }
  //      else false
  //    }
  //    else false
  //  }

  def withSafePath(action: SafePath ⇒ Unit) = {
    println("WSP " + treeNodePanel.currentSafePath.now())
    treeNodePanel.currentSafePath.now.foreach { sp ⇒
      action(sp)
    }
  }

  val editTitle = inputTag().amend(
    placeholder := "File name",
    //  width := 200,
    onMountFocus,
    idAttr := fileaction.editInput
  )

  val overwriting = Var(false)

  //  val editForm: FormElement = form(
  //    editTitle,
  //    onSubmit --> { _ ⇒
  //      {
  //        withSafePath { sp ⇒
  //          if (overwriting.now) {
  //            rename(sp, () ⇒ {})
  //            //Popover.hide
  //          }
  //          else
  //            testRename(sp, editTitle.ref, editForm.ref, editForm.ref.parentNode.lastChild)
  //        }
  //        false
  //      }
  //    }
  //  )

  //  def replaceTitle(element: org.scalajs.dom.raw.Node) = {
  //    parentNode(element, 2).replaceChild(buildTitleRoot(editTitle.ref.value).ref, parentNode(element, 1))
  //    ()
  //  }

  //  def editDiv(tag: String, confirmString: String) = span(height := "24", width := "250",
  //    editForm,
  //    buttonGroup.amend(
  //      confirmRename(tag, confirmString),
  //      cancelRename
  //    )
  //  )

  def glyphItemize(icon: HESetter) = icon.appended(cls := "glyphitem popover-item")

  val actionConfirmation: Var[Option[Div]] = Var(None)

  def confirmation(text: String, todo: () ⇒ Unit) =
    div(
      fileActions,
      div(text, width := "50%", margin := "10px"),
      div(fileItemCancel, "Cancel", onClick --> { _ ⇒ actionConfirmation.set(None) }),
      div(fileItemWarning, "OK", onClick --> { _ ⇒
        todo()
        actionConfirmation.set(None)
      })
    )

  def contentRoot = {
    div(
      height := "80px",
      child <-- actionConfirmation.signal.map { ac ⇒
        ac match {
          case Some(c) ⇒ c
          case None ⇒
            div(
              fileActions,
              iconAction(glyphItemize(glyph_download), "download", () ⇒ download),
              iconAction(glyphItemize(glyph_trash), "trash", () ⇒ actionConfirmation.set(Some(confirmation(s"Delete ${initSafePath.name} ?", () ⇒ trash)))),
              iconAction(glyphItemize(OMTags.glyph_arrow_left_right), "duplicate", () ⇒ duplicate),
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
                  iconAction(OMTags.glyph_share, "to OMS", () ⇒ toScript)
                case _ ⇒ emptyMod
              }
            )
        }
      }
    )
  }

  //  def testRename(safePath: SafePath, parent: org.scalajs.dom.raw.Node, pivot: org.scalajs.dom.raw.Node, cancelNode: org.scalajs.dom.raw.Node) = {
  //    val newTitle = editTitle.ref.value
  //    val newSafePath = safePath.parent ++ newTitle
  //    //treeNodeTabs.saveAllTabs(() ⇒ {
  //    Post()[Api].existsExcept(newSafePath, false).call().foreach {
  //      b ⇒
  //        if (b) {
  //          overwriting.set(true)
  //          cancelNode.parentNode.replaceChild(editDiv(fileaction.confirmOverwrite, "Overwrite ?").ref, cancelNode)
  //          editTitle
  //        }
  //        else {
  //          overwriting.set(false)
  //          rename(safePath, () ⇒ replaceTitle(pivot))
  //          //Popover.hide
  //        }
  //    }
  //    //})
  //  }

  def rename(safePath: SafePath, replacing: () ⇒ Unit) = {
    val newTitle = editTitle.ref.value
    Post()[Api].renameFile(safePath, newTitle).call().foreach {
      newNode ⇒
        treeNodeTabs.rename(safePath, newNode)
        treeNodePanel.invalidCacheAndDraw
        treeNodeTabs.checkTabs
        treeNodePanel.currentSafePath.set(Some(safePath.parent ++ newTitle))
        replacing()
    }
  }

  //  import org.scalajs.dom.raw._
  //
  //  @tailrec
  //  private def parentNode(node: Node, depth: Int): Node = {
  //    if (depth == 0) node
  //    else parentNode(node.parentNode, depth - 1)
  //  }

}
