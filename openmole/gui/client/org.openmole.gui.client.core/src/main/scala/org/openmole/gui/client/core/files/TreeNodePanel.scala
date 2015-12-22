package org.openmole.gui.client.core.files

import org.openmole.gui.client.core.AbsolutePositioning.FileZone
import org.openmole.gui.client.core.ClientProcessState._
import org.openmole.gui.client.core.{ panels, AlertPanel, PanelTriggerer, OMPost }
import org.openmole.gui.ext.data._
import org.openmole.gui.misc.utils.Utils
import org.openmole.gui.shared._
import fr.iscpif.scaladget.api.{ BootstrapTags ⇒ bs }
import org.scalajs.dom.html.Input
import org.scalajs.dom.raw.{ HTMLInputElement, DragEvent }
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

object TreeNodePanel {

  def sons(dirNode: DirNode) = OMPost[Api].listFiles(dirNode).call()

}

import TreeNodePanel._

class TreeNodePanel(implicit executionTriggerer: PanelTriggerer) {
  val toBeEdited: Var[Option[TreeNode]] = Var(None)
  val dragState: Var[String] = Var("")
  val transferring: Var[ProcessState] = Var(Standby())
  val draggedNode: Var[Option[TreeNode]] = Var(None)
  val fileDisplayer = new FileDisplayer

  computeAllSons(manager.current)

  val newNodeInput: Input = bs.input("")(
    placeholder := "File name",
    width := "130px",
    autofocus
  ).render

  val editNodeInput: Input = bs.input("")(
    placeholder := "Name",
    width := "130px",
    height := "26px",
    autofocus
  ).render

  val addRootDirButton: Select[TreeNodeType] = {
    val content = Seq((TreeNodeType.file, key(glyph_file)), (TreeNodeType.folder, key(glyph_folder_close)))
    Select("fileOrFolder", content, content.map {
      _._1
    }.headOption, btn_success + "borderRightFlat", () ⇒ {
      addRootDirButton.content().map { c ⇒ newNodeInput.placeholder = c.name + " name" }
    })
  }

  val view = tags.div(
    Rx {
      tags.form(id := "adddir")(
        tags.div(`class` := "tree-header",
          inputGroup(navbar_left)(
            inputGroupButton(addRootDirButton.selector),
            inputGroupButton(tags.form(newNodeInput, onsubmit := { () ⇒
              {
                val newFile = newNodeInput.value
                val currentDirNode = manager.current
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
            inputGroupAddon(id := "fileinput-addon")(
              tags.label(`class` := "inputFileStyleSmall",
                uploadButton((fileInput: HTMLInputElement) ⇒ {
                  FileManager.upload(fileInput, manager.current.safePath(), (p: ProcessState) ⇒ transferring() = p, UploadProject())
                })).tooltip("Upload file")),
            inputGroupAddon(id := "fileinput-addon")(
              tags.span(cursor := "pointer", `class` := " btn-file", id := "success-like", onclick := { () ⇒ refreshCurrentDirectory })(
                glyph(glyph_refresh)
              ).tooltip("Refresh file tree", RightDirection()))
          )
        )
      )
    },
    Rx {
      val toDraw = manager.drop(1)
      val dirNodeLineSize = toDraw.size
      tags.div(`class` := "tree-path",
        buttonGroup()(
          glyphButton(" Home", btn_primary, glyph_home, goToDirAction(manager.head))(dropPairs(manager.head)),
          if (dirNodeLineSize > 2) goToDirButton(toDraw(dirNodeLineSize - 3), Some("...")),
          toDraw.drop(dirNodeLineSize - 2).takeRight(2).map { dn ⇒ goToDirButton(dn) }
        )
      )
    },
    tags.table(`class` := "tree" + dragState())(
      tags.tr(
        tags.td(height := "40px",
          textAlign := "center",
          transferring.withWaiter { _ ⇒
            refreshCurrentDirectory
            tags.div()
          }
        )
      ),
      tags.tr(
        Rx {
          if (manager.allNodes.size == 0) tags.div("Create a first OpenMOLE script (.oms)")(`class` := "message")
          else drawTree(manager.current.sons())
        }
      )
    )
  )

  def downloadFile(treeNode: TreeNode, saveFile: Boolean, onLoaded: String ⇒ Unit = (s: String) ⇒ {
  }) =
    FileManager.download(
      treeNode,
      (p: ProcessState) ⇒ transferring() = p,
      onLoaded
    )

  def goToDirButton(dn: DirNode, name: Option[String] = None) = bs.button(name.getOrElse(dn.name()), btn_default)(
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

  def drawTree(tns: Seq[TreeNode]) = tags.table(`class` := "file-list")(
    for (tn ← tns.sorted(TreeNodeOrdering)) yield {
      drawNode(tn)
    }
  )

  def drawNode(node: TreeNode) = node match {
    case fn: FileNode ⇒
      clickableElement(fn, "file", () ⇒ {
        if (node.safePath().extension.displayable) {
          downloadFile(fn, false, (content: String) ⇒ fileDisplayer.display(manager.root.safePath(), node, content, executionTriggerer))
        }
      })
    case dn: DirNode ⇒ clickableElement(dn, "dir", () ⇒ manager + dn)
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
    refresh(manager.current)
  }

  def refresh(dn: DirNode) = {
    computeAllSons(dn)
    newNodeInput.value = ""
  }

  def trashNode(treeNode: TreeNode) = {
    fileDisplayer.tabs -- treeNode
    AlertPanel.popup(s"Do you really want to delete ${
      treeNode.name()
    }?",
      () ⇒ {
        OMPost[Api].deleteFile(treeNode.safePath()).call().foreach {
          d ⇒
            refreshCurrentDirectory
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
          refreshCurrentDirectory
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
                      refreshCurrentDirectory
                      refresh(d)
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

    def clickablePair(classType: String, todo: () ⇒ Unit) = Seq(
      style := "float:left",
      cursor := "pointer",
      fontWeight := "bold",
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
      tags.div(`class` := "file-info",
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
                refreshCurrentDirectory
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
    )

  }

}
