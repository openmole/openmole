package org.openmole.gui.client.core.files

import org.openmole.gui.client.core.Post
import org.openmole.gui.shared._
import org.openmole.gui.misc.js.Forms._
import org.scalajs.dom.html.{ Input, UList }
import scalatags.JsDom.all._
import scalatags.JsDom.{ TypedTag, tags ⇒ tags }
import org.openmole.gui.misc.js.{ Forms ⇒ bs }
import org.openmole.gui.misc.js.JsRxTags._
import org.openmole.gui.misc.utils.Utils._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import TreeNode._
import NodeState._
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

  def apply(dirNode: DirNode): TreeNodePanel = new TreeNodePanel(dirNode)

  def dirNode(path: String) = DirNode(path.split("/").last, path)

  def computeAllSons(dn: DirNode): Unit = {
    sons(dn).foreach { sons ⇒
      dn.sons() = sons
      dn.sons().foreach { tn ⇒
        tn match {
          case (d: DirNode) ⇒
            computeAllSons(d)
          case _ ⇒
        }
      }
    }
  }

  def sons(dirNode: DirNode) = Post[Api].listFiles(dirNode).call()

  def drawNode(node: TreeNode) = node match {
    case fn: FileNode ⇒ clickableElement(fn, "file", () ⇒ {
      println(fn.name() + " display the file")
    })
    case dn: DirNode ⇒ clickableElement(dn, "dir", () ⇒ {
      dn.state() match {
        case EXPANDED ⇒ dn.state() = COLLAPSED
        case COLLAPSED ⇒
          dn.state() = EXPANDED
      }
    }
    )
  }

  def clickableElement(treeNode: TreeNode,
                       classType: String,
                       todo: () ⇒ Unit) = tags.li(
    tags.span(cursor := "pointer", onclick := { () ⇒
      {
        todo()
      }
    }, `class` := classType)(
      tags.i(`class` := {
        treeNode.hasSons match {
          case true ⇒
            treeNode.state() match {
              case COLLAPSED ⇒ "glyphicon glyphicon-plus-sign"
              case EXPANDED  ⇒ "glyphicon glyphicon-minus-sign"
            }
          case false ⇒ ""
        }
      }),
      tags.i(treeNode.name())
    )
  )

}

import TreeNodePanel._

class TreeNodePanel(_dirNode: DirNode) {

  val treeNode: Var[DirNode] = Var(_dirNode)
  val addDirState: Var[Boolean] = Var(false)

  computeAllSons(_dirNode)

  val addRootDirButton =
    bs.glyphButton(" Add", btn_success, glyph_folder_close)(`type` := "submit")(onclick := { () ⇒
      rootDirInput.value = ""
      addDirState() = !addDirState()
    })

  val rootDirInput: Input = bs.input("")(
    placeholder := "Folder name",
    width := "130px",
    autofocus
  ).render

  val view = tags.div(
    Rx {
      bs.form()(
        inputGroup(navbar_left)(
          inputGroupButton(addRootDirButton),
          if (addDirState()) rootDirInput else tags.span()
        ),
        onsubmit := { () ⇒
          {
            Post[Api].addRootDirectory(rootDirInput.value).call().foreach { b ⇒
              if (b) computeAllSons(treeNode())
              addDirState() = !addDirState()
            }
          }
        })
    },
    tags.div(`class` := "tree")(
      Rx {
        drawTree(treeNode().sons())
      }
    )
  )

  def drawTree(tns: Seq[TreeNode]): TypedTag[UList] = tags.ul(
    for (tn ← tns.sorted(TreeNodeOrdering)) yield {
      drawTree(tn)
    }
  )

  def drawTree(tn: TreeNode): Rx[TypedTag[UList]] =
    Rx {
      tags.ul(
        drawNode(tn),
        tn match {
          case d: DirNode ⇒
            d.state() match {
              case EXPANDED ⇒ tags.ul(
                for (son ← d.sons().sorted(TreeNodeOrdering)) yield {
                  drawTree(son)
                }
              )
              case _ ⇒
            }
          case _ ⇒
        }
      )
    }

}