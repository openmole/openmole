package org.openmole.gui.client.core.files

import org.openmole.gui.client.core.{ Settings, PanelTriggerer, OMPost }
import org.openmole.gui.ext.data.{ UploadProject, DisplayableFile }
import org.openmole.gui.misc.utils.Utils
import org.openmole.gui.shared._
import org.openmole.gui.misc.js.BootstrapTags._
import org.scalajs.dom.html.Input
import org.scalajs.dom.raw.{ HTMLInputElement, DragEvent }
import scalatags.JsDom.all._
import scalatags.JsDom.{ tags ⇒ tags }
import org.openmole.gui.misc.js.{ BootstrapTags ⇒ bs, Select }
import org.openmole.gui.misc.js.JsRxTags._
import org.openmole.gui.misc.utils.Utils._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.async.Async.await
import TreeNode._
import autowire._
import rx._

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

  def apply(dirNode: DirNode)(implicit executionTriggerer: PanelTriggerer): TreeNodePanel = new TreeNodePanel(dirNode)

  def sons(dirNode: DirNode) = OMPost[Api].listFiles(dirNode).call()

}

import TreeNodePanel._

class TreeNodePanel(rootNode: DirNode)(implicit executionTriggerer: PanelTriggerer) {
  val dirNodeLine: Var[Seq[DirNode]] = Var(Seq(rootNode))
  val toBeEdited: Var[Option[TreeNode]] = Var(None)
  val dragState: Var[String] = Var("")
  val transferring: Var[FileTransferState] = Var(Standby())
  val draggedNode: Var[Option[TreeNode]] = Var(None)
  val fileDisplayer = new FileDisplayer

  computeAllSons(rootNode)

  val newNodeInput: Input = bs.input("")(
    placeholder := "File name",
    width := "130px",
    autofocus
  ).render

  val editNodeInput: Input = bs.input("")(
    placeholder := "Name",
    width := "130px",
    autofocus
  ).render

  val addRootDirButton: Select[TreeNodeType] = {
    val content = Seq(TreeNodeType.file, TreeNodeType.folder)
    Select("fileOrFolder", content, content.headOption, btn_success, glyph_folder_close, () ⇒ {
      addRootDirButton.content().map { c ⇒ newNodeInput.placeholder = c.name + " name" }
    })
  }

  val view = tags.div(
    Rx {
      val toDraw = dirNodeLine().drop(1)
      val dirNodeLineSize = toDraw.size
      buttonGroup()(
        glyphButton(" Home", btn_primary, glyph_home, goToDirAction(dirNodeLine().head)),
        if (dirNodeLineSize > 2) goToDirButton(toDraw(dirNodeLineSize - 3), Some("...")),
        toDraw.drop(dirNodeLineSize - 2).takeRight(2).map { dn ⇒ goToDirButton(dn) }
      )
    },
    Rx {
      tags.form(id := "adddir")(
        inputGroup(navbar_left)(
          inputGroupButton(addRootDirButton.selector),
          inputGroupButton(tags.form(newNodeInput, onsubmit := { () ⇒
            {
              val newFile = newNodeInput.value
              val currentDirNode = dirNodeLine().last
              addRootDirButton.content().map {
                _ match {
                  case dt: DirType ⇒ OMPost[Api].addDirectory(currentDirNode, newFile).call().foreach { b ⇒
                    if (b) refreshCurrentDirectory
                  }
                  case ft: FileType ⇒ OMPost[Api].addFile(currentDirNode, newFile).call().foreach { b ⇒
                    if (b) refreshCurrentDirectory
                  }
                }
              }
            }
            false
          })),
          inputGroupAddon(id := "fileinput-addon")(uploadButton((fileInput: HTMLInputElement) ⇒ {
            FileManager.upload(fileInput.files, dirNodeLine().last.safePath(), (p: FileTransferState) ⇒ transferring() = p, UploadProject())
          })),
          inputGroupAddon(id := "fileinput-addon")(
            tags.span(cursor := "pointer", `class` := " btn-file", id := "success-like", onclick := { () ⇒ refreshCurrentDirectory })(
              glyph(glyph_refresh)
            ))
        )
      )
    }, Rx {
      if (dirNodeLine().flatMap {
        _.sons()
      }.size == 0) {
        tags.div("Create a first OpenMOLE script (.oms)")(`class` := "message")
      }
      else {
        tags.div(`class` := "tree" + dragState() /*,
          ondragover := { (e: DragEvent) ⇒
            dragState() = " droppable hover"
            e.dataTransfer.dropEffect = "copy"
            e.preventDefault
            e.stopPropagation
            false
          },
          ondragend := { (e: DragEvent) ⇒
            dragState() = ""
            false
          },
          ondrop := { (e: DragEvent) ⇒
            dragState() = ""
            FileManager.upload(e.dataTransfer.files, dirNodeLine().last.safePath(), (p: FileTransferState) ⇒ transferring() = p, UploadProject())
            e.preventDefault
            e.stopPropagation
            false
          }*/ )(
          transferring() match {
            case _: Standby ⇒
            case _: Transfered ⇒
              refreshCurrentDirectory
              transferring() = Standby()
            case _ ⇒ progressBar(transferring().display, transferring().ratio)(id := "treeprogress")
          },
          drawTree(dirNodeLine().last.sons())
        )
      }
    }
  )

  def downloadFile(treeNode: TreeNode, saveFile: Boolean, onLoaded: String ⇒ Unit = (s: String) ⇒ {}) =
    FileManager.download(
      treeNode,
      (p: FileTransferState) ⇒ transferring() = p,
      onLoaded
    )

  def goToDirButton(dn: DirNode, name: Option[String] = None) = bs.button(name.getOrElse(dn.name()), btn_default)(onclick := { () ⇒
    goToDirAction(dn)()
  })

  def goToDirAction(dn: DirNode): () ⇒ Unit = () ⇒ {
    dirNodeLine() = dirNodeLine().zipWithIndex.filter(_._1 == dn).headOption.map {
      case (dn, index) ⇒ dirNodeLine().take(index + 1)
    }.getOrElse(dirNodeLine())
    drawTree(dirNodeLine().last.sons())
  }

  def drawTree(tns: Seq[TreeNode]) = tags.ul(`class` := "filelist")(
    for (tn ← tns.sorted(TreeNodeOrdering)) yield {
      drawNode(tn)
    }
  )

  def drawNode(node: TreeNode) = node match {
    case fn: FileNode ⇒
      clickableElement(fn, "file", () ⇒ {
        if (node.safePath().extension.displayable) {
          downloadFile(fn, false, (content: String) ⇒ fileDisplayer.display(rootNode.safePath(), node, content, executionTriggerer))
        }
      })
    case dn: DirNode ⇒ clickableElement(dn, "dir", () ⇒ {
      dirNodeLine() = dirNodeLine() :+ dn
    }
    )
  }

  def clickableElement(tn: TreeNode,
                       classType: String,
                       todo: () ⇒ Unit) =
    toBeEdited() match {
      case Some(etn: TreeNode) ⇒
        if (etn == tn) {
          editNodeInput.value = tn.name()
          tags.li(
            tags.form(id := "editnode")(
              editNodeInput,
              onsubmit := { () ⇒
                {
                  renameNode(tn, editNodeInput.value)
                  false
                }
              }
            )
          )
        }
        else ReactiveLine(tn, classType, todo).render
      case _ ⇒ ReactiveLine(tn, classType, todo).render
    }

  def computeSons(dn: DirNode): Unit = {
    sons(dn).foreach {
      sons ⇒
        dn.sons() = sons
    }
  }

  def computeAllSons(dn: DirNode): Unit = {
    sons(dn).foreach {
      sons ⇒
        dn.sons() = sons
        dn.sons().foreach {
          tn ⇒
            tn match {
              case (d: DirNode) ⇒ computeAllSons(d)
              case _            ⇒
            }
        }
    }
  }

  def refreshCurrentDirectory = {
    computeAllSons(dirNodeLine().last)
    newNodeInput.value = ""
  }

  def trashNode(treeNode: TreeNode) = {
    fileDisplayer.tabs -- treeNode
    OMPost[Api].deleteFile(treeNode).call().foreach { d ⇒
      refreshCurrentDirectory
    }
  }

  def renameNode(treeNode: TreeNode, newName: String) = OMPost[Api].renameFile(treeNode, newName).call().foreach {
    newNode ⇒
      fileDisplayer.tabs.rename(treeNode, newNode)
      refreshCurrentDirectory
      toBeEdited() = None
  }

  object ReactiveLine {
    def apply(tn: TreeNode, classType: String, todo: () ⇒ Unit) = new ReactiveLine(tn, classType, todo)
  }

  class ReactiveLine(tn: TreeNode, classType: String, todo: () ⇒ Unit) {

    val lineHovered: Var[Boolean] = Var(false)

    val render = tags.li(
      onmouseover := { () ⇒
        lineHovered() = true
      },
      onmouseout := { () ⇒
        lineHovered() = false
      }, ondragstart := { (e: DragEvent) ⇒
        e.dataTransfer.setData("text/plain", "nothing") //  FIREFOX TRICK
        draggedNode() match {
          case Some(t: TreeNode) ⇒
          case _ ⇒
            draggedNode() = Some(tn)
        }
        true
      }, ondragenter := { (e: DragEvent) ⇒
        false
      }, ondragover := { (e: DragEvent) ⇒
        e.dataTransfer.dropEffect = "copy"
        e.preventDefault
        false
      },
      ondrop := { (e: DragEvent) ⇒
        e.preventDefault
        draggedNode().map { sp ⇒
          tn match {
            case d: DirNode ⇒
              if (sp.safePath().path != tn.safePath().path) {
                OMPost[Api].move(sp.safePath(), tn.safePath()).call().foreach { b ⇒
                  refreshCurrentDirectory
                }
              }
            case _ ⇒
          }
        }
        draggedNode() = None
        false
      }, tags.span(
        cursor := "pointer",
        draggable := true,
        onclick := { () ⇒
          todo()
        },
        `class` := classType)(
          tags.i(id := "plusdir", `class` := {
            tn.hasSons match {
              case true  ⇒ "glyphicon glyphicon-plus-sign"
              case false ⇒ ""
            }
          }),
          tags.i(tn.name())
        ),
      tags.span(
        tags.span(`class` := "filesize")(tags.i(tn.readableSize)),
        tags.span(id := Rx {
          "treeline" + {
            if (lineHovered()) "-hover" else ""
          }
        })(
          glyphSpan(glyph_trash, () ⇒ trashNode(tn))(id := "glyphtrash", `class` := "glyphitem"),
          glyphSpan(glyph_edit, () ⇒ toBeEdited() = Some(tn))(`class` := "glyphitem"),
          a(glyphSpan(glyph_download, () ⇒ Unit)(`class` := "glyphitem"), href := s"downloadFile?path=${Utils.toURI(tn.safePath().path)}")
        )
      )
    )

  }

}
