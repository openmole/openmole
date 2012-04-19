/*
 * Copyright (C) 2010 reuillon
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
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.misc.workspace

import com.thoughtworks.xstream.XStream
import java.io.File
import java.io.IOException
import java.util.{Random => JRandom}
import java.util.UUID
import java.util.logging.Level
import java.util.logging.Logger
import org.apache.commons.configuration.FileConfiguration
import org.apache.commons.configuration.PropertiesConfiguration
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy
import org.jasypt.util.text.BasicTextEncryptor
import org.joda.time.format.ISOPeriodFormat
import org.openmole.misc.tools.service.Random
import org.openmole.misc.tools.service.Random._
import scala.collection.mutable.HashMap
import org.openmole.misc.eventdispatcher.{EventDispatcher, Event}
import org.openmole.misc.exception.InternalProcessingError
import org.openmole.misc.exception.UserBadDataError
import org.openmole.misc.tools.io.FileUtil._

object Workspace {
  
  val noUniqueResourceProperty = "org.openmole.misc.workspace.noUniqueResource"
  
  case class PasswordRequired extends Event[Workspace]
  
  val sessionUUID = UUID.randomUUID
  val OpenMoleDir = ".openmole"
  val ConfigurationFile = "preferences"
  val GlobalGroup = "Global"
  val TmpLocation = ".tmp"
  val PersitentLocation = "persistent"
  val running = ".running"
  val UniqueID = new ConfigurationLocation(GlobalGroup, "UniqueID")
 
  private val group = "Workspace"
  private val fixedPrefix = "file"
  private val fixedPostfix = ".wf"
  private val fixedDir = "dir"
  
  private val passwordTest = new ConfigurationLocation(group, "passwordTest", true)
  private val passwordTestString = "test"
  
  private val configurations = new HashMap[ConfigurationLocation, () => String]

  this += (UniqueID, UUID.randomUUID.toString)
  this += (passwordTest, passwordTestString)
  
  def anotherIsRunningAt(location: File) = {
    val f = new File(location, running)
    f.contentOption match {
      case Some(uuid) =>
        !uuid.isEmpty && UUID.fromString(uuid).compareTo(sessionUUID) != 0
      case None => false
    }
  }
  
  lazy val defaultLocation = new File(System.getProperty("user.home"), OpenMoleDir)
  
  def instance = synchronized {
    if(_instance == null) _instance = new Workspace(defaultLocation)
    _instance
  }
  
  def instance_=(wp: Workspace) = {
    if(_instance != null) _instance.clean
    _instance = wp
  }
    
  private var _instance: Workspace = null
  
  Runtime.getRuntime.addShutdownHook(new Thread {
      override def run = synchronized {
        instance.clean
      }
    })
  
  def += (location: ConfigurationLocation, defaultValue: () => String) = synchronized {
    configurations(location) = defaultValue
  }

  def += (location: ConfigurationLocation,  defaultValue: String) = synchronized {
    configurations(location) = () => defaultValue
  }
  
  def location = instance.location
  
  def newDir(prefix: String): File = instance.newDir(prefix)
  
  def newFile(prefix: String, suffix: String): File =  instance.newFile(prefix, suffix)

  def defaultValue(location: ConfigurationLocation): String = instance.defaultValue(location)
  
  def rawPreference(location: ConfigurationLocation): String = instance.rawPreference(location)
  
  def preference(location: ConfigurationLocation): String = instance.preference(location)

  def setPreference(location: ConfigurationLocation, value: String) = instance.setPreference(location,value)

  def removePreference(location: ConfigurationLocation) = instance.removePreference(location)

  def file(name: String): File = instance.file(name)

  def preferenceAsInt(location: ConfigurationLocation): Int = instance.preferenceAsInt(location)
  
  def preferenceAsDouble(location: ConfigurationLocation): Double = instance.preferenceAsDouble(location)
  
  def newFile: File = instance.newFile
  
  def newDir: File = instance.newDir

  def reset = instance.reset

  def preferenceAsLong(location: ConfigurationLocation): Long = instance.preferenceAsLong(location)
  
  def password_=(password: String) = instance.password_=(password)
  
  def passwordIsCorrect(password: String) = instance.passwordIsCorrect(password)
  
  def passwordChoosen = instance.passwordChoosen

  def preferenceAsDurationInMs(location: ConfigurationLocation): Long = instance.preferenceAsDurationInMs(location)

  def preferenceAsDurationInS(location: ConfigurationLocation): Int = instance.preferenceAsDurationInS(location)

  def isPreferenceSet(location: ConfigurationLocation): Boolean = instance.isPreferenceSet(location)
  
  def persistentList[T](clazz: Class[T]) = instance.persistentList(clazz)
  
  def decrypt(s: String) = instance.decrypt(s)
  
  def encrypt(s: String) = instance.encrypt(s)
  
  def rng = instance.rng
  
  /*def newRNG(seed: Long) = instance.newRNG(seed)
  
  def newRNG = instance.newRNG*/
  
  def newSeed = instance.newSeed
}


class Workspace(val location: File) {

  import Workspace.{PersitentLocation,fixedPrefix, fixedPostfix, fixedDir, passwordTest, passwordTestString, running, TmpLocation, ConfigurationFile, configurations, sessionUUID, UniqueID, noUniqueResourceProperty}
  
  location.mkdirs
  val run = new File(location, running)
  
  {
    val noURProp = System.getProperty(noUniqueResourceProperty)
    val noUR = noURProp != null && noURProp.equalsIgnoreCase("true")
    if(!run.exists && !noUR) {
      run.createNewFile
      run.content = sessionUUID.toString
    }
  }
  
  @transient val tmpDir = new File(new File(location, TmpLocation), sessionUUID.toString)
  tmpDir.mkdirs
  
  @transient val persistentDir = new File(location, PersitentLocation)
  persistentDir.mkdirs
  
  def newSeed = rng.nextLong

  val rng = Random.buildSynchronized(sessionUUID)
  
  private def textEncryptor(password: Option[String]) = {
    password match {
      case Some(password) =>
        val textEncryptor = new BasicTextEncryptor
        textEncryptor.setPassword(password)
        textEncryptor
      case None => throw new UserBadDataError("Password is not set.")
    }
  }
  
  @transient private var _password: Option[String] = None
  
  @transient private lazy val configurationFile: File = {
    val file = new File(location, ConfigurationFile)
    file.createNewFile
    file
  }
  
  @transient private lazy val configuration: FileConfiguration = synchronized {
    val configuration = new PropertiesConfiguration(configurationFile)
    configuration.setReloadingStrategy(new FileChangedReloadingStrategy)
    configuration
  }
    
  def clean = {
    run.contentOption match {
      case Some(uuid) =>
        if(!uuid.isEmpty && UUID.fromString(uuid).compareTo(sessionUUID) == 0) run.delete
      case None =>
    }
    
    tmpDir.recursiveDelete
  }
  
  def newDir(prefix: String): File = {
    val tempFile = File.createTempFile(prefix, "", tmpDir)

    if (!tempFile.delete) throw new IOException    
    if (!tempFile.mkdir) throw new IOException

    //tempFile.deleteOnExit
    tempFile
  }
  
  def newFile(prefix: String, suffix: String): File =  File.createTempFile(prefix, suffix, tmpDir)

  def defaultValue(location: ConfigurationLocation): String = {
    configurations.get(location) match {
      case None => ""
      case Some(cf) => 
        val ret = cf()
        if (ret == null) ""
        else ret
    }
  }

  
  def rawPreference(location: ConfigurationLocation): String = synchronized {
    val conf = configuration.subset(location.group)
    conf.getString(location.name)
  }
  
  def preference(location: ConfigurationLocation): String = synchronized {
    if(!isPreferenceSet(location)) setToDefaultValue(location)
    val confVal = rawPreference(location)

    if (!location.cyphered) confVal
    else decrypt(confVal)
  }
  
  def setToDefaultValue(location: ConfigurationLocation) = synchronized {
    configurations.get(location) match {
      case None => null 
      case Some(value) =>
        val default = value()
        setPreference(location, default)
        default
    }
  }

  def setPreference(location: ConfigurationLocation, value: String) = synchronized {
    val conf = configuration.subset(location.group)
    val prop = if(location.cyphered) encrypt(value) else value
    conf.setProperty(location.name, prop)
    configuration.save
  }

  def removePreference(location: ConfigurationLocation) = synchronized {
    val conf = configuration.subset(location.group)
    conf.clearProperty(location.name)
  }

  def file(name: String): File = new File(location, name)

  def preferenceAsInt(location: ConfigurationLocation): Int = preference(location).toInt
  
  def preferenceAsDouble(location: ConfigurationLocation): Double = preference(location).toDouble
  
  def newFile: File = newFile(fixedPrefix, fixedPostfix)
  
  def newDir: File = newDir(fixedDir)

  def reset {
    configurationFile.delete
  }

  def preferenceAsLong(location: ConfigurationLocation): Long = preference(location).toLong
  
  def password_=(password: String): Unit = synchronized {
    if(!passwordIsCorrect(password)) throw new UserBadDataError("Password is incorrect.") 
    this._password = Some(password)
    if(!isPreferenceSet(passwordTest)) setToDefaultValue(passwordTest)
  }

  def decrypt(s: String) = {
    _password match {
      case None => EventDispatcher.trigger(this, new Workspace.PasswordRequired)
      case Some(p) =>
    } 
    textEncryptor(_password).decrypt(s)
  }
  
  def encrypt(s: String) = {
    _password match {
      case None => EventDispatcher.trigger(this, new Workspace.PasswordRequired)
      case Some(p) =>
    } 
    textEncryptor(_password).encrypt(s)
  }
  
  def passwordIsCorrect(password: String) = {
    try {
      if(isPreferenceSet(passwordTest)) {
        val te = textEncryptor(Some(password))
        te.decrypt(rawPreference(passwordTest))
        true
      } else true
    } catch {
      case e => 
        Logger.getLogger(Workspace.getClass.getName).log(Level.FINE, "Password incorrect", e)
        false
    }
  }
  
  def passwordChoosen = isPreferenceSet(passwordTest)

  def preferenceAsDurationInMs(location: ConfigurationLocation): Long = 
    ISOPeriodFormat.standard.parsePeriod(preference(location)).toStandardSeconds.getSeconds * 1000L

  def preferenceAsDurationInS(location: ConfigurationLocation): Int = {
    val formatter = ISOPeriodFormat.standard
    val period = formatter.parsePeriod(preference(location))
    period.toStandardSeconds.getSeconds
  }
  
  def isPreferenceSet(location: ConfigurationLocation): Boolean = synchronized {
    rawPreference(location) != null
  }
  
  def persistentList[T](clazz: Class[T]) = new PersistentList[T](
    {
      val xstream = new XStream
      xstream.setClassLoader(clazz.getClassLoader)
      xstream
    }, {
      val f = new File(persistentDir, clazz.getName)
      f.mkdirs
      f
    })
  
  object PersistentList {
    val pattern = "[0-9]+"
    def wsync[T] = Workspace.this.synchronized[T] _
  }

  class PersistentList[T](serializer: XStream, dir: File) extends Iterable[(Int, T)] {
  
    import PersistentList._
    
    def file(i: Int) = new File(dir, i.toString)
  
    def -= (i: Int) = wsync { file(i).delete }
  
    def apply(i: Int): T = wsync { serializer.fromXML(file(i).content).asInstanceOf[T] }
  
    def update(i: Int, obj: T) = wsync { file(i).content = serializer.toXML(obj) }
  
    override def iterator = wsync {
      dir.listFiles{f: File => f.getName.matches(pattern)}.map{_.getName.toInt}.sorted.map{i => i -> apply(i)}.iterator
    }
  }
  
  
}
