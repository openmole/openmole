/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
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
package org.openmole.ide.core.implementation.panel

import org.openmole.ide.core.implementation.dataproxy.{ PrototypeDataProxyUI, Proxies, DataProxyUI }
import org.openmole.ide.misc.tools.image.Images._
import org.openmole.ide.misc.widget.multirow.RowWidget.SMALL
import org.openmole.ide.misc.widget.multirow.MultiWidget.CLOSE_IF_EMPTY
import org.openmole.ide.misc.widget.ContentAction
import org.openmole.ide.core.implementation.execution.ScenesManager
import org.openmole.ide.core.implementation.workflow.ISceneContainer
import org.openmole.ide.misc.widget.multirow.{ MultiComboLinkLabelGroovyTextFieldEditor, MultiComboLinkLabel }
import org.openmole.ide.misc.widget.multirow.MultiComboLinkLabel.{ ComboLinkLabelData, ComboLinkLabelPanel }
import org.openmole.ide.misc.widget.multirow.MultiComboLinkLabelGroovyTextFieldEditor.{ ComboLinkLabelGroovyTextFieldEditorData, ComboLinkLabelGroovyTextFieldEditorPanel }

object MultiProxies {

  def comboLink[T <: DataProxyUI](fullList: Seq[T], data: Seq[T]) = {
    val contentActions = fullList.map {
      p ⇒ (p, contentAction(p))
    }.toList

    new MultiComboLinkLabel("", contentActions, data.map {
      p ⇒ new ComboLinkLabelPanel(contentActions, EYE, new ComboLinkLabelData(Some(p)))
    }, EYE, CLOSE_IF_EMPTY, insets = SMALL)
  }

  def comboLinkGroovyEditor[T <: PrototypeDataProxyUI](fullList: Seq[T], data: Seq[T], inputParameters: Map[T, String]) = {
    val contentActions = fullList.map {
      p ⇒ (p, p.dataUI.coreObject.get, contentAction(p))
    }.toList

    new MultiComboLinkLabelGroovyTextFieldEditor("", contentActions,
      data.map {
        d ⇒
          new ComboLinkLabelGroovyTextFieldEditorPanel(contentActions, EYE,
            new ComboLinkLabelGroovyTextFieldEditorData(d.dataUI.coreObject.get, Some(d), inputParameters.getOrElse(d, "")))
      }, EYE, CLOSE_IF_EMPTY, insets = SMALL)
  }

  def contentAction[T <: DataProxyUI](proto: T) = new ContentAction(proto.dataUI.toString, proto) {
    override def apply =
      ScenesManager.currentSceneContainer match {
        case Some(x: ISceneContainer) ⇒ x.scene.displayPropertyPanel(proto)
        case None                     ⇒
      }
  }
}