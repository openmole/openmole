package org.openmole.gui.client.core.files

import org.openmole.gui.client.core.AbsolutePositioning.FileZone
import org.openmole.gui.client.core.{ AlertPanel, PanelTriggerer, OMPost }
import org.openmole.gui.client.core.CoreUtils
import org.openmole.gui.ext.data._
import org.openmole.gui.misc.utils.Utils
import org.openmole.gui.shared._
import fr.iscpif.scaladget.api.{ BootstrapTags ⇒ bs, ClassKeyAggregator }
import org.scalajs.dom.html.{ Input }
import org.scalajs.dom.raw.{ HTMLDivElement, DragEvent }
import scalatags.JsDom.all._
import scalatags.JsDom.{ TypedTag, tags ⇒ tags }
import org.openmole.gui.misc.js.{ _ }
import org.openmole.gui.misc.js.JsRxTags._
import org.openmole.gui.client.core.files.treenodemanager.{ instance ⇒ manager }
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import org.openmole.gui.misc.js.Tooltip._
import FileSorting._
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

  case class NodeEdition(node: TreeNode, replicateMode: Boolean = false)

  val toBeEdited: Var[Option[NodeEdition]] = Var(None)
  val dragState: Var[String] = Var("")
  val draggedNode: Var[Option[TreeNode]] = Var(None)
  val fileDisplayer = new FileDisplayer
  val fileToolBar = new FileToolBar(() ⇒ drawTree, () ⇒ refreshAndDraw)
  val tree: Var[TypedTag[HTMLDivElement]] = Var(tags.div())

  val editNodeInput: Input = bs.input("")(
    placeholder := "Name",
    width := "130px",
    height := "26px",
    autofocus
  ).render

  lazy val view = {
    manager.computeCurrentSons(() ⇒ drawTree, filter)
    tags.div(
      fileToolBar.div, Rx {
        val toDraw = manager.drop(1)
        val dirNodeLineSize = toDraw.size
        bs.div("tree-path")(
          goToDirButton(manager.head, OMTags.glyphString(glyph_home) + " left treePathItems"),
          toDraw.drop(dirNodeLineSize - 2).takeRight(2).map { dn ⇒ goToDirButton(dn, "treePathItems", s"| ${dn.name()}") }
        )

      },
      fileToolBar.sortingGroup.div,
      Rx {
        tree()
      }
    )
  }

  def filter: FileFilter = fileToolBar.fileFilter()

  def sorting: TreeSorting = fileToolBar.treeSorting()

  def downloadFile(treeNode: TreeNode, saveFile: Boolean, onLoaded: String ⇒ Unit = (s: String) ⇒ {}) =
    FileManager.download(
      treeNode,
      (p: ProcessState) ⇒ {
        fileToolBar.transferring() = p
      },
      onLoaded
    )

  def goToDirButton(dn: DirNode, ck: ClassKeyAggregator, name: String = "") = bs.span(ck)(name)(
    onclick := {
      () ⇒
        if (fileToolBar.hasFilter) CoreUtils.refreshCurrentDirectory(fileFilter = FileFilter.defaultFilter)
        fileToolBar.resetFilter
        manager.switch(dn)
        computeAndDraw
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

  def computeAndDraw = manager.computeCurrentSons(() ⇒ drawTree, filter)

  def refreshAndDraw = refreshAnd(() ⇒ drawTree)

  def refreshAnd(todo: () ⇒ Unit) = CoreUtils.refreshCurrentDirectory(todo, filter)

  def drawTree: Unit = {
    manager.computeAndGetCurrentSons(filter).map { sons ⇒
      tree() = tags.div(
        if (manager.isRootCurrent && manager.isProjectsEmpty) {
          tags.div("Create a first OpenMOLE script (.oms)")(`class` := "message")
        }
        else
          tags.table(`class` := "tree" + dragState())(
            tags.tr(
              tags.div(
                tags.table(`class` := "file-list")(
                  for (tn ← sons.sorted(fileToolBar.treeSorting().fileSorting).order(fileToolBar.treeSorting().fileOrdering)) yield {
                    drawNode(tn)
                  }
                )
              )
            )
          )
      )
    }
  }

  def drawNode(node: TreeNode) = node match {
    case fn: FileNode ⇒
      clickableElement(fn, "file", () ⇒ {
        displayNode(fn)
      })
    case dn: DirNode ⇒ clickableElement(dn, "dir", () ⇒ {
      manager + dn
      computeAndDraw
    })
  }

  def displayNode(tn: TreeNode) = tn match {
    case fn: FileNode ⇒
      if (fn.safePath().extension.displayable) {
        downloadFile(fn, false, (content: String) ⇒ {
          fileDisplayer.display(fn, content)
          refreshAndDraw
        })
      }
    case _ ⇒
  }

  def clickableElement(
    tn: TreeNode,
    classType: String,
    todo: () ⇒ Unit) = {
    toBeEdited() match {
      case Some(etn: NodeEdition) ⇒
        if (etn.node.path == tn.path) {
          editNodeInput.value = tn.name()
          tags.tr(
            tags.div(
              `class` := "edit-node",
              tags.form(
                editNodeInput,
                onsubmit := {
                  () ⇒
                    {
                      renameNode(tn, editNodeInput.value, etn.replicateMode)
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
  }

  def stringAlert(message: String, okaction: () ⇒ Unit) =
    AlertPanel.string(message, okaction, zone = FileZone())

  def trashNode(treeNode: TreeNode): Unit = {
    fileDisplayer.tabs -- treeNode
    stringAlert(
      s"Do you really want to delete ${
        treeNode.name()
      }?",
      () ⇒ {
        CoreUtils.trashNode(treeNode.safePath(), filter) {
          () ⇒
            fileDisplayer.tabs.checkTabs
            refreshAndDraw
        }
      }
    )
  }

  def renameNode(treeNode: TreeNode, newName: String, replicateMode: Boolean) = {
    def rename = OMPost[Api].renameFile(treeNode, newName).call().foreach { newNode ⇒
      fileDisplayer.tabs.rename(treeNode, newNode)
      toBeEdited() = None
      refreshAndDraw
      fileDisplayer.tabs.checkTabs
    }

    fileDisplayer.tabs.saveAllTabs(() ⇒ {
      OMPost[Api].existsExcept(treeNode.cloneWithName(newName), replicateMode).call().foreach { b ⇒
        if (b) stringAlert(s"${newName} already exists, overwrite ?", () ⇒ rename)
        else rename
      }
    })
  }

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
                      refreshAndDraw
                      fileDisplayer.tabs.checkTabs
                  })
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

    def timeOrSize(tn: TreeNode): String = fileToolBar.treeSorting().fileSorting match {
      case TimeSorting ⇒ tn.readableTime
      case _           ⇒ tn.readableSize
    }

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
        tags.i(id := "plusdir")
      ),
      tags.div(
        clickablePair(classType, todo),
        `class` := "fileNameOverflow " + classType + "Text",
        tn.name()
      ).tooltip(tn.name(), condition = () ⇒ tn.name().length > 24),
      Rx {
        manager.checkMode match {
          case true ⇒ tags.div(`class` := "file-info")(checkbox.onlyBox)
          case _ ⇒ tags.div(`class` := "file-info")(
            tags.span(`class` := "file-size")(tags.i(timeOrSize(tn))),
            tags.span(id := Rx {
              "treeline" + {
                if (lineHovered()) "-hover" else ""
              }
            })(
              glyphSpan(glyph_trash, () ⇒ trashNode(tn))(id := "glyphtrash", `class` := "glyphitem file-glyph"),
              glyphSpan(glyph_edit, () ⇒ {
                toBeEdited() = Some(NodeEdition(tn))
                drawTree
              })(`class` := "glyphitem file-glyph"),
              a(
                glyphSpan(glyph_download_alt, () ⇒ Unit)(`class` := "glyphitem file-glyph"),
                href := s"downloadFile?path=${Utils.toURI(tn.safePath().path)}"
              ),
              tn.safePath().extension match {
                case FileExtension.TGZ ⇒ glyphSpan(glyph_archive, () ⇒ {
                  OMPost[Api].extractTGZ(tn).call().foreach { r ⇒
                    refreshAndDraw
                  }
                })(`class` := "glyphitem file-glyph")
                case _ ⇒
              },
              glyphSpan(OMTags.glyph_arrow_right_and_left, () ⇒ CoreUtils.replicate(tn, (replicated: TreeNodeData) ⇒ {
                refreshAnd(() ⇒ {
                  toBeEdited() = Some(NodeEdition(replicated, true))
                  drawTree
                })
              }))(`class` := "glyphitem file-glyph")

            /*,
                      if (tn.isPlugin) glyphSpan(OMTags.glyph_plug, () ⇒
                        OMPost[Api].autoAddPlugins(tn.safePath()).call().foreach { p ⇒
                          panels.pluginTriggerer.open
                        })(`class` := "glyphitem file-glyph")*/
            )
          )
        }
      }
    )
  }

}
