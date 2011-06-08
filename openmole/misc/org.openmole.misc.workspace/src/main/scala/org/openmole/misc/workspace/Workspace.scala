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

import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.logging.Level
import java.util.logging.Logger
import org.apache.commons.configuration.FileConfiguration
import org.apache.commons.configuration.PropertiesConfiguration
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy
import org.jasypt.util.text.BasicTextEncryptor
import org.joda.time.format.ISOPeriodFormat
import scala.collection.mutable.HashMap
import org.openmole.misc.exception.InternalProcessingError
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.misc.logging.LoggerService

object Workspace {
  
  val OpenMoleDir = ".openmole"
  val ConfigurationFile = ".preferences"
  val GlobalGroup = "Global"
  val DefaultTmpLocation = ".tmp"
  val running = ".running"
  val UniqueID = new ConfigurationLocation(GlobalGroup, "UniqueID")

  private val group = "Workspace"
  private val fixedPrefix = "file"
  private val fixedPostfix = ".wf"
  private val fixedDir = "dir"
  
  private val passwordTest = new ConfigurationLocation(group, "passwordTest", true)
  private val LevelConfiguration = new ConfigurationLocation(group, "LoggingLevel")
  
  private val passwordTestString = "test"
  
  private val configurations = new HashMap[ConfigurationLocation, () => String]

  this += (UniqueID, UUID.randomUUID.toString)
  this += (passwordTest, passwordTestString)
  this += (LevelConfiguration, Level.INFO.toString)
  
  def isAlreadyRunningAt(location: File) = new File(location, running).exists
  
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
  
  def passwordIsCorrect = instance.passwordIsCorrect
  
  def passwordChoosen = instance.passwordChoosen

  def preferenceAsDurationInMs(location: ConfigurationLocation): Long = instance.preferenceAsDurationInMs(location)

  def preferenceAsDurationInS(location: ConfigurationLocation): Int = instance.preferenceAsDurationInS(location)

  def isPreferenceSet(location: ConfigurationLocation): Boolean = instance.isPreferenceSet(location)
}


class Workspace(val location: File) {

  import Workspace.{fixedPrefix, fixedPostfix, fixedDir, passwordTest, passwordTestString, running, DefaultTmpLocation, ConfigurationFile, configurations, LevelConfiguration}
  
  location.mkdirs
  val run = new File(location, running)
  run.createNewFile
  
  val tmpDir = new File(new File(location, DefaultTmpLocation), sessionUUID.toString)
  tmpDir.mkdirs
  
  @transient private var textEncryptor = new BasicTextEncryptor
  
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

  @transient lazy val sessionUUID = UUID.randomUUID
  
  LoggerService.setLevel(Level.parse(preference(LevelConfiguration)))
  
  def clean = {
    run.delete
    tmpDir.recursiveDelete
  }
  
  def newDir(prefix: String): File = {
    val tempFile = File.createTempFile(prefix, "", tmpDir)

    if (!tempFile.delete) throw new IOException    
    if (!tempFile.mkdir) throw new IOException

    tempFile.deleteOnExit
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

  def preference(location: ConfigurationLocation): String = synchronized {
    val conf = configuration.subset(location.group)
    val confVal = conf.getString(location.name)

    if (confVal == null) {
      configurations.get(location) match {
        case None => return null 
        case Some(value) =>
          val default = value()
          setPreference(location, default)
          return default
      }
    } 

    if (!location.cyphered) {
      return confVal
    } else {
      return textEncryptor.decrypt(confVal)
    }
  }

  def setPreference(location: ConfigurationLocation, value: String) = synchronized {
    val conf = configuration.subset(location.group)
    val prop = if(location.cyphered) textEncryptor.encrypt(value) else value
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

  def reset = {
    configurationFile.delete
  }

  def preferenceAsLong(location: ConfigurationLocation): Long = preference(location).toLong
  
  def password_=(password: String) = synchronized {
    textEncryptor = new BasicTextEncryptor
    textEncryptor.setPassword(password)
    passwordIsCorrect //set preference
  }
  
  def passwordIsCorrect = {
    try {
      preference(passwordTest)
      true
    } catch {
      case e => 
        Logger.getLogger(Workspace.getClass.getName).log(Level.FINE, "Password incorrect", e)
        false
    }
  }
  
  def passwordChoosen = {
    isPreferenceSet(passwordTest)
  }

  def preferenceAsDurationInMs(location: ConfigurationLocation): Long = {
    ISOPeriodFormat.standard.parsePeriod(preference(location)).toStandardSeconds.getSeconds * 1000L
  }

  def preferenceAsDurationInS(location: ConfigurationLocation): Int = {
    val formatter = ISOPeriodFormat.standard
    val period = formatter.parsePeriod(preference(location))
    return period.toStandardSeconds.getSeconds
  }

  def isPreferenceSet(location: ConfigurationLocation): Boolean = synchronized {
    val conf = configuration.subset(location.group)
    return (conf.getString(location.name) != null);
  }
}
