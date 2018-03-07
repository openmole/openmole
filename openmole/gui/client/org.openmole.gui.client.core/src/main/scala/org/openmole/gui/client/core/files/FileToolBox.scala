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
  type UID = String

  object prefix {
    val trash: Prefix = "tra"
    val confirmTrash: Prefix = "cot"
    val cancelTrash: Prefix = "cat"

    val rename: Prefix = "ren"
    val editInput: Prefix = "edi"
    val confirmRename: Prefix = "cor"
    val confirmOverwrite: Prefix = "coo"
    val cancelRename: Prefix = "car"

  }

  def uid(prefix: Prefix): UID = uuID.short(prefix)
  def prefix(id: UID): Prefix = id.take(3)

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

  val TRASH = uid(prefix.trash)
  val CONFIRM_TRASH = uid(prefix.confirmTrash)
  val CANCEL_TRASH = uid(prefix.cancelTrash)

  val RENAME = uid(prefix.rename)
  val EDIT_INPUT = uid(prefix.editInput)
  val CONFIRM_RENAME = uid(prefix.confirmRename)
  val CONFIRM_OVERWRITE = uid(prefix.confirmOverwrite)
  val CANCEL_RENAME = uid(prefix.cancelRename)

  val trashTrigger = span(trash, id := TRASH)
  val confirmTrashTrigger = button(btn_danger, "Delete file", id := CONFIRM_TRASH)
  val cancelTrashTrigger = button(btn_default, "Cancel", id := CANCEL_TRASH)
  val confirmationGroup = buttonGroup()(confirmTrashTrigger, cancelTrashTrigger)

  val renameTrigger = span(edit, id := RENAME)

  def confirmRename(tag: String, confirmString: String) = button(btn_danger, confirmString, id := tag)

  val cancelRename = button(btn_default, "Cancel", id := CANCEL_RENAME)

  def actions(element: HTMLElement, closeAll: () ⇒ Unit): Boolean = {
    val safePath = treeNodePanel.currentSafePath.now
    val parent = element.parentNode
    prefix(element.id) match {
      case prefix.trash ⇒
        
        parent.replaceChild(confirmationGroup, element)
        true
      case prefix.confirmTrash ⇒
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
      case prefix.cancelTrash ⇒
        parent.parentNode.replaceChild(contentRoot, parent)
        true
      case prefix.rename ⇒
        safePath.foreach { sp ⇒
          editTitle.value = sp.name
          parent.parentNode.replaceChild(editDiv(CONFIRM_RENAME, "Rename"), parent)
        }
        true
      case prefix.editInput ⇒ true
      case prefix.confirmRename ⇒
        safePath.foreach { sp ⇒
          testRename(sp, parent)
        }
        true
      case prefix.confirmOverwrite ⇒
        safePath.foreach { sp ⇒
          rename(sp, (safePath: SafePath) ⇒ {})
          parentNode(parent, 3).replaceChild(buildTitleRoot(sp.name), parentNode(parent, 2))
          closeAll()
        }
        true
      case prefix.cancelRename ⇒
        safePath.foreach { sp ⇒
          parent.parentNode.parentNode.replaceChild(buildTitleRoot(sp.name), parent.parentNode)
        }
        true
      case _ ⇒
        println("unknown")
        false
    }
  }

  //def safePath = treenodemanager.instance.current.now ++ treeNode.name.now

  val editTitle = inputTag()(
    placeholder := "File name",
    //  width := 200,
    autofocus,
    id := EDIT_INPUT
  ).render

  val editForm: HTMLFormElement = form(
    editTitle,
    onsubmit := {
      () ⇒
        {
          treeNodePanel.currentSafePath.now.foreach { sp ⇒
            testRename(sp, editForm.parentNode)
            // rename((safePath: SafePath) ⇒ parentNode(editForm, 2).replaceChild(buildTitleRoot(safePath.name), parentNode(editForm, 1)))
          }
          false
        }
    }
  ).render

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

  def testRename(safePath: SafePath, parent: Node) = {
    val newTitle = editTitle.value
    val newSafePath = safePath.parent ++ newTitle
    treeNodeTabs.saveAllTabs(() ⇒ {
      post()[Api].existsExcept(newSafePath, false).call().foreach {
        b ⇒
          if (b) parent.parentNode.replaceChild(editDiv(CONFIRM_OVERWRITE, "Overwrite ?"), parent)
          else rename(safePath, (safePath: SafePath) ⇒ parentNode(parent, 2).replaceChild(buildTitleRoot(safePath.name), parentNode(parent, 1)))
      }
    })
  }

  def rename(safePath: SafePath, replacing: SafePath ⇒ Unit) = {
    val newTitle = editTitle.value
    post()[Api].renameFile(safePath, newTitle).call().foreach {
      newNode ⇒
        treeNodeTabs.rename(safePath, newNode)
        treeNodePanel.invalidCacheAndDraw
        treeNodeTabs.checkTabs
        treeNodePanel.currentSafePath() = Some(safePath.parent ++ newTitle)
        replacing(newNode)
    }
  }

  @tailrec
  private def parentNode(node: Node, depth: Int): Node = {
    if (depth == 0) node
    else parentNode(node.parentNode, depth - 1)
  }

}
