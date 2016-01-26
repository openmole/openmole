package org.openmole.gui.client.core.files

import org.openmole.gui.client.core.AbsolutePositioning.FileZone
import org.openmole.gui.client.core.{ panels, AlertPanel, PanelTriggerer, OMPost }
import org.openmole.gui.client.core.CoreUtils
import org.openmole.gui.ext.data._
import org.openmole.gui.misc.utils.Utils
import org.openmole.gui.shared._
import fr.iscpif.scaladget.api.{ BootstrapTags ⇒ bs, ClassKeyAggregator }
import org.scalajs.dom.html.Input
import org.scalajs.dom.raw.DragEvent
import scalatags.JsDom.all._
import scalatags.JsDom.{ tags ⇒ tags }
import org.openmole.gui.misc.js.{ _ }
import org.openmole.gui.misc.js.JsRxTags._
import org.openmole.gui.client.core.files.treenodemanager.{ instance ⇒ manager }
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import org.openmole.gui.misc.js.Tooltip._
import TreeNode._
import autowire._
import rx._
import bs._

/*
 * Copyright (C) 16/04/15 // mathieu.leclaire@openmole.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

class TreeNodePanel(implicit executionTriggerer: PanelTriggerer) {
  val toBeEdited: Var[Option[TreeNode]] = Var(None)
  val dragState: Var[String] = Var("")
  val draggedNode: Var[Option[TreeNode]] = Var(None)
  val fileDisplayer = new FileDisplayer
  val fileTooBar = new FileToolBar

  CoreUtils.refreshCurrentDirectory()

  val editNodeInput: Input = bs.input("")(
    placeholder := "Name",
    width := "130px",
    height := "26px",
    autofocus
  ).render

  val view = tags.div(
    Rx {
      fileTooBar.div
    }, Rx {
      val toDraw = manager.drop(1)
      val dirNodeLineSize = toDraw.size
      bs.div("tree-path")(
        goToDirButton(manager.head, OMTags.glyphString(glyph_home) + " left treePathItems"),
        toDraw.drop(dirNodeLineSize - 2).takeRight(2).map { dn ⇒ goToDirButton(dn, "treePathItems", s"| ${dn.name()}") }
      )

    },
    tags.table(`class` := "tree" + dragState())(
      tags.tr(
        Rx {
          if (manager.allNodes.size == 0) tags.div("Create a first OpenMOLE script (.oms)")(`class` := "message")
          else drawTree(manager.current.sons())
        }
      )
    )
  )

  def downloadFile(treeNode: TreeNode, saveFile: Boolean, onLoaded: String ⇒ Unit = (s: String) ⇒ {}) =
    FileManager.download(
      treeNode,
      (p: ProcessState) ⇒ fileTooBar.transferring() = p,
      onLoaded
    )

  def goToDirButton(dn: DirNode, ck: ClassKeyAggregator, name: String = "") = bs.span(ck)(name)(
    onclick := {
      () ⇒
        goToDirAction(dn)()
    }, dropPairs(dn)
  )

  def dropPairs(dn: DirNode) = Seq(
    draggable := true, ondrop := {
      dropAction(dn)
    },
    ondragenter := {
      (e: DragEvent) ⇒
        false
    },
    ondragover := {
      (e: DragEvent) ⇒
        e.dataTransfer.dropEffect = "move"
        e.preventDefault
        false
    }
  )

  def goToDirAction(dn: DirNode): () ⇒ Unit = () ⇒ {
    manager.switch(dn)
    drawTree(manager.current.sons())
  }

  def drawTree(tns: Seq[TreeNode]) = {
    tags.table(`class` := "file-list")(
      for (tn ← tns.sorted(TreeNodeOrdering)) yield {
        drawNode(tn)
      }
    )
  }

  def drawNode(node: TreeNode) = node match {
    case fn: FileNode ⇒
      clickableElement(fn, "file", () ⇒ {
        displayNode(fn)
      })
    case dn: DirNode ⇒ clickableElement(dn, "dir", () ⇒ manager + dn)
  }

  def displayNode(tn: TreeNode) = tn match {
    case fn: FileNode ⇒
      if (fn.safePath().extension.displayable) {
        downloadFile(fn, false, (content: String) ⇒ fileDisplayer.display(fn, content))
      }
    case _ ⇒
  }

  def clickableElement(tn: TreeNode,
                       classType: String,
                       todo: () ⇒ Unit) =
    toBeEdited() match {
      case Some(etn: TreeNode) ⇒
        if (etn == tn) {
          editNodeInput.value = tn.name()
          tags.tr(
            tags.div(`class` := "edit-node",
              tags.form(
                editNodeInput,
                onsubmit := {
                  () ⇒
                    {
                      renameNode(tn, editNodeInput.value)
                      false
                    }
                }
              )
            )
          )
        }
        else ReactiveLine(tn, classType, todo).render
      case _ ⇒ ReactiveLine(tn, classType, todo).render
    }

  def trashNode(treeNode: TreeNode): Unit = {
    fileDisplayer.tabs -- treeNode
    AlertPanel.string(s"Do you really want to delete ${
      treeNode.name()
    }?",
      () ⇒ {
        CoreUtils.trashNode(treeNode.safePath()) { () ⇒
          CoreUtils.refreshCurrentDirectory()
          fileDisplayer.tabs.checkTabs
        }
      }, zone = FileZone()
    )
  }

  def renameNode(treeNode: TreeNode, newName: String) =
    fileDisplayer.tabs.saveAllTabs(() ⇒
      OMPost[Api].renameFile(treeNode, newName).call().foreach {
        newNode ⇒
          fileDisplayer.tabs.rename(treeNode, newNode)
          CoreUtils.refreshCurrentDirectory()
          toBeEdited() = None
          fileDisplayer.tabs.checkTabs
      }
    )

  def dropAction(tn: TreeNode) = {
    (e: DragEvent) ⇒
      e.preventDefault
      draggedNode().map {
        sp ⇒
          tn match {
            case d: DirNode ⇒
              if (sp.safePath().path != d.safePath().path) {
                fileDisplayer.tabs.saveAllTabs(() ⇒
                  OMPost[Api].move(sp.safePath(), tn.safePath()).call().foreach {
                    b ⇒
                      CoreUtils.refreshCurrentDirectory()
                      fileDisplayer.tabs.checkTabs
                  }
                )
              }
            case _ ⇒
          }
      }
      draggedNode() = None
      false
  }

  object ReactiveLine {
    def apply(tn: TreeNode, classType: String, todo: () ⇒ Unit) = new ReactiveLine(tn, classType, todo)
  }

  class ReactiveLine(tn: TreeNode, classType: String, todo: () ⇒ Unit) {

    val lineHovered: Var[Boolean] = Var(false)
    val checkbox = CheckBox("", manager.isSelected(tn), key("marginRight5")) { cb ⇒
      manager.setSelected(tn, cb.checked)
    }

    def clickablePair(classType: String, todo: () ⇒ Unit) = Seq(
      style := "float:left",
      cursor := "pointer",
      draggable := true,
      onclick := { () ⇒ todo() },
      `class` := classType
    )

    val render = tags.tr(
      onmouseover := { () ⇒ lineHovered() = true },
      onmouseout := { () ⇒ lineHovered() = false }, ondragstart := { (e: DragEvent) ⇒
        e.dataTransfer.setData("text/plain", "nothing") //  FIREFOX TRICK
        draggedNode() match {
          case Some(t: TreeNode) ⇒
          case _                 ⇒ draggedNode() = Some(tn)
        }
        true
      }, ondragenter := { (e: DragEvent) ⇒
        false
      }, ondragover := { (e: DragEvent) ⇒
        e.dataTransfer.dropEffect = "move"
        e.preventDefault
        false
      },
      ondrop := {
        dropAction(tn)
      },
      tags.div(clickablePair(classType, todo))(
        tags.i(id := "plusdir", `class` := {
          tn.hasSons match {
            case true  ⇒ "glyphicon glyphicon-plus-sign"
            case false ⇒ ""
          }
        })),
      tags.div(
        clickablePair(classType, todo),
        `class` := "fileNameOverflow " + classType + "Text",
        tn.name()
      ).tooltip(tn.name(), condition = () ⇒ tn.name().length > 24),

      manager.selectionMode() match {
        case true ⇒
          tags.div(`class` := "file-info")(checkbox.onlyBox)
        case _ ⇒ tags.div(`class` := "file-info")(
          tags.span(`class` := "file-size")(tags.i(tn.readableSize)),
          tags.span(id := Rx {
            "treeline" + {
              if (lineHovered()) "-hover" else ""
            }
          })(
            glyphSpan(glyph_trash, () ⇒ trashNode(tn))(id := "glyphtrash", `class` := "glyphitem file-glyph"),
            glyphSpan(glyph_edit, () ⇒ toBeEdited() = Some(tn))(`class` := "glyphitem file-glyph"),
            a(glyphSpan(glyph_download_alt, () ⇒ Unit)(`class` := "glyphitem file-glyph"),
              href := s"downloadFile?path=${Utils.toURI(tn.safePath().path)}"),
            tn.safePath().extension match {
              case FileExtension.TGZ ⇒ glyphSpan(glyph_archive, () ⇒ {
                OMPost[Api].extractTGZ(tn).call().foreach { r ⇒
                  CoreUtils.refreshCurrentDirectory()
                }
              })(`class` := "glyphitem file-glyph")
              case _ ⇒
            },
            if (tn.isPlugin) glyphSpan(OMTags.glyph_plug, () ⇒
              OMPost[Api].autoAddPlugins(tn.safePath()).call().foreach { p ⇒
                panels.pluginTriggerer.open
              })(`class` := "glyphitem file-glyph")
          )
        )
      }
    )
  }

}
