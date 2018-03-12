package org.openmole.gui.client.core.files

import org.openmole.gui.client.core._
import org.openmole.gui.client.core.panels.treeNodeTabs
import autowire._
import boopickle.Default._

import scala.concurrent.ExecutionContext.Implicits.global
import org.openmole.gui.ext.api.Api

import scalatags.JsDom.all._
import scaladget.bootstrapnative.bsn._
import org.openmole.gui.client.core.panels._
import org.openmole.gui.ext.data.SafePath
import org.openmole.gui.ext.tool.client._
import org.scalajs.dom.raw.{ HTMLElement, HTMLFormElement, Node }
import rx._

import scala.annotation.tailrec

object FileToolBox {

  type Prefix = String

  object fileaction {
    val trash: Prefix = "trash"
    val confirmTrash: Prefix = "co-trash"
    val cancelTrash: Prefix = "ca-trash"

    val rename: Prefix = "rename"
    val editInput: Prefix = "edit-input"
    val confirmRename: Prefix = "co-rename"
    val confirmOverwrite: Prefix = "co-overwrite"
    val cancelRename: Prefix = "ca-rename"

  }

  def apply(initSafePath: SafePath) = {
    new FileToolBox(initSafePath)
  }

}

import FileToolBox._

class FileToolBox(initSafePath: SafePath) {
  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

  import scaladget.tools._

  val baseGlyph = ms("glyphitem") +++ omsheet.color(DARK_GREY) +++ (marginTop := 2)
  val trash = baseGlyph +++ glyph_trash
  val edit = baseGlyph +++ glyph_edit
  val download_alt = baseGlyph +++ glyph_download_alt
  val archive = baseGlyph +++ glyph_archive
  val arrow_right_and_left = baseGlyph +++ glyph_arrow_right_and_left

  val trashTrigger = span(trash, id := fileaction.trash)
  val confirmTrashTrigger = button(btn_danger, "Delete file", id := fileaction.confirmTrash)
  val cancelTrashTrigger = button(btn_default, "Cancel", id := fileaction.cancelTrash)
  val confirmationGroup = buttonGroup()(confirmTrashTrigger, cancelTrashTrigger)

  val renameTrigger = span(edit, id := fileaction.rename)

  def confirmRename(tag: String, confirmString: String) = button(btn_danger, confirmString, id := tag)

  val cancelRename = button(btn_default, "Cancel", id := fileaction.cancelRename)

  def actions(element: HTMLElement, closeAll: () ⇒ Unit): Boolean = {
    val safePath = treeNodePanel.currentSafePath.now
    val parent = element.parentNode
    element.id match {
      case fileaction.trash ⇒

        parent.replaceChild(confirmationGroup, element)
        true
      case fileaction.confirmTrash ⇒
        treeNodePanel.currentSafePath.now.foreach { safePath ⇒
          CoreUtils.trashNode(safePath) {
            () ⇒
              treeNodeTabs -- safePath
              treeNodeTabs.checkTabs
              treeNodePanel.invalidCacheAndDraw
              closeAll()
          }
        }
        true
      case fileaction.cancelTrash ⇒
        parent.parentNode.replaceChild(contentRoot, parent)
        true
      case fileaction.rename ⇒
        safePath.foreach { sp ⇒
          editTitle.value = sp.name
          parent.parentNode.replaceChild(editDiv(fileaction.confirmRename, "Rename"), parent)
        }
        true
      case fileaction.editInput ⇒ true
      case fileaction.confirmRename ⇒
        safePath.foreach { sp ⇒
          testRename(sp, parent, parent, element.parentNode)
        }
        true
      case fileaction.confirmOverwrite ⇒
        safePath.foreach { sp ⇒
          rename(sp, () ⇒ {})
          parentNode(parent, 3).replaceChild(buildTitleRoot(sp.name), parentNode(parent, 2))
          closeAll()
        }
        true
      case fileaction.cancelRename ⇒
        safePath.foreach { sp ⇒
          parent.parentNode.parentNode.replaceChild(buildTitleRoot(sp.name), parent.parentNode)
        }
        true
      case _ ⇒
        println("unknown")
        false
    }
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
          treeNodePanel.currentSafePath.now.foreach { sp ⇒
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
  val contentRoot = div(
    trashTrigger
  )

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
