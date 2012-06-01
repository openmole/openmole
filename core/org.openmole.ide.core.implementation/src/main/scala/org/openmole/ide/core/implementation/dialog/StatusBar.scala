/*
 * Copyright (C) 2012 mathieu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.core.implementation.dialog

import java.awt.Color
import javax.swing.event.HyperlinkEvent
import javax.swing.event.HyperlinkListener
import javax.swing.text.html.HTML
import javax.swing.text.html.HTMLDocument
import javax.swing.text.html.HTMLEditorKit
import org.openmole.ide.core.implementation.execution.ScenesManager
import org.openmole.ide.core.model.panel.PanelMode._
import org.openmole.ide.core.model.dataproxy.IDataProxyUI
import org.openmole.ide.core.model.workflow.ISceneContainer
import scala.collection.mutable.HashMap
import scala.swing.EditorPane

object StatusBar extends EditorPane { statusBar ⇒
  background = Color.WHITE
  opaque = true

  //val kit = new HTMLEditorKit
  // val doc = new HTMLDocument
  // editorKit = kit
  // peer.setDocument(doc)

  val proxyMapping = new HashMap[Int, IDataProxyUI]

  def inform(info: String,
             proxy: IDataProxyUI): Unit = {
    if (!text.contains(info)) {
      proxyMapping += caret.position -> proxy
      appendLink("[INFO] ")
      append(info)
    }
  }

  def inform(info: String) = {
    if (!text.contains(info)) {
      appendBold("[INFO]")
      append(info)
    }
  }

  def warn(warning: String,
           proxy: IDataProxyUI): Unit = {
    if (!text.contains(warning)) {
      proxyMapping += caret.position -> proxy
      appendLink("[WARNING] ")
      append(warning)
    }
  }

  def warn(warning: String) = {
    if (!text.contains(warning)) {
      appendBold("[WARNING] ")
      append(warning)
    }
  }

  def block(b: String,
            proxy: IDataProxyUI): Unit = {
    if (!text.contains(b)) {
      proxyMapping += caret.position -> proxy
      appendLink("[CRITICAL] ")
      append(b)
    }
  }

  def block(b: String) = {
    if (!text.contains(b)) {
      appendBold("[CRITICAL] ")
      append(b)
    }
  }

  def clear = {
    proxyMapping.clear
    text = ""
  }

  def isValid = text.isEmpty

  def appendBold(s: String) =
    //kit.insertHTML(doc, doc.getLength, s, 0, 0, HTML.Tag.STRONG)
    text += s

  def appendLink(s: String) =
    // kit.insertHTML(doc, doc.getLength, s, 0, 0, HTML.Tag.A)
    text += s

  def append(s: String) = {
    //kit.insertHTML(doc, doc.getLength, s, 0, 0, HTML.Tag.P)
    text += s + "\n"
  }

  peer.addHyperlinkListener(new MyHyperlinkListener)

  class MyHyperlinkListener extends HyperlinkListener {
    def hyperlinkUpdate(evt: HyperlinkEvent) = {
      if (evt.getEventType == HyperlinkEvent.EventType.ACTIVATED) {
        val proxy = proxyMapping.getOrElse(caret.position, None)
        proxy match {
          case Some(x: IDataProxyUI) ⇒
            ScenesManager.currentSceneContainer match {
              case Some(sc: ISceneContainer) ⇒ sc.scene.displayPropertyPanel(x, EDIT)
              case None ⇒
            }
        }
      }
    }

    class MyEditorPane(t: String,
                       oldEditor: Option[MyEditorPane] = None) extends EditorPane("text/html",
      if (oldEditor.isDefined) oldEditor.get text
      else "" + t) { editor ⇒
    }
  }
}
