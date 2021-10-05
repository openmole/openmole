package org.openmole.gui.client.core.files

import org.openmole.gui.client.core._
import autowire._
import boopickle.Default._
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
  val trash: HESetters = Seq(baseGlyph, glyph_trash)
  val edit: HESetters = Seq(baseGlyph, glyph_edit)
  val download_alt: HESetters = Seq(baseGlyph, glyph_download_alt)
  val archive: HESetters = Seq(baseGlyph, glyph_archive)
  val arrow_right_and_left: HESetters = Seq(baseGlyph, glyph_arrow_right_and_left)
  val execute: HESetters = Seq(baseGlyph, OMTags.glyph_flash)
  val toScript: HESetters = Seq(baseGlyph, OMTags.glyph_share)

  def iconAction(faction: FileAction, icon: HESetters, text: String) = div(idAttr := faction, icon, div(text, giFontFamily, fontSize := "12px", paddingTop := "5", flex := "1"))

  val trashTrigger = iconAction(fileaction.trash, trash, "delete")
  val downloadTrigger = iconAction(fileaction.download, download_alt, "download")
  val confirmTrashTrigger = button(btn_danger, "Delete file", idAttr := fileaction.confirmTrash)
  val cancelTrashTrigger = button(btn_secondary, "Cancel", idAttr := fileaction.cancelTrash)
  val confirmationGroup = buttonGroup.amend(confirmTrashTrigger, cancelTrashTrigger)

  val renameTrigger = span(edit, idAttr := fileaction.rename)

  def confirmRename(tag: String, confirmString: String) = button(btn_danger, confirmString, idAttr := tag)

  val cancelRename = button(btn_secondary, "Cancel", idAttr := fileaction.cancelRename)

  val duplicateTrigger = iconAction(fileaction.duplicate, arrow_right_and_left, "duplicate")

  def actions(element0: HTMLElement): Boolean = {
    if (element0 != null) {
      val parent0 = element0.parentNode
      if (parent0 != null) {
        val (testID, element, parent) =
          if (element0.id.isEmpty) (element0.parentElement.id, parent0, parent0.parentNode)
          else (element0.id, element0, parent0)
        testID match {
          case fileaction.trash ⇒
            parent.parentNode.replaceChild(confirmationGroup.ref, parent)
            true
          case fileaction.confirmTrash ⇒
            withSafePath { safePath ⇒
              CoreUtils.trashNode(safePath) {
                () ⇒
                  treeNodeTabs remove safePath
                  treeNodeTabs.checkTabs
                  treeNodePanel.invalidCacheAndDraw
                //Popover.hide
              }
            }
            true
          case fileaction.cancelTrash ⇒
            parent.parentNode.replaceChild(contentRoot.ref, parent)
            true
          case fileaction.rename ⇒
            withSafePath { sp ⇒
              editTitle.ref.value = sp.name
              parent.parentNode.replaceChild(editDiv(fileaction.confirmRename, "Rename").ref, parent)
            }
            true
          case fileaction.editInput ⇒ true
          case fileaction.confirmRename ⇒
            withSafePath { sp ⇒
              testRename(sp, parent, parent, element.parentNode)
            }
            true
          case fileaction.confirmOverwrite ⇒
            withSafePath { sp ⇒
              rename(sp, () ⇒ {})
              parentNode(parent, 3).replaceChild(buildTitleRoot(sp.name).ref, parentNode(parent, 2))
              //Popover.hide
            }
            true
          case fileaction.cancelRename ⇒
            withSafePath { sp ⇒
              parent.parentNode.parentNode.replaceChild(buildTitleRoot(sp.name).ref, parent.parentNode)
            }
            true
          case fileaction.download ⇒
            withSafePath { sp ⇒
              org.scalajs.dom.document.location.href = routes.downloadFile(client.Utils.toURI(sp.path))
              //Popover.hide
            }
            true
          case fileaction.extract ⇒
            withSafePath { sp ⇒
              extractTGZ(sp)
              //Popover.hide
            }
            true
          case fileaction.duplicate ⇒
            withSafePath { sp ⇒
              val newName = {
                val prefix = sp.path.last
                if (prefix.contains(".")) prefix.replaceFirst("[.]", "_1.")
                else prefix + "_1"
              }
              CoreUtils.duplicate(sp, newName)
              //Popover.hide
            }
            true
          case fileaction.execute ⇒
            import scala.concurrent.duration._
            withSafePath { sp ⇒
              Post(timeout = 120 seconds, warningTimeout = 60 seconds)[Api].runScript(ScriptData(sp), true).call().foreach { execInfo ⇒
                //Popover.hide
                showExecution()
              }
            }
            true
          case fileaction.toScript ⇒
            withSafePath { sp ⇒
              Plugins.fetch { p ⇒
                val wizardPanel = panels.modelWizardPanel(p.wizardFactories)
                wizardPanel.dialog.show
                wizardPanel.fromSafePath(sp)
                //Popover.hide
              }
            }
            true
          case _ ⇒ false
        }
      }
      else false
    }
    else false
  }

  def withSafePath(action: SafePath ⇒ Unit) =
    treeNodePanel.currentSafePath.now.foreach { sp ⇒
      action(sp)
    }

  val editTitle = inputTag().amend(
    placeholder := "File name",
    //  width := 200,
    onMountFocus,
    idAttr := fileaction.editInput
  )

  val overwriting = Var(false)

  val editForm: FormElement = form(
    editTitle,
    onSubmit --> { _ ⇒
      {
        withSafePath { sp ⇒
          if (overwriting.now) {
            rename(sp, () ⇒ {})
            //Popover.hide
          }
          else
            testRename(sp, editTitle.ref, editForm.ref, editForm.ref.parentNode.lastChild)
        }
        false
      }
    }
  )

  def replaceTitle(element: org.scalajs.dom.raw.Node) = {
    parentNode(element, 2).replaceChild(buildTitleRoot(editTitle.ref.value).ref, parentNode(element, 1))
    ()
  }

  def editDiv(tag: String, confirmString: String) = span(height := "24", width := "250",
    editForm,
    buttonGroup.amend(
      confirmRename(tag, confirmString),
      cancelRename
    )
  )

  def buildTitleRoot(title: String) = div(
    span(wordWrap := "break-word", title),
    renameTrigger
  )

  val titleRoot = buildTitleRoot(initSafePath.name)

  val contentRoot = {
    div(
      omsheet.centerElement,
      downloadTrigger,
      FileExtension(initSafePath.name) match {
        case FileExtension.TGZ | FileExtension.TAR | FileExtension.ZIP | FileExtension.TXZ ⇒
          iconAction(fileaction.extract, archive, "extract")
        case _ ⇒ span()
      },
      FileExtension(initSafePath.name) match {
        case FileExtension.OMS ⇒
          iconAction(fileaction.execute, execute, "run")
        case _ ⇒ span()
      },
      FileExtension(initSafePath.name) match {
        case FileExtension.JAR | FileExtension.NETLOGO | FileExtension.R | FileExtension.TGZ ⇒
          iconAction(fileaction.toScript, toScript, "to OMS")
        case _ ⇒ span()
      },
      duplicateTrigger,
      trashTrigger
    )
  }

  def extractTGZ(safePath: SafePath) =
    Post()[Api].extractTGZ(safePath).call().foreach {
      r ⇒
        r.error match {
          case Some(e: org.openmole.gui.ext.data.ErrorData) ⇒
            panels.alertPanel.detail("An error occurred during extraction", ErrorData.stackTrace(e), transform = RelativeCenterPosition, zone = FileZone)
          case _ ⇒ treeNodePanel.invalidCacheAndDraw
        }
    }

  def testRename(safePath: SafePath, parent: org.scalajs.dom.raw.Node, pivot: org.scalajs.dom.raw.Node, cancelNode: org.scalajs.dom.raw.Node) = {
    val newTitle = editTitle.ref.value
    val newSafePath = safePath.parent ++ newTitle
    //treeNodeTabs.saveAllTabs(() ⇒ {
    Post()[Api].existsExcept(newSafePath, false).call().foreach {
      b ⇒
        if (b) {
          overwriting.set(true)
          cancelNode.parentNode.replaceChild(editDiv(fileaction.confirmOverwrite, "Overwrite ?").ref, cancelNode)
          editTitle
        }
        else {
          overwriting.set(false)
          rename(safePath, () ⇒ replaceTitle(pivot))
          //Popover.hide
        }
    }
    //})
  }

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

  import org.scalajs.dom.raw._
  @tailrec
  private def parentNode(node: Node, depth: Int): Node = {
    if (depth == 0) node
    else parentNode(node.parentNode, depth - 1)
  }

}
