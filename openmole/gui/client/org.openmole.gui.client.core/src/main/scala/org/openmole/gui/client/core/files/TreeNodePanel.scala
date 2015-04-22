package org.openmole.gui.client.core.files

import org.openmole.gui.client.core.Post
import org.openmole.gui.ext.data._
import org.openmole.gui.shared._
import org.openmole.gui.misc.js.{ Forms ⇒ bs }
import org.openmole.gui.misc.js.Forms._
import org.scalajs.dom.html.UList
import org.scalajs.dom.raw.{ HTMLUListElement, HTMLDivElement }
import scalatags.JsDom.all._
import scalatags.JsDom.{ TypedTag, tags ⇒ tags }
import org.openmole.gui.misc.js.JsRxTags._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
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

  implicit def stringToVarString(s: String): Var[String] = Var(s)

  implicit def treeNodeDataToTreeNode(tnd: TreeNodeData): TreeNode =
    if (tnd.isDirectory) DirNode(tnd.name, tnd.canonicalPath, Var(Seq()))
    else FileNode(tnd.name, tnd.canonicalPath)

  implicit def seqTreeNodeDataToSeqTreeNode(tnds: Seq[TreeNodeData]): Seq[TreeNode] = tnds.map(treeNodeDataToTreeNode(_))

  def apply(path: String) = new TreeNodePanel(DirNode(path.split("/").last, path))

  def computeSons(dirNode: DirNode) = Post[Api].listFiles(dirNode.canonicalPath()).call().foreach { sons ⇒
    dirNode.sons() = sons
  }

  def drawNode(node: TreeNode) = node match {
    case fn: FileNode ⇒ clickableElement(fn.name(), FILE, "file", () ⇒ {
      println(fn.name() + " display the file")
    })
    case dn: DirNode ⇒ clickableElement(dn.name(), dn.state(), "dir", () ⇒ {
      dn.state() match {
        case EXPANDED ⇒ dn.state() = COLLAPSED
        case COLLAPSED ⇒
          computeSons(dn)
          dn.state() = EXPANDED
      }
    }
    )
  }

  def clickableElement(name: String, state: NodeState, classType: String, todo: () ⇒ Unit) = tags.li(
    tags.span(cursor := "pointer", `class` := classType)(
      tags.i(`class` := {
        state match {
          case COLLAPSED ⇒ "glyphicon glyphicon-plus-sign"
          case EXPANDED  ⇒ "glyphicon glyphicon-minus-sign"
          case _         ⇒ ""
        }
      }),
      tags.i(name, onclick := { () ⇒
        {
          todo()
        }
      })
    )
  )
}

import TreeNodePanel._

class TreeNodePanel(_treeNode: DirNode) {

  val treeNode = Var(_treeNode)

  val view = tags.div(
    `class` := "tree"
  )(
      drawTree(treeNode())
    )

  def drawTree(tn: TreeNode): Rx[TypedTag[UList]] =
    Rx {
      println("RX")
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