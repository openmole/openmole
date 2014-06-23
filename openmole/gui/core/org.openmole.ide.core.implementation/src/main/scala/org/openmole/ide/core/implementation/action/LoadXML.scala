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
import org.openmole.ide.core.implementation.execution.{ ScenesManager, Settings }
import org.openmole.ide.core.implementation.serializer.{ MoleData, GUISerializer }
import org.openmole.ide.core.implementation.dialog.{ StatusBar, DialogFactory }
import org.openmole.ide.core.implementation.dialog.StatusBar._
import scala.swing.FileChooser.Result._
import org.openmole.ide.core.implementation.dataproxy.Proxies
import org.openmole.ide.core.implementation.panel.ConceptMenu
import ConceptMenu._
import org.openmole.ide.core.implementation.prototype.GenericPrototypeDataUI
import org.openmole.ide.misc.tools.util.Types
import util.Success
import util.Failure
import scala.Some
import org.openmole.ide.core.implementation.preference.Preferences
import scala.swing.FileChooser

object LoadXML {

  def show(chooser: FileChooser) = {
    var text = ""
    if (chooser.showDialog(new Label, "OK") == Approve) text = chooser.selectedFile.getPath
    text
  }

  def importProject = DialogFactory.importProjectDialog

  def load: String = {
    val fileName = show(DialogFactory.fileChooser("OpenMOLE project loading",
      "*.om",
      "om",
      Settings.currentPath))
    if (!fileName.isEmpty) {
      tryFile(fileName).map {
        f ⇒ load(f, None)
      }.headOption.getOrElse("")
    }
    else ""
  }

  def tryFile(text: String) = _tryFile(text, List("", ".om", ".tar"))

  private def _tryFile(text: String, exts: List[String]): Option[File] = {
    if (!exts.isEmpty) {
      val fileName = text + exts.head
      val f = new File(fileName)
      if (f.isFile) Some(f)
      else _tryFile(text, exts.tail)
    }
    else None
  }

  def load(f: File, extraDir: Option[File] = None): String = {
    Settings.currentPath = Some(f.getParentFile)
    Settings.currentProject = Some(f)
    val seriliazer = new GUISerializer
    seriliazer.init(Proxies.instance)
    val deserialised = seriliazer.deserialize(f, extraDir)
    displayErrors(deserialised.written)
    val (proxies, scene) = deserialised.value
    StatusBar.clear
    Proxies.instance ++ proxies
    addPrototypes(proxies)
    addTasks(proxies)
    addSamplings(proxies)
    addEnvironments(proxies)
    addHooks(proxies)
    addSources(proxies)
    scene.foreach(mdu ⇒ ScenesManager().addBuildSceneContainer(MoleData.toScene(mdu, proxies)))
    Preferences.addRecentFiles(f.getAbsolutePath)
    f.getAbsolutePath
  }

  def addTasks(proxies: Proxies) =
    for {
      p ← proxies.tasks
      if (!p.generated)
    } +=(p)

  def addSamplings(proxies: Proxies) =
    for {
      p ← proxies.samplings
      if (!p.generated)
    } +=(p)

  def addEnvironments(proxies: Proxies) =
    for {
      p ← proxies.environments
      if (!p.generated)
    } +=(p)

  def addSampling(proxies: Proxies) =
    for {
      p ← proxies.sources
      if (!p.generated)
    } +=(p)

  def addSources(proxies: Proxies) =
    for {
      p ← proxies.sources
      if (!p.generated)
    } +=(p)

  def addHooks(proxies: Proxies) =
    for {
      p ← proxies.hooks
      if (!p.generated)
    } +=(p)

  def addPrototypes(proxies: Proxies) =
    for {
      p ← proxies.prototypes
    } {
      if (!p.generated) +=(p)
      if (!(GenericPrototypeDataUI.baseType ::: GenericPrototypeDataUI.extraType contains Types.standardize(p.dataUI.typeClassString))) {
        GenericPrototypeDataUI.extraType = GenericPrototypeDataUI.extraType :+ Types.standardize(p.dataUI.typeClassString)
      }
    }

}
