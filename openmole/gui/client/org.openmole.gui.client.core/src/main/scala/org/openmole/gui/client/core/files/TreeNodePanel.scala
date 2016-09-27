package org.openmole.gui.client.core.files

import org.openmole.gui.client.core.alert.AbsolutePositioning.{ FileZone, RelativeCenterPosition }
import org.openmole.gui.client.core.alert.AlertPanel
import org.openmole.gui.client.core.files.FileToolBar.{ PluginTool, TrashTool }
import org.openmole.gui.client.core.{ CoreUtils, OMPost }
import org.openmole.gui.client.core.Waiter._
import org.openmole.gui.ext.data._
import org.openmole.gui.misc.utils.{ Utils, stylesheet }
import org.openmole.gui.shared._
import fr.iscpif.scaladget.api.{ Popup, BootstrapTags ⇒ bs }
import org.openmole.gui.misc.utils.{ stylesheet ⇒ omsheet }
import org.scalajs.dom.html.Input
import org.scalajs.dom.raw._

import scalatags.JsDom.all._
import scalatags.JsDom.{ TypedTag, tags }
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

object TreeNodePanel {
  val instance = new TreeNodePanel

  def apply() = instance

  def refreshAnd(todo: () ⇒ Unit) = instance.refreshAnd(todo)

  def refreshAndDraw = instance.refreshAndDraw

}

class TreeNodePanel {

  case class NodeEdition(node: TreeNode, replicateMode: Boolean = false)

  val toBeEdited: Var[Option[NodeEdition]] = Var(None)
  val fileDisplayer = new FileDisplayer
  val fileToolBar = new FileToolBar(this)
  val tree: Var[TypedTag[HTMLElement]] = Var(tags.div())
  val selectionMode = Var(false)

  selectionMode.trigger {
    if (!selectionMode.now) manager.clearSelection
  }

  val editNodeInput: Input = bs.input()(
    placeholder := "Name",
    width := "240px",
    height := "24px",
    autofocus
  ).render

  lazy val fileControler = Rx {
    val toDraw = manager.drop(1)()
    val dirNodeLineSize = toDraw.size
    div(ms("tree-path"))(
      goToDirButton(manager.head(), glyph_home +++ floatLeft +++ "treePathItems"),
      toDraw.drop(dirNodeLineSize - 2).takeRight(2).map { dn ⇒ goToDirButton(dn, "treePathItems", s"| ${dn.name()}") }
    )
  }

  lazy val labelArea =
    div(
      Rx {
        if (manager.copied().isEmpty) tags.div
        else tags.label("paste")(label_danger, stylesheet.pasteLabel, onclick := { () ⇒ paste(manager.copied(), manager.current()) })
      },
      fileToolBar.sortingGroup.div
    )

  lazy val view = {
    drawTree
    tags.div(
      Rx {
        tree()
      }
    )
  }

  private def paste(safePaths: Seq[SafePath], to: SafePath) = {
    def refreshWithNoError = {
      manager.noError
      refreshAndDraw
    }

    def onpasted = {
      manager.emptyCopied
      // unselectTool
    }

    val same = safePaths.filter { sp ⇒
      sp == to
    }
    if (same.isEmpty) {
      CoreUtils.testExistenceAndCopyProjectFilesTo(safePaths, to).foreach { existing ⇒
        if (existing.isEmpty) {
          refreshWithNoError
          onpasted
        }
        else manager.setFilesInError(
          "Some files already exists, overwrite ?",
          existing,
          () ⇒ CoreUtils.copyProjectFilesTo(safePaths, to).foreach { b ⇒
            refreshWithNoError
            onpasted
          }, () ⇒ {
            refreshWithNoError
            // unselectTool
          }
        )
      }
    }
    else manager.setFilesInComment(
      "Paste a folder in itself is not allowed",
      same,
      () ⇒ manager.noError
    )
  }

  def filter: FileFilter = fileToolBar.fileFilter.now

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
        fileToolBar.clearMessage
        manager.clearSelection
        turnSelectionTo(false)
        manager.switch(dn)
        drawTree
    }
  )

  def refreshAndDraw = refreshAnd(() ⇒ {
    drawTree
  })

  def refreshAnd(todo: () ⇒ Unit) = CoreUtils.updateSons(manager.current.now, todo, filter)

  def computePluggables = fileToolBar.selectedTool.now match {
    case Some(PluginTool) ⇒ manager.computePluggables(() ⇒ if (!manager.pluggables.now.isEmpty) turnSelectionTo(true))
    case _                ⇒
  }

  def drawTree: Unit = {
    computePluggables
    tree() = manager.computeCurrentSons(filter).withFutureWaiter("Get files", (sons: (Seq[TreeNode], Boolean)) ⇒ {
      if (sons._2) manager.updateSon(manager.current.now, sons._1)
      div(
        if (manager.isRootCurrent && manager.isProjectsEmpty) {
          div("Create a first OpenMOLE script (.oms)")(ms("message"))
        }
        else
          tags.table(
            tbody(omsheet.fileList)(
              for (tn ← sons._1) yield {
                drawNode(tn)
              }
            )
          )
      )
    })

  }

  def drawNode(node: TreeNode) = node match {
    case fn: FileNode ⇒
      clickableElement(fn, TreeNodeType.file, () ⇒ {
        displayNode(fn)
      })
    case dn: DirNode ⇒ clickableElement(dn, TreeNodeType.folder, () ⇒ {
      manager + dn
      fileToolBar.clearMessage
      fileToolBar.unselectTool

      drawTree
    })
  }

  def displayNode(tn: TreeNode) = tn match {
    case fn: FileNode ⇒
      if (fn.safePath.now.extension.displayable) {
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
    toBeEdited.now match {
      case Some(etn: NodeEdition) ⇒
        if (etn.node.path == tn.path) {
          editNodeInput.value = tn.name.now
          tr(
            td(
              height := 26,
              form(
                editNodeInput,
                onsubmit := {
                  () ⇒
                    {
                      if (etn.node.name.now == editNodeInput.value) {
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

  def stringAlertWithDetails(message: String, detail: String) =
    AlertPanel.detail(message, detail, transform = RelativeCenterPosition, zone = FileZone)

  def trashNode(treeNode: TreeNode): Unit = {
    stringAlert(
      s"Do you really want to delete ${
        treeNode.name.now
      }?",
      () ⇒ {
        CoreUtils.trashNode(treeNode.safePath.now) {
          () ⇒
            fileDisplayer.tabs -- treeNode
            fileDisplayer.tabs.checkTabs
            refreshAndDraw
        }
      }
    )
  }

  def extractTGZ(tnd: TreeNode) =
    OMPost[Api].extractTGZ(tnd).call().foreach { r ⇒
      r.error match {
        case Some(e: org.openmole.gui.ext.data.Error) ⇒ stringAlertWithDetails("An error occurred during extraction", e.stackTrace)
        case _                                        ⇒ refreshAndDraw
      }
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

  def turnSelectionTo(b: Boolean) = selectionMode() = b

  val settingsSet: Var[Option[ReactiveLine]] = Var(None)

  def unsetSettings = settingsSet() = None

  object ReactiveLine {
    def apply(tn: TreeNode, treeNodeType: TreeNodeType, todo: () ⇒ Unit) = new ReactiveLine(tn, treeNodeType, todo)
  }

  class ReactiveLine(tn: TreeNode, treeNodeType: TreeNodeType, todo: () ⇒ Unit) {

    val selected: Var[Boolean] = Var(manager.isSelected(tn))

    val clickablePair = (treeNodeType match {
      case fn: FileNodeType ⇒ stylesheet.file
      case _                ⇒ stylesheet.dir
    }) +++ floatLeft +++ pointer +++ Seq(
      onclick := { (e: MouseEvent) ⇒
        if (!selectionMode.now) todo()
      }
    )

    def timeOrSize(tn: TreeNode): String = fileToolBar.fileFilter.now.fileSorting match {
      case TimeSorting ⇒ tn.readableTime
      case _           ⇒ tn.readableSize
    }

    lazy val fileIndent: ModifierSeq = tn match {
      case d: DirNode ⇒ sheet.paddingLeft(22)
      case _          ⇒ sheet.emptyMod
    }

    def clearSelectionExecpt(tn: TreeNode) = {
      selected() = true
      manager.clearSelectionExecpt(tn)
    }

    def addToSelection(b: Boolean): Unit = {
      selected() = b
      manager.setSelected(tn, selected.now)
    }

    def addToSelection: Unit = addToSelection(!selected.now)

    val render: Modifier = {
      val baseGlyph = sheet.marginTop(2) +++ "glyphitem"
      val settingsGlyph = ms("glyphitem") +++ glyph_settings +++ sheet.paddingLeft(4)
      val trash = baseGlyph +++ glyph_trash
      val edit = baseGlyph +++ glyph_edit
      val download_alt = baseGlyph +++ glyph_download_alt
      val archive = baseGlyph +++ glyph_archive
      val arrow_right_and_left = baseGlyph +++ glyph_arrow_right_and_left

      {
        tr(
          td(
            onclick := { (e: MouseEvent) ⇒
              {
                if (selectionMode.now) {
                  addToSelection
                  if (e.ctrlKey) clearSelectionExecpt(tn)
                }
              }
            },
            Rx {
              span(clickablePair)(
                div(stylesheet.fileNameOverflow +++ fileIndent)(tn.name())
              ).tooltip(
                  tags.span(tn.name()), popupStyle = whitePopup, arrowStyle = Popup.whiteBottomArrow, condition = () ⇒ tn.name().length > 24
                )
            },
            Rx {
              div(stylesheet.fileInfo)(
                if (!selectionMode()) {
                  div(
                    if (settingsSet() == Some(this)) {
                      span(
                        span(onclick := { () ⇒ unsetSettings }, baseGlyph)(
                          raw("&#215")
                        ),
                        tags.span(onclick := { () ⇒
                          trashNode(tn)
                          unsetSettings
                        }, trash),
                        span(onclick := { () ⇒
                          toBeEdited() = Some(NodeEdition(tn))
                          drawTree
                        }, edit),
                        a(
                          span(onclick := { () ⇒ unsetSettings })(download_alt),
                          href := s"downloadFile?path=${Utils.toURI(tn.safePath().path)}"
                        ),
                        tn.safePath().extension match {
                          case FileExtension.TGZ | FileExtension.TAR ⇒
                            span(archive, onclick := { () ⇒
                              extractTGZ(tn)
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
                      )
                    }
                    else
                      span(stylesheet.fileSize)(
                        tags.i(timeOrSize(tn)),
                        tags.span(onclick := { () ⇒
                          settingsSet() = Some(this)
                        }, settingsGlyph)
                      )
                  )
                }
                else div()
              )
            },
            Rx {
              if (selectionMode()) {
                div(width := "100%",
                  if (selected()) {
                    fileToolBar.selectedTool() match {
                      case Some(TrashTool) ⇒ stylesheet.fileSelectedForDeletion
                      case Some(PluginTool) if manager.pluggables().contains(tn) ⇒ stylesheet.fileSelected
                      case _ ⇒ stylesheet.fileSelected
                    }
                  }
                  else stylesheet.fileSelectionMode,
                  span(stylesheet.fileSelectionMessage))
              }
              else div()
            }
          )
        )

      }
    }
  }

}

