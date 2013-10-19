/*
 * Copyright (C) 2010 Romain Reuillon
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
import java.util.{ Random ⇒ JRandom }
import java.util.UUID
import java.util.logging.Level
import java.util.logging.Logger
import org.apache.commons.configuration._
import org.apache.commons.configuration.reloading._
import org.jasypt.util.text._
import org.openmole.misc.tools.service.Random
import org.openmole.misc.tools.service.Random._
import scala.collection.mutable.HashMap
import org.openmole.misc.eventdispatcher.{ EventDispatcher, Event }
import org.openmole.misc.exception._
import org.openmole.misc.replication._
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.misc.tools.service.Duration._
import scala.util.Try
import org.openmole.misc.osgi

object Workspace {

  case object PasswordRequired extends Event[Workspace]

  val sessionUUID = UUID.randomUUID

  val preferences = "preferences"
  val globalGroup = "Global"
  val tmpLocation = ".tmp"
  val persistentLocation = "persistent"
  val authenticationsLocation = "authentications"
  val pluginLocation = "plugins"
  val uniqueID = new ConfigurationLocation(globalGroup, "UniqueID")

  private val group = "Workspace"
  private val fixedPrefix = "file"
  private val fixedPostfix = ".bin"
  private val fixedDir = "category"

  private val passwordTest = new ConfigurationLocation(group, "passwordTest", true)
  private val passwordTestString = "test"

  private val configurations = new HashMap[ConfigurationLocation, () ⇒ String]

  val ErrorArraySnipSize = new ConfigurationLocation(group, "ErrorArraySnipSize")

  this += (uniqueID, UUID.randomUUID.toString)
  this += (passwordTest, passwordTestString)
  this += (ErrorArraySnipSize, "10")

  lazy val defaultLocation = DBServerInfo.base //new File(System.getProperty("user.home"), openMoleDir)
  lazy val instance = new Workspace(defaultLocation)

  Runtime.getRuntime.addShutdownHook(new Thread {
    override def run = synchronized {
      instance.clean
    }
  })

  //implicit def objectToInstance(ws: this.type) = instance

  def +=(location: ConfigurationLocation, defaultValue: () ⇒ String) = synchronized {
    configurations(location) = defaultValue
  }

  def +=(location: ConfigurationLocation, defaultValue: String) = synchronized {
    configurations(location) = () ⇒ defaultValue
  }

  def pluginDirLocation = instance.pluginDir

  def location = instance.location

  def tmpDir = instance.tmpDir

  def newDir(prefix: String): File = instance.newDir(prefix)

  def newFile(prefix: String, suffix: String): File = instance.newFile(prefix, suffix)

  def defaultValue(location: ConfigurationLocation): String = instance.defaultValue(location)

  def rawPreference(location: ConfigurationLocation): String = instance.rawPreference(location)

  def preference(location: ConfigurationLocation): String = instance.preference(location)

  def setPreference(location: ConfigurationLocation, value: String) = instance.setPreference(location, value)

  def removePreference(location: ConfigurationLocation) = instance.removePreference(location)

  def file(name: String): File = instance.file(name)

  def preferenceAsInt(location: ConfigurationLocation): Int = instance.preferenceAsInt(location)

  def preferenceAsDouble(location: ConfigurationLocation): Double = instance.preferenceAsDouble(location)

  def withTmpFile[T](f: File ⇒ T): T = instance.withTmpFile(f)

  def newFile: File = instance.newFile

  def newDir: File = instance.newDir

  def reset = instance.reset

  def preferenceAsLong(location: ConfigurationLocation): Long = instance.preferenceAsLong(location)

  def setPassword(password: String) = instance.password_=(password)

  def passwordIsCorrect(password: String) = instance.passwordIsCorrect(password)

  def passwordChosen = instance.passwordChosen

  def preferenceAsDuration(location: ConfigurationLocation) = instance.preferenceAsDuration(location)

  def encrypt(s: String) = instance.encrypt(s)

  def persistent(name: String) = instance.persistent(name)

  implicit def authenticationProvider = instance.authenticationProvider

  def authentications = instance.authentications

  def rng = instance.rng

  def newSeed = instance.newSeed

  object NoneTextEncryptor extends TextEncryptor {
    def decrypt(encryptedMessage: String) = encryptedMessage
    def encrypt(message: String) = message
  }

  def textEncryptor(password: String) =
    if (!password.trim.isEmpty) {
      val textEncryptor = new BasicTextEncryptor
      textEncryptor.setPassword(password)
      textEncryptor
    }
    else NoneTextEncryptor

  def decrypt(s: String, password: String = instance.password) =
    if (s.isEmpty) s
    else Workspace.textEncryptor(password).decrypt(s)

  def openMOLELocation = osgi.openMOLELocation
}

class Workspace(val location: File) {

  import Workspace.{ textEncryptor, decrypt, NoneTextEncryptor, persistentLocation, authenticationsLocation, pluginLocation, fixedPrefix, fixedPostfix, fixedDir, passwordTest, passwordTestString, tmpLocation, preferences, configurations, sessionUUID, uniqueID }

  location.mkdirs

  val tmpDir = new File(new File(location, tmpLocation), sessionUUID.toString)
  tmpDir.mkdirs

  val pluginDir = new File(location, pluginLocation)
  pluginDir.mkdirs

  val persistentDir = new File(location, persistentLocation)
  persistentDir.mkdirs

  def newSeed = rng.nextLong

  val rng = Random.newRNG(sessionUUID)

  @transient private var _password: Option[String] = None

  @transient private lazy val configurationFile: File = {
    val file = new File(location, preferences)
    file.createNewFile
    file
  }

  @transient private lazy val configuration: FileConfiguration = synchronized {
    val configuration = new PropertiesConfiguration(configurationFile)
    configuration.setReloadingStrategy(new FileChangedReloadingStrategy)
    configuration
  }

  def clean = tmpDir.recursiveDelete

  def newDir(prefix: String): File = tmpDir.newDir(prefix)

  def newFile(prefix: String, suffix: String): File = tmpDir.newFile(prefix, suffix)

  def defaultValue(location: ConfigurationLocation): String = {
    configurations.get(location) match {
      case None ⇒ ""
      case Some(cf) ⇒
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
    if (!isPreferenceSet(location)) setToDefaultValue(location)
    val confVal = rawPreference(location)

    if (!location.cyphered) confVal
    else decrypt(confVal, password)
  }

  def setToDefaultValue(location: ConfigurationLocation) = synchronized {
    configurations.get(location) match {
      case None ⇒ null
      case Some(value) ⇒
        val default = value()
        setPreference(location, default)
        default
    }
  }

  def setPreference(location: ConfigurationLocation, value: String) = synchronized {
    val conf = configuration.subset(location.group)
    val prop = if (location.cyphered) encrypt(value) else value
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

  def withTmpFile[T](f: File ⇒ T): T = {
    val file = newFile
    try f(file)
    finally file.delete
  }

  def newFile: File = newFile(fixedPrefix, fixedPostfix)

  def newDir: File = newDir(fixedDir)

  def reset = synchronized {
    persistentDir.recursiveDelete
    val uniqueId = preference(uniqueID)
    configurationFile.content = ""
    configuration.clear
    _password = None
    setPreference(uniqueID, uniqueId)
  }

  def preferenceAsLong(location: ConfigurationLocation): Long = preference(location).toLong

  def password_=(password: String): Unit = synchronized {
    if (!passwordIsCorrect(password)) throw new UserBadDataError("Password is incorrect.")
    this._password = Some(password)
    if (!isPreferenceSet(passwordTest)) setToDefaultValue(passwordTest)
  }

  private def password = {
    _password match {
      case None    ⇒ EventDispatcher.trigger(this, Workspace.PasswordRequired)
      case Some(p) ⇒
    }
    _password.getOrElse(throw new UserBadDataError("Password is not set."))
  }

  def encrypt(s: String) = textEncryptor(password).encrypt(s)

  def passwordIsCorrect(password: String) = {
    try {
      if (isPreferenceSet(passwordTest)) {
        val te = textEncryptor(password)
        te.decrypt(rawPreference(passwordTest))
        true
      }
      else true
    }
    catch {
      case e: Throwable ⇒
        Logger.getLogger(Workspace.getClass.getName).log(Level.FINE, "Password incorrect", e)
        false
    }
  }

  def passwordChosen = isPreferenceSet(passwordTest)

  def preferenceAsDuration(location: ConfigurationLocation) = new DurationStringDecorator(preference(location))

  def isPreferenceSet(location: ConfigurationLocation): Boolean = synchronized {
    rawPreference(location) != null
  }

  def persistent(name: String) = Persistent(persistentDir.child(name))

  def authenticationProvider =
    AuthenticationProvider(
      authentications.allByCategory,
      password
    )

  lazy val authentications = new Persistent(persistentDir.child(Workspace.authenticationsLocation)) with Authentication

}
