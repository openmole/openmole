package org.openmole.gui.client.core.files

import org.openmole.gui.client.core.alert.AbsolutePositioning.{ RelativeCenterPosition, FileZone }
import org.openmole.gui.client.core.alert.AlertPanel
import org.openmole.gui.client.core.files.FileToolBar.{ CopyTool, TrashTool }
import org.openmole.gui.client.core.{ PanelTriggerer, OMPost }
import org.openmole.gui.client.core.CoreUtils
import org.openmole.gui.ext.data._
import org.openmole.gui.misc.utils.{ stylesheet, Utils }
import org.openmole.gui.shared._
import fr.iscpif.scaladget.api.{ BootstrapTags ⇒ bs, Popup }
import org.scalajs.dom.html.Input
import org.scalajs.dom.raw._
import scalatags.JsDom.all._
import scalatags.JsDom.{ TypedTag, tags ⇒ tags }
import org.openmole.gui.misc.js.JsRxTags._
import org.openmole.gui.client.core.files.treenodemanager.{ instance ⇒ manager }
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import TreeNode._
import autowire._
import rx._
import bs._
import fr.iscpif.scaladget.stylesheet.{ all ⇒ sheet }
import sheet._

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
  val fileToolBar = new FileToolBar(this)
  val tree: Var[TypedTag[HTMLDivElement]] = Var(tags.div())
  val selectionMode = Var(false)

  val editNodeInput: Input = bs.input()(
    placeholder := "Name",
    width := "130px",
    height := "26px",
    autofocus
  ).render

  lazy val view = {
    manager.computeCurrentSons(() ⇒ drawTree, filter)
    val goToModifierSeq = glyph_home +++ floatLeft +++ "treePathItems"
    tags.div(
      fileToolBar.div, Rx {
      val toDraw = manager.drop(1)
      val dirNodeLineSize = toDraw.size
      div(ms("tree-path"))(
        goToDirButton(manager.head, goToModifierSeq),
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

  def downloadFile(treeNode: TreeNode, saveFile: Boolean, onLoaded: String ⇒ Unit = (s: String) ⇒ {}) =
    FileManager.download(
      treeNode,
      (p: ProcessState) ⇒ {
        fileToolBar.transferring() = p
      },
      onLoaded
    )

  def goToDirButton(dn: DirNode, ck: ModifierSeq, name: String = "") = span(ck)(name)(
    onclick := {
      () ⇒
        fileToolBar.resetFilter
        CoreUtils.refreshCurrentDirectory(fileFilter = fileToolBar.fileFilter())
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
      tree() = div(
        if (manager.isRootCurrent && manager.isProjectsEmpty) {
          div("Create a first OpenMOLE script (.oms)")(ms("message"))
        }
        else
          tags.table(ms("tree" + dragState()))(
            tr(
              tags.table(ms("file-list"))(
                for (tn ← sons) yield {
                  drawNode(tn)
                }
              )
            )
          )
      )
    }
  }

  def drawNode(node: TreeNode) = node match {
    case fn: FileNode ⇒
      clickableElement(fn, TreeNodeType.file, () ⇒ {
        displayNode(fn)
      })
    case dn: DirNode ⇒ clickableElement(dn, TreeNodeType.folder, () ⇒ {
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
    tn:           TreeNode,
    treeNodeType: TreeNodeType,
    todo:         () ⇒ Unit
  ) = {
    toBeEdited() match {
      case Some(etn: NodeEdition) ⇒
        if (etn.node.path == tn.path) {
          editNodeInput.value = tn.name()
          tr(
            div(
              height := 26,
              form(
                editNodeInput,
                onsubmit := {
                  () ⇒
                    {
                      if (etn.node.name() == editNodeInput.value) {
                        toBeEdited() = None
                        drawTree
                      }
                      else renameNode(tn, editNodeInput.value, etn.replicateMode)
                      false
                    }
                }
              )
            )
          )
        }
        else ReactiveLine(tn, treeNodeType, todo).render
      case _ ⇒ ReactiveLine(tn, treeNodeType, todo).render
    }
  }

  def stringAlert(message: String, okaction: () ⇒ Unit) =
    AlertPanel.string(message, okaction, transform = RelativeCenterPosition, zone = FileZone)

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

  def turnSelectionTo(b: Boolean) = selectionMode() = b

  object ReactiveLine {
    def apply(tn: TreeNode, treeNodeType: TreeNodeType, todo: () ⇒ Unit) = new ReactiveLine(tn, treeNodeType, todo)
  }

  class ReactiveLine(tn: TreeNode, treeNodeType: TreeNodeType, todo: () ⇒ Unit) {

    val lineHovered: Var[Boolean] = Var(false)
    val selected: Var[Boolean] = Var(manager.isSelected(tn))

    val clickablePair = (treeNodeType match {
      case fn: FileNodeType ⇒ stylesheet.file
      case _                ⇒ stylesheet.dir
    }) +++ floatLeft +++ pointer +++ Seq(
      draggable := true,
      onclick := { () ⇒ todo() }
    )

    def timeOrSize(tn: TreeNode): String = fileToolBar.fileFilter().fileSorting match {
      case TimeSorting ⇒ tn.readableTime
      case _           ⇒ tn.readableSize
    }

    lazy val fileIndent: ModifierSeq = tn match {
      case d: DirNode ⇒ sheet.paddingLeft(27)
      case _          ⇒ sheet.emptyMod
    }

    def clearSelectionExecpt(tn: TreeNode) = {
      selected() = true
      manager.clearSelectionExecpt(tn)
    }

    def addToSelection(b: Boolean): Unit = {
      selected() = b
      manager.setSelected(tn, selected())
    }

    def addToSelection: Unit = addToSelection(!selected())

    val render: Modifier = {
      val baseGlyph = sheet.marginTop(2) +++ "glyphitem"
      val trash = baseGlyph +++ glyph_trash
      val edit = baseGlyph +++ glyph_edit
      val download_alt = baseGlyph +++ glyph_download_alt
      val archive = baseGlyph +++ glyph_archive
      val arrow_right_and_left = baseGlyph +++ glyph_arrow_right_and_left

      val rowDiv = div(
        relativePosition,
        onmouseover := { () ⇒ lineHovered() = true },
        onmouseout := { () ⇒ lineHovered() = false },
        ondragstart := { (e: DragEvent) ⇒
          e.dataTransfer.setData("text/plain", "nothing") //  FIREFOX TRICK
          draggedNode() match {
            case Some(t: TreeNode) ⇒
            case _                 ⇒ draggedNode() = Some(tn)
          }
          true
        },
        ondragenter := { (e: DragEvent) ⇒
          false
        },
        ondragover := { (e: DragEvent) ⇒
          e.dataTransfer.dropEffect = "move"
          e.preventDefault
          false
        },
        ondrop := {
          dropAction(tn)
        }, div(
          clickablePair,
          Rx {
            span(stylesheet.fileNameOverflow +++ fileIndent)(
              tn.name()
            )
          }
        ).tooltip(tags.span(tn.name()), popupStyle = whitePopup, arrowStyle = Popup.whiteBottomArrow, condition = () ⇒ tn.name().length > 24),
        div(stylesheet.fileInfo)(
          Rx {
            if (selectionMode()) div
            else {
              div(
                span(stylesheet.fileSize)(tags.i(timeOrSize(tn))),
                span(`class` := Rx {
                  if (lineHovered()) "opaque" else "transparent"
                })(
                  span(onclick := { () ⇒ trashNode(tn) }, trash),
                  span(onclick := { () ⇒
                    toBeEdited() = Some(NodeEdition(tn))
                    drawTree
                  }, edit),
                  a(
                    span(onclick := { () ⇒ Unit })(download_alt),
                    href := s"downloadFile?path=${Utils.toURI(tn.safePath().path)}"
                  ),
                  tn.safePath().extension match {
                    case FileExtension.TGZ ⇒
                      span(archive, onclick := { () ⇒
                        OMPost[Api].extractTGZ(tn).call().foreach { r ⇒
                          refreshAndDraw
                        }
                      })
                    case _ ⇒
                  },
                  span(onclick := { () ⇒
                    CoreUtils.replicate(tn, (replicated: TreeNodeData) ⇒ {
                      refreshAnd(() ⇒ {
                        toBeEdited() = Some(NodeEdition(replicated, true))
                        drawTree
                      })
                    })
                  })(arrow_right_and_left)

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
      )
      tr(
        rowDiv(
          Rx {
            if (selectionMode()) {
              div(
                onclick := { (e: MouseEvent) ⇒
                  addToSelection
                  if (e.ctrlKey) clearSelectionExecpt(tn)
                }
              )({
                  if (selected()) {
                    fileToolBar.selectedTool() match {
                      case Some(TrashTool) ⇒ stylesheet.fileSelectedForDeletion
                      case _               ⇒ stylesheet.fileSelected
                    }
                  }
                  else stylesheet.fileSelectionMode
                }, span(
                  stylesheet.fileSelectionMessage,
                  if (selected()) {
                    fileToolBar.selectedTool() match {
                      case Some(TrashTool) ⇒ glyph_trash
                      case Some(CopyTool)  ⇒ glyph_copy
                      case _               ⇒
                    }
                  }
                  else emptyMod
                ))
            }
            else div(overflow := "hidden")
          }
        )
      )
    }

  }

}

