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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
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
import org.jasypt.exceptions.EncryptionOperationNotPossibleException
import org.jasypt.util.text.BasicTextEncryptor
import org.joda.time.format.ISOPeriodFormat
import org.openmole.commons.exception.InternalProcessingError
import org.openmole.commons.exception.UserBadDataError
import org.openmole.commons.aspect.caching.Cachable;
import org.openmole.commons.aspect.caching.ChangeState;
import scala.collection.mutable.HashMap
import org.openmole.commons.tools.io.FileUtil

object Workspace {
  val OpenMoleDir = ".openmole"
  val ConfigurationFile = ".preferences"
  val GlobalGroup = "Global"
  val DefaultTmpLocation = ".tmp"
  val running = ".running"
  val UniqueID = new ConfigurationLocation(GlobalGroup, "UniqueID")

  lazy val defaultLocation = new File(System.getProperty("user.home"), OpenMoleDir)
  def isAlreadyRunningAt(location: File) = new File(location, running).exists

  private val group = "Workspace"
  private val fixedPrefix = "file"
  private val fixedPostfix = ".wf"
  private val fixedDir = "dir"
  private val passwordTest = new ConfigurationLocation(group, "passwordTest", true)
  private val passwordTestString = "test"

  private var _location: File = null
  @transient private var textEncryptor = new BasicTextEncryptor
  private val configurations = new HashMap[ConfigurationLocation, () => String]
    
  //@transient private var _password = ""

  this += (UniqueID, UUID.randomUUID.toString)
  this += (passwordTest, passwordTestString)
  
  @ChangeState
  def location_= (location: File) = {
    new File(locationInternal, running).delete
    _location = location
    val run = new File(_location, running)
    run.createNewFile
    run.deleteOnExit
  }
  
  def locationInternal = {
    if(_location == null) defaultLocation
    else _location
  }
  
  def location: File = {
    val ret = locationInternal
    if (!ret.exists) ret.mkdirs
    ret
  }

  def += (location: ConfigurationLocation, defaultValue: () => String) = synchronized {
    configurations(location) = defaultValue
  }

  def += (location: ConfigurationLocation,  defaultValue: String) = synchronized {
    configurations(location) = () => defaultValue
  }

  @transient lazy val sessionUUID = UUID.randomUUID
  
  
  
  @Cachable
  def tmpDir: File = {
    val tmpLocation = new File(new File(location, DefaultTmpLocation), sessionUUID.toString)

    if (!tmpLocation.mkdirs) {
      throw new InternalProcessingError("Cannot create tmp dir " + tmpLocation.getAbsolutePath)
    }
    
    Runtime.getRuntime.addShutdownHook(new Thread {
        override def run = FileUtil.recursiveDelete(tmpLocation)
      })
    
    tmpLocation
  }

  def newDir(prefix: String): File = {
    val tempFile = File.createTempFile(prefix, "", tmpDir)

    if (!tempFile.delete) throw new IOException    
    if (!tempFile.mkdir) throw new IOException

    tempFile.deleteOnExit
    tempFile
  }
  
  def newFile(prefix: String, suffix: String): File =  File.createTempFile(prefix, suffix, tmpDir)


  @Cachable
  private def configurationFile: File = {
    val file = new File(location, ConfigurationFile)
    file.createNewFile
    file
  }

  @Cachable
  private def configuration: FileConfiguration = synchronized {
    val configuration = new PropertiesConfiguration(configurationFile)
    configuration.setReloadingStrategy(new FileChangedReloadingStrategy)
    return configuration
  }

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

  /*@Cachable
   private def textEncryptor: BasicTextEncryptor = synchronized {
   val tmpTextEncryptor = new BasicTextEncryptor
   tmpTextEncryptor.setPassword(_password)
   tmpTextEncryptor
   }*/

  def removePreference(location: ConfigurationLocation) = synchronized {
    val conf = configuration.subset(location.group)
    conf.clearProperty(location.name)
  }

  def file(name: String): File = new File(location, name)

  def preferenceAsInt(location: ConfigurationLocation): Int = preference(location).toInt
  
  def preferenceAsDouble(location: ConfigurationLocation): Double = preference(location).toDouble
  
  def newFile: File = newFile(fixedPrefix, fixedPostfix)
  
  def newDir: File = newDir(fixedDir)

  @ChangeState
  def reset = configurationFile.delete

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
