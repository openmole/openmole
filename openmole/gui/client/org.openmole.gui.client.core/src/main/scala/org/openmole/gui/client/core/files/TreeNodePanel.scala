package org.openmole.gui.client.core.files

import java.text.SimpleDateFormat
import java.util.Date

import org.openmole.gui.client.core.alert.AbsolutePositioning.{ FileZone, RelativeCenterPosition }
import org.openmole.gui.client.core.alert.AlertPanel
import org.openmole.gui.client.core.files.FileToolBar.{ FilterTool, PluginTool, TrashTool }
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
import org.scalajs.dom
import sheet._

import scala.scalajs.js

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

  def refreshAnd(todo: () ⇒ Unit) = {
    instance.refreshAnd(todo)
  }

  def refreshAndDraw = instance.refreshAndDraw

}

class TreeNodePanel {

  case class NodeEdition(safePath: SafePath, replicateMode: Boolean = false)

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
    val current = manager.current()
    div(ms("tree-path"))(
      goToDirButton(manager.root, glyph_home +++ floatLeft +++ "treePathItems"),
      Seq(current.parent, current).filterNot { sp ⇒
        sp.isEmpty || sp == manager.root
      }.map { sp ⇒
        goToDirButton(sp, "treePathItems", s"| ${sp.name}")
      }
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

  def downloadFile(safePath: SafePath, saveFile: Boolean, onLoaded: String ⇒ Unit = (s: String) ⇒ {}) =
    FileManager.download(
      safePath,
      (p: ProcessState) ⇒ {
        fileToolBar.transferring() = p
      },
      onLoaded
    )

  def goToDirButton(safePath: SafePath, ck: ModifierSeq, name: String = "") = span(ck)(name)(
    onclick := {
      () ⇒
        fileToolBar.resetFilter
        fileToolBar.clearMessage
        manager.clearSelection
        turnSelectionTo(false)
        manager.switch(safePath)
        drawTree
    }
  )

  def refreshAndDraw = {
    refreshAnd(() ⇒ {
      drawTree
    })
  }

  def refreshAnd(todo: () ⇒ Unit) = {
    manager.computeCurrentSons(filter).foreach { x ⇒
      todo()
    }
  }

  def computePluggables = fileToolBar.selectedTool.now match {
    case Some(PluginTool) ⇒ manager.computePluggables(() ⇒ if (!manager.pluggables.now.isEmpty) turnSelectionTo(true))
    case _                ⇒
  }

  val scrollHeight = Var(0)

  import org.scalajs.dom

  def drawTree: Unit = {
    computePluggables
    tree() = manager.computeCurrentSons(filter).withFutureWaiter("Get files", (sons: (Seq[TreeNode], Boolean)) ⇒ {
      lazy val moreEntries: HTMLAnchorElement = a(
        pointer +++ (fontSize := 12),
        onclick := { () ⇒
          /*fileToolBar.fileFilter.now.threshold.map { th ⇒
            OMPost[Api].moreEntries(manager.current.now, th).call().withFutureWaiterAndSideEffect("", (me: Seq[TreeNodeData]) ⇒ {
              fileBody.removeChild(moreEntries)
              me.foreach { e ⇒
                fileBody.appendChild(drawNode(e).render)
              }
              fileBody.appendChild(moreEntries)
            })
          }*/
        }, "More entries"
      ).render

      lazy val fileBody: HTMLTableSectionElement = tbody(omsheet.fileList)(
        for (tn ← sons._1) yield {
          drawNode(tn)
        },
        if (sons._1.length > fileToolBar.fileNumberThreshold) {
          moreEntries
        }
        else div()

      ).render
      // if (sons._2) manager.updateSon(manager.current.now, sons._1)
      tags.table(
        if (manager.isRootCurrent && manager.isProjectsEmpty) {
          div("Create a first OpenMOLE script (.oms)")(ms("message"))
        }
        else {
          fileBody
        }
      )
    })

  }

  def drawNode(node: TreeNode) = node match {
    case fn: FileNode ⇒
      clickableElement(fn, TreeNodeType.file, () ⇒ {
        displayNode(fn)
      })
    case dn: DirNode ⇒ clickableElement(dn, TreeNodeType.folder, () ⇒ {
      manager + dn.name.now
      fileToolBar.clearMessage
      fileToolBar.unselectTool
    })
  }

  def displayNode(tn: TreeNode) = tn match {
    case fn: FileNode ⇒
      val ext = DataUtils.fileToExtension(tn.name.now)
      val tnSafePath = manager.current.now ++ tn.name.now
      if (ext.displayable) {
        downloadFile(tnSafePath, false, (content: String) ⇒ {
          fileDisplayer.display(tnSafePath, content, ext)
          refreshAndDraw
        })
      }
    case _ ⇒
  }

  def clickableElement(
    tn:           TreeNode,
    treeNodeType: TreeNodeType,
    todo:         () ⇒ Unit
  ): TypedTag[dom.html.TableRow] = {
    val tnSafePath = manager.current.now ++ tn.name.now
    toBeEdited.now match {
      case Some(etn: NodeEdition) ⇒
        if (etn.safePath.path == tnSafePath.path) {
          editNodeInput.value = tn.name.now
          tr(
            td(
              height := 26,
              form(
                editNodeInput,
                onsubmit := {
                  () ⇒
                    {
                      if (etn.safePath.name == editNodeInput.value) {
                        toBeEdited() = None
                        drawTree
                      }
                      else renameNode(tnSafePath, editNodeInput.value, etn.replicateMode)
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

  def trashNode(safePath: SafePath): Unit = {
    stringAlert(
      s"Do you really want to delete ${
        safePath.name
      }?",
      () ⇒ {
        CoreUtils.trashNode(safePath) {
          () ⇒
            fileDisplayer.tabs -- safePath
            fileDisplayer.tabs.checkTabs
            refreshAndDraw
        }
      }
    )
  }

  def extractTGZ(safePath: SafePath) =
    OMPost[Api].extractTGZ(safePath).call().foreach { r ⇒
      r.error match {
        case Some(e: org.openmole.gui.ext.data.Error) ⇒ stringAlertWithDetails("An error occurred during extraction", e.stackTrace)
        case _                                        ⇒ refreshAndDraw
      }
    }

  def renameNode(safePath: SafePath, newName: String, replicateMode: Boolean) = {
    def rename = OMPost[Api].renameFile(safePath, newName).call().foreach { newNode ⇒
      fileDisplayer.tabs.rename(safePath, newNode)
      toBeEdited() = None
      refreshAndDraw
      fileDisplayer.tabs.checkTabs
    }

    fileDisplayer.tabs.saveAllTabs(() ⇒ {
      OMPost[Api].existsExcept(safePath.copy(path = safePath.path.dropRight(1) :+ newName), replicateMode).call().foreach { b ⇒
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
    val tnSafePath = manager.current.now ++ tn.name.now

    val clickablePair = (treeNodeType match {
      case fn: FileNodeType ⇒ stylesheet.file
      case _                ⇒ stylesheet.dir
    }) +++ floatLeft +++ pointer +++ Seq(
      onclick := { (e: MouseEvent) ⇒
        if (!selectionMode.now) todo()
      }
    )

    def timeOrSize(tn: TreeNode): String = fileToolBar.fileFilter.now.fileSorting match {
      case TimeSorting ⇒ CoreUtils.longTimeToString(tn.time)
      case _           ⇒ CoreUtils.readableByteCount(tn.size)
    }

    lazy val fileIndent: ModifierSeq = tn match {
      case d: DirNode ⇒ sheet.paddingLeft(22)
      case _          ⇒ sheet.emptyMod
    }

    def clearSelectionExecpt(safePath: SafePath) = {
      selected() = true
      manager.clearSelectionExecpt(safePath)
    }

    def addToSelection(b: Boolean): Unit = {
      selected() = b
      manager.setSelected(tnSafePath, selected.now)
    }

    def addToSelection: Unit = addToSelection(!selected.now)

    val render: TypedTag[dom.html.TableRow] = {
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
                  if (e.ctrlKey) clearSelectionExecpt(tnSafePath)
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
                          trashNode(tnSafePath)
                          unsetSettings
                        }, trash),
                        span(onclick := { () ⇒
                          toBeEdited() = Some(NodeEdition(tnSafePath))
                          drawTree
                        }, edit),
                        a(
                          span(onclick := { () ⇒ unsetSettings })(download_alt),
                          href := s"downloadFile?path=${Utils.toURI(tnSafePath.path)}"
                        ),
                        DataUtils.fileToExtension(tn.name.now) match {
                          case FileExtension.TGZ | FileExtension.TAR ⇒
                            span(archive, onclick := { () ⇒
                              extractTGZ(tnSafePath)
                            })
                          case _ ⇒
                        },
                        span(onclick := { () ⇒
                          CoreUtils.replicate(tnSafePath, (replicated: SafePath) ⇒ {
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
                div(
                  width := "100%",
                  if (selected()) {
                    fileToolBar.selectedTool() match {
                      case Some(TrashTool) ⇒ stylesheet.fileSelectedForDeletion
                      case Some(PluginTool) if manager.pluggables().contains(tn) ⇒ stylesheet.fileSelected
                      case _ ⇒ stylesheet.fileSelected
                    }
                  }
                  else stylesheet.fileSelectionMode,
                  span(stylesheet.fileSelectionMessage)
                )
              }
              else div()
            }
          )
        )

      }
    }
  }

}

