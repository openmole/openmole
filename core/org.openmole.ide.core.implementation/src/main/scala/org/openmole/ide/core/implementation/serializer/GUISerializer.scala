/*
 * Copyright (C) 2011 Mathieu Mathieu Leclaire <mathieu.Mathieu Leclaire at openmole.org>
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

package org.openmole.ide.core.implementation.serializer

import com.ice.tar.TarInputStream
import com.ice.tar.TarOutputStream
import com.thoughtworks.xstream.XStream
import java.io.EOFException
import com.thoughtworks.xstream.io.xml.DomDriver
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileReader
import java.io.FileWriter
import org.openmole.ide.core.implementation.dialog.StatusBar
import org.openmole.ide.core.implementation.execution.ScenesManager
import org.openmole.ide.core.model.data.IHookDataUI
import org.openmole.ide.core.model.dataproxy._
import org.openmole.ide.core.implementation.dataproxy._
import java.io.ObjectInputStream
import java.nio.file.Files
import org.openmole.ide.core.implementation.data._
import org.openmole.ide.core.implementation.panel.ConceptMenu
import org.openmole.ide.core.implementation.workflow.BuildMoleScene
import org.openmole.ide.core.implementation.workflow.MoleScene
import org.openmole.misc.tools.io.FileUtil._
import scala.collection.JavaConversions._
import org.openmole.misc.tools.io.TarArchiver._
import scala.collection.mutable.HashSet
import scala.collection.JavaConversions._

class GUISerializer {

  val xstream = new XStream(new DomDriver)
  val prototypeConverter = new PrototypeConverter(xstream.getMapper, xstream.getReflectionProvider)
  val samplingConverter = new SamplingCompositionConverter(xstream.getMapper, xstream.getReflectionProvider, prototypeConverter)
  val environmentConverter = new EnvironmentConverter(xstream.getMapper, xstream.getReflectionProvider)

  xstream.registerConverter(new MoleSceneConverter(this))
  xstream.registerConverter(prototypeConverter)
  xstream.registerConverter(samplingConverter)
  xstream.registerConverter(environmentConverter)

  xstream.alias("molescene", classOf[MoleScene])
  xstream.alias("sampling", classOf[ISamplingCompositionDataProxyUI])
  xstream.alias("prototype", classOf[IPrototypeDataProxyUI])
  xstream.alias("environment", classOf[IEnvironmentDataProxyUI])

  var hookList = new HashSet[IHookDataUI]

  def serializeConcept(prefix: String,
                       concept: String,
                       set: List[(_, Int)]) = {
    val conceptFile = new File(prefix + "/" + concept)
    conceptFile.mkdirs
    set.foreach {
      case (s, id) ⇒
        val f = new File(conceptFile.getCanonicalFile + "/" + id + ".xml")
        val writer = new FileWriter(f)
        val out = xstream.createObjectOutputStream(writer, concept)
        out.writeObject(s)
        out.close
    }
  }

  def serialize(toFile: String) = {
    val path = new File(toFile)
    val prefix = path.getParentFile + "/" + path.getName.split('.')(0)
    if (path.getParentFile.isDirectory) {
      serializeConcept(prefix, "task", Proxys.tasks.map { s ⇒ s -> s.id }.toList)
      serializeConcept(prefix,
        "sampling",
        Proxys.samplings.filterNot(s ⇒ samplingConverter.added.contains(s.id)).map { s ⇒ s -> s.id }.toList)
      serializeConcept(prefix,
        "prototype",
        Proxys.prototypes.filterNot(s ⇒ prototypeConverter.added.contains(s.id)).map { s ⇒ s -> s.id }.toList)
      serializeConcept(prefix,
        "environment",
        Proxys.environments.filterNot(s ⇒ environmentConverter.added.contains(s.id)).map { s ⇒ s -> s.id }.toList)
      serializeConcept(prefix, "hook", ScenesManager.moleScenes.flatMap {
        _.manager.capsules.values.flatMap {
          _.dataUI.hooks.values
        }
      }.map { h ⇒ h -> h.id }.toList)
      serializeConcept(prefix, "mole", ScenesManager.moleScenes.map { ms ⇒ ms -> ms.manager.id }.toList)
      val os = new TarOutputStream(new FileOutputStream(path))
      try os.createDirArchiveWithRelativePathNoVariableContent(new File(prefix))
      finally os.close
      new File(prefix).recursiveDelete
    }
  }

  def readStream(f: File) = try {
    Right(xstream.createObjectInputStream(new FileReader(f)))
  } catch {
    case e: Throwable ⇒
      StatusBar.block("An error occured when loading " + f.getAbsolutePath + "\n" + e.getMessage,
        stack = e.getStackTraceString,
        exceptionName = e.getClass.getCanonicalName)
      Left
  }

  def addTask(t: ITaskDataProxyUI) =
    if (!Proxys.tasks.contains(t)) {
      Proxys.tasks += t
      ConceptMenu.taskMenu.popup.contents += ConceptMenu.addItem(t)
    }

  def unserializeProxy(prefixFile: String,
                       concept: String) = {
    new File(prefixFile + "/" + concept).listFiles.toList.foreach { f ⇒

      readStream(f) match {
        case Right(x: ObjectInputStream) ⇒
          try {
            val readObject = x.readObject
            readObject match {
              case t: ITaskDataProxyUI ⇒ addTask(t)
              case p: IPrototypeDataProxyUI ⇒ prototypeConverter.addPrototype(p)
              case s: ISamplingCompositionDataProxyUI ⇒ samplingConverter.addSampling(s)
              case e: IEnvironmentDataProxyUI ⇒ environmentConverter.addEnvironment(e)
              case ms: BuildMoleScene ⇒ ScenesManager.addBuildSceneContainer(ms)
              case _ ⇒
                StatusBar.block("Failed to unserialize the " + concept + " " + readObject.toString)
            }
          } catch {
            case eof: EOFException ⇒ StatusBar.inform("Project loaded")
            case e: Throwable ⇒ StatusBar.block("Failed to unserialize a data of type " + concept,
              stack = e.getMessage + "\n" + e.getStackTraceString,
              exceptionName = e.getClass.getCanonicalName)
          } finally {
            x.close
          }
        case Left ⇒
      }
    }
  }

  def unserializeHook(prefixFile: String) = {
    hookList.clear
    new File(prefixFile + "/hook").listFiles.toList.flatMap { f ⇒
      readStream(f) match {
        case Right(x: ObjectInputStream) ⇒
          try {
            val readObject = x.readObject
            readObject match {
              case h: IHookDataUI ⇒ Some(h)
              case _ ⇒ Nil
            }
          }
      }
    }.foreach { hookList += }
  }

  def unserialize(fromFile: String) = {
    StatusBar.clear
    Proxys.clearAll
    ScenesManager.closeAll

    val os = new TarInputStream(new FileInputStream(fromFile))
    val extractDir = Files.createTempDirectory("openmole").toFile
    os.extractDirArchiveWithRelativePathAndClose(extractDir)
    unserializeProxy(extractDir.getAbsolutePath, "sampling")
    unserializeProxy(extractDir.getAbsolutePath, "environment")
    unserializeHook(extractDir.getAbsolutePath)
    unserializeProxy(extractDir.getAbsolutePath, "prototype")
    unserializeProxy(extractDir.getAbsolutePath, "task")
    unserializeProxy(extractDir.getAbsolutePath, "mole")
  }
}