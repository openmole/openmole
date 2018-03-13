package org.openmole.gui.client.core.files

import org.openmole.gui.client.core._
import org.openmole.gui.client.core.panels.treeNodeTabs
import autowire._
import boopickle.Default._
import org.openmole.gui.client.core.alert.AbsolutePositioning._
import org.openmole.gui.client.core.alert.AlertPanel

import scala.concurrent.ExecutionContext.Implicits.global
import org.openmole.gui.ext.api.Api

import scalatags.JsDom.all._
import scaladget.bootstrapnative.bsn._
import org.openmole.gui.client.core.panels._
import org.openmole.gui.ext.data.{ DataUtils, FileExtension, SafePath }
import org.openmole.gui.ext.tool.client._
import org.scalajs.dom.raw._
import rx._

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
  }

  def apply(initSafePath: SafePath) = {
    new FileToolBox(initSafePath)
  }

}

import FileToolBox._

class FileToolBox(initSafePath: SafePath) {

  import scaladget.tools._

  val baseGlyph = ms("glyphitem") +++ omsheet.color(DARK_GREY) +++ (marginTop := 2)
  val trash = baseGlyph +++ glyph_trash
  val edit = baseGlyph +++ glyph_edit
  val download_alt = baseGlyph +++ glyph_download_alt
  val archive = baseGlyph +++ glyph_archive
  val arrow_right_and_left = baseGlyph +++ glyph_arrow_right_and_left

  val trashTrigger = span(trash, id := fileaction.trash)
  val downloadTrigger = a(span(download_alt, id := fileaction.download))
  val confirmTrashTrigger = button(btn_danger, "Delete file", id := fileaction.confirmTrash)
  val cancelTrashTrigger = button(btn_default, "Cancel", id := fileaction.cancelTrash)
  val confirmationGroup = buttonGroup()(confirmTrashTrigger, cancelTrashTrigger)

  val renameTrigger = span(edit, id := fileaction.rename)

  def confirmRename(tag: String, confirmString: String) = button(btn_danger, confirmString, id := tag)

  val cancelRename = button(btn_default, "Cancel", id := fileaction.cancelRename)

  val duplicateTrigger = span(arrow_right_and_left, id := fileaction.duplicate)

  def actions(element: HTMLElement): Boolean = {
    val parent = element.parentNode
    element.id match {
      case fileaction.trash ⇒
        parent.replaceChild(confirmationGroup, element)
        true
      case fileaction.confirmTrash ⇒
        withSafePath { safePath ⇒
          CoreUtils.trashNode(safePath) {
            () ⇒
              treeNodeTabs -- safePath
              treeNodeTabs.checkTabs
              treeNodePanel.invalidCacheAndDraw
              treeNodePanel.closeAllPopovers
          }
        }
        true
      case fileaction.cancelTrash ⇒
        parent.parentNode.replaceChild(contentRoot, parent)
        true
      case fileaction.rename ⇒
        withSafePath { sp ⇒
          editTitle.value = sp.name
          parent.parentNode.replaceChild(editDiv(fileaction.confirmRename, "Rename"), parent)
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
          parentNode(parent, 3).replaceChild(buildTitleRoot(sp.name), parentNode(parent, 2))
          treeNodePanel.closeAllPopovers
        }
        true
      case fileaction.cancelRename ⇒
        withSafePath { sp ⇒
          parent.parentNode.parentNode.replaceChild(buildTitleRoot(sp.name), parent.parentNode)
        }
        true
      case fileaction.download ⇒
        withSafePath { sp ⇒
          org.scalajs.dom.document.location.href = s"downloadFile?path=${Utils.toURI(sp.path)}"
          treeNodePanel.closeAllPopovers
        }
        true
      case fileaction.extract ⇒
        withSafePath { sp ⇒
          extractTGZ(sp)
        }
        true
      case fileaction.duplicate ⇒
        withSafePath { sp ⇒
          val newName = {
            val prefix = sp.path.last
            sp match {
              case _: DirNode ⇒ prefix + "_1"
              case _ ⇒
                if (prefix.contains("."))
                  prefix.replaceFirst("[.]", "_1.")
                else prefix + "_1"
            }
          }
          CoreUtils.replicate(sp, newName)
          treeNodePanel.closeAllPopovers
        }
        true
      case _ ⇒
        println("unknown")
        false
    }
  }

  def withSafePath(action: SafePath ⇒ Unit) =
    treeNodePanel.currentSafePath.now.foreach { sp ⇒
      action(sp)
    }

  val editTitle = inputTag()(
    placeholder := "File name",
    //  width := 200,
    autofocus,
    id := fileaction.editInput
  ).render

  val overwriting = Var(false)

  val editForm: HTMLFormElement = form(
    editTitle,
    onsubmit := {
      () ⇒
        {
          withSafePath { sp ⇒
            if (overwriting.now) {
              rename(sp, () ⇒ {})
              treeNodePanel.closeAllPopovers
            }
            else
              testRename(sp, editTitle, editForm, editForm.parentNode.lastChild)
          }
          false
        }
    }
  ).render

  def replaceTitle(element: Node) = {
    parentNode(element, 2).replaceChild(buildTitleRoot(editTitle.value), parentNode(element, 1))
    ()
  }

  def editDiv(tag: String, confirmString: String) = span(height := 24, width := 250)(
    editForm,
    buttonGroup()(
      confirmRename(tag, confirmString),
      cancelRename
    )
  )

  def buildTitleRoot(title: String) = div(
    span(title),
    renameTrigger
  )

  val titleRoot = buildTitleRoot(initSafePath.name)

  val contentRoot = {
    div(
      downloadTrigger,
      DataUtils.fileToExtension(initSafePath.name) match {
        case FileExtension.TGZ | FileExtension.TAR | FileExtension.ZIP ⇒
          span(archive, id := fileaction.extract)
        case _ ⇒ span
      },
      duplicateTrigger,
      trashTrigger
    )
  }

  def extractTGZ(safePath: SafePath) =
    post()[Api].extractTGZ(safePath).call().foreach {
      r ⇒
        r.error match {
          case Some(e: org.openmole.gui.ext.data.Error) ⇒
            AlertPanel.detail("An error occurred during extraction", e.stackTrace, transform = RelativeCenterPosition, zone = FileZone)
          case _ ⇒ treeNodePanel.invalidCacheAndDraw
        }
    }

  def testRename(safePath: SafePath, parent: Node, pivot: Node, cancelNode: Node) = {
    val newTitle = editTitle.value
    val newSafePath = safePath.parent ++ newTitle
    treeNodeTabs.saveAllTabs(() ⇒ {
      post()[Api].existsExcept(newSafePath, false).call().foreach {
        b ⇒
          if (b) {
            overwriting() = true
            cancelNode.parentNode.replaceChild(editDiv(fileaction.confirmOverwrite, "Overwrite ?"), cancelNode)
            editTitle.focus()
          }
          else {
            overwriting() = false
            rename(safePath, () ⇒ replaceTitle(pivot))
            treeNodePanel.closeAllPopovers
          }
      }
    })
  }

  def rename(safePath: SafePath, replacing: () ⇒ Unit) = {
    val newTitle = editTitle.value
    post()[Api].renameFile(safePath, newTitle).call().foreach {
      newNode ⇒
        treeNodeTabs.rename(safePath, newNode)
        treeNodePanel.invalidCacheAndDraw
        treeNodeTabs.checkTabs
        treeNodePanel.currentSafePath() = Some(safePath.parent ++ newTitle)
        replacing()
    }
  }

  @tailrec
  private def parentNode(node: Node, depth: Int): Node = {
    if (depth == 0) node
    else parentNode(node.parentNode, depth - 1)
  }

}
