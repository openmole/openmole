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

package org.openmole.ide.core.implementation.action

import java.io.File
import scala.swing.Label
import scala.swing.FileChooser.SelectionMode._
import org.openmole.ide.core.implementation.execution.{ ScenesManager, Settings }
import org.openmole.ide.core.implementation.serializer.{ MoleData, GUISerializer }
import org.openmole.ide.core.implementation.dialog.{ StatusBar, DialogFactory }
import org.openmole.ide.core.implementation.dialog.StatusBar._
import scala.swing.FileChooser.Result._
import util.{ Success, Failure }
import org.openmole.ide.core.implementation.dataproxy.Proxies
import org.openmole.ide.core.implementation.workflow._
import org.openmole.misc.exception.ExceptionUtils
import org.openmole.ide.core.implementation.panel.ConceptMenu
import org.openmole.ide.core.implementation.prototype.GenericPrototypeDataUI
import org.openmole.ide.misc.tools.util.Types
import org.openmole.ide.core.implementation.builder.SceneFactory
import java.awt.Point
import util.Success
import util.Failure
import scala.Some

object LoadXML {

  def show = {
    val fc = DialogFactory.fileChooser(" OpenMOLE project loading",
      "*.om",
      "om")
    var text = ""
    if (fc.showDialog(new Label, "OK") == Approve) text = fc.selectedFile.getPath
    val file = new File(text)
    if (file.isFile) {
      Settings.currentProject = Some(file)
      (new GUISerializer).deserialize(text) match {
        case Failure(t) ⇒ displayErrors(List(t))
        case Success((proxies, scene)) ⇒
          StatusBar().clear
          ScenesManager.closeAll
          Proxies.instance = proxies
          addPrototypes(proxies)
          addTasks(proxies)
          addSamplings(proxies)
          addEnvironments(proxies)
          addHooks(proxies)
          addSources(proxies)
          scene.foreach(mdu ⇒ ScenesManager.addBuildSceneContainer(MoleData.toScene(mdu, proxies)))
      }
    }
    text
  }

  def addTasks(proxies: Proxies) =
    for {
      p ← proxies.tasks
      if (!p.generated)
    } ConceptMenu.taskMenu.popup.contents += ConceptMenu.addItem(p)

  def addSamplings(proxies: Proxies) =
    for {
      p ← proxies.samplings
      if (!p.generated)
    } ConceptMenu.samplingMenu.popup.contents += ConceptMenu.addItem(p)

  def addEnvironments(proxies: Proxies) =
    for {
      p ← proxies.environments
      if (!p.generated)
    } ConceptMenu.environmentMenu.popup.contents += ConceptMenu.addItem(p)

  def addSampling(proxies: Proxies) =
    for {
      p ← proxies.sources
      if (!p.generated)
    } ConceptMenu.sourceMenu.popup.contents += ConceptMenu.addItem(p)

  def addSources(proxies: Proxies) =
    for {
      p ← proxies.sources
      if (!p.generated)
    } ConceptMenu.sourceMenu.popup.contents += ConceptMenu.addItem(p)

  def addHooks(proxies: Proxies) =
    for {
      p ← proxies.hooks
      if (!p.generated)
    } ConceptMenu.hookMenu.popup.contents += ConceptMenu.addItem(p)

  def addPrototypes(proxies: Proxies) =
    for {
      p ← proxies.prototypes
    } {
      if (!p.generated) ConceptMenu.prototypeMenu.popup.contents += ConceptMenu.addItem(p)
      if (!(GenericPrototypeDataUI.baseType ::: GenericPrototypeDataUI.extraType contains Types.standardize(p.dataUI.typeClassString))) {
        GenericPrototypeDataUI.extraType = GenericPrototypeDataUI.extraType :+ Types.standardize(p.dataUI.typeClassString)
      }
    }

}
