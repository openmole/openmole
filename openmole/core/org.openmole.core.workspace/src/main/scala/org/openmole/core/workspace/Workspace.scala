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

package org.openmole.core.workspace

import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import java.util.logging.Level
import java.util.logging.Logger
import org.apache.commons.configuration._
import org.apache.commons.configuration.reloading._
import org.jasypt.util.text._
import org.openmole.core.event.{ Event, EventDispatcher }
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.replication.DBServerInfo
import org.openmole.tool.crypto.Certificate
import org.openmole.tool.file._
import org.openmole.core.tools.service._
import Random._
import scala.collection.mutable.HashMap
import scala.concurrent.duration.FiniteDuration
import scala.util.Try

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
  val fixedPrefix = "file"
  val fixedPostfix = ".bin"
  val fixedDir = "dir"

  private val passwordTest = new ConfigurationLocation(group, "passwordTest", true)
  private val passwordTestString = "test"

  private val configurations = new HashMap[ConfigurationLocation, () ⇒ String]

  val ErrorArraySnipSize = new ConfigurationLocation(group, "ErrorArraySnipSize")

  this += (uniqueID, UUID.randomUUID.toString)
  this += (passwordTest, passwordTestString)
  this += (ErrorArraySnipSize, "10")

  lazy val defaultLocation = DBServerInfo.base

  Runtime.getRuntime.addShutdownHook(new Thread {
    override def run = synchronized {
      instance.clean
    }
  })

  def +=(location: ConfigurationLocation, defaultValue: () ⇒ String) = synchronized {
    configurations(location) = defaultValue
  }

  def +=(location: ConfigurationLocation, defaultValue: String) = synchronized {
    configurations(location) = () ⇒ defaultValue
  }

  def allTmpDir(location: File) = new File(location, tmpLocation)
  def lock(location: File) = new File(allTmpDir(location), s"${sessionUUID.toString}-lock")

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

  val OpenMOLELocationProperty = "openmole.location"

  def openMOLELocation =
    Option(System.getProperty(OpenMOLELocationProperty, null)).map(new File(_))

  val instance = new Workspace(defaultLocation)

  implicit def authenticationProvider = instance.authenticationProvider

}

class Workspace(val location: File) {

  import Workspace._ //{ textEncryptor, decrypt, NoneTextEncryptor, persistentLocation, authenticationsLocation, pluginLocation, fixedPrefix, fixedPostfix, fixedDir, passwordTest, passwordTestString, tmpLocation, preferences, configurations, sessionUUID, uniqueID }

  location.mkdirs

  val tmpDir = new File(Workspace.allTmpDir(location), sessionUUID.toString)
  tmpDir.mkdirs

  val pluginDir = new File(location, pluginLocation)
  pluginDir.mkdirs

  val persistentDir = new File(location, persistentLocation)
  persistentDir.mkdirs

  val rng = Random.newRNG(uuid2long(sessionUUID))
  val currentSeed = new AtomicLong(rng.nextLong)
  def newSeed = currentSeed.getAndIncrement()

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

  lazy val overridden = collection.mutable.HashMap[String, String]()

  def clean = {
    tmpDir.recursiveDelete
  }

  def newDir(prefix: String = fixedDir): File = tmpDir.newDir(prefix)

  def newFile(prefix: String = fixedPrefix, suffix: String = fixedPostfix): File = tmpDir.newFile(prefix, suffix)

  def defaultValue(location: ConfigurationLocation): String = {
    configurations.get(location) match {
      case None ⇒ ""
      case Some(cf) ⇒
        val ret = cf()
        if (ret == null) ""
        else ret
    }
  }

  def overridePreference(preference: ConfigurationLocation, value: String): Unit =
    overridePreference(preference.toString, value)

  def overridePreference(preference: String, value: String): Unit = synchronized {
    overridden(preference) = value
  }

  def unsetOverridePreference(preference: String) = synchronized {
    overridden.remove(preference)
  }

  def rawPreference(location: ConfigurationLocation): String = synchronized {
    overridden.getOrElse(location.toString, configuration.subset(location.group).getString(location.name))
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

  def withTmpFile[T](prefix: String, postfix: String)(f: File ⇒ T): T = {
    val file = newFile(prefix, postfix)
    try f(file)
    finally file.delete
  }

  def withTmpFile[T](f: File ⇒ T): T = {
    val file = newFile()
    try f(file)
    finally file.delete
  }

  def reset() = synchronized {
    persistentDir.recursiveDelete
    val uniqueId = preference(uniqueID)
    configurationFile.content = ""
    configuration.clear
    _password = None
    setPreference(uniqueID, uniqueId)
  }

  def preferenceAsLong(location: ConfigurationLocation): Long = preference(location).toLong

  def setPassword(password: String): Unit = synchronized {
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

  def passwordIsCorrect(password: String) = synchronized {
    try {
      val test = rawPreference(passwordTest)
      if (test == null && Workspace.passwordChosen) false
      // allow password to be set initially
      else if (!Workspace.passwordChosen) true
      else {
        val te = textEncryptor(password)
        te.decrypt(test) == passwordTestString
      }
    }
    catch {
      case e: Throwable ⇒
        Logger.getLogger(Workspace.getClass.getName).log(Level.FINE, "Incorrect password", e)
        false
    }
  }

  def passwordChosen = isPreferenceSet(passwordTest)
  def passwordHasBeenSet =
    passwordChosen && Try(preference(Workspace.passwordTest) == passwordTestString).getOrElse(false)

  def preferenceAsDuration(location: ConfigurationLocation): FiniteDuration = preference(location)

  def isPreferenceSet(location: ConfigurationLocation): Boolean = synchronized {
    rawPreference(location) != null
  }

  def persistent(name: String) = Persistent(persistentDir / name)

  def authenticationProvider = Decrypt(password)

  lazy val authentications = new Persistent(persistentDir / Workspace.authenticationsLocation) with Authentication

  lazy val keyStoreLocation = file("OMKeystore")
  lazy val keyStorePassword = "openmole"
  lazy val keyStore = Certificate.loadOrGenerate(keyStoreLocation, keyStorePassword)

}
