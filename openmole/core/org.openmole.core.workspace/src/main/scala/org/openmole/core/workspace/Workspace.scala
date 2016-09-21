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
import java.security.Security
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import java.util.logging.Level
import java.util.logging.Logger

import org.jasypt.util.text._
import org.openmole.core.event.{ Event, EventDispatcher }
import org.openmole.core.exception.{ InternalProcessingError, UserBadDataError }
import org.openmole.core.replication.DBServerInfo
import org.openmole.core.tools.io.FromString
import org.openmole.tool.crypto.Certificate
import org.openmole.tool.file._
import org.openmole.core.tools.service._
import Random._

object Workspace {

  case object PasswordRequired extends Event[Workspace]

  lazy val sessionUUID = UUID.randomUUID

  def preferences = "preferences"
  def globalGroup = "Global"
  def tmpLocation = ".tmp"
  def persistentLocation = "persistent"
  def authenticationsLocation = "authentications"
  def pluginLocation = "plugins"

  lazy val uniqueIDLocation = ConfigurationLocation[String](globalGroup, "UniqueID", None)

  private val group = "Workspace"
  def fixedPrefix = "file"
  def fixedPostfix = ".bin"
  def fixedDir = "dir"

  private def passwordTestString = "test"
  private def passwordTest = ConfigurationLocation[String](group, "passwordTest", Some(passwordTestString), true)

  def ErrorArraySnipSize = ConfigurationLocation[Int](group, "ErrorArraySnipSize", Some(10))

  lazy val defaultLocation = DBServerInfo.base

  Runtime.getRuntime.addShutdownHook(new Thread {
    override def run = synchronized {
      instance.clean
    }
  })

  def setDefault[T: ConfigurationString](location: ConfigurationLocation[T]) = synchronized {
    instance.setDefault(location)
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

  def OpenMOLELocationProperty = "openmole.location"

  def openMOLELocationOption =
    Option(System.getProperty(OpenMOLELocationProperty, null)).map(new File(_))

  def openMOLELocation =
    openMOLELocationOption.getOrElse(throw new InternalProcessingError("openmole.location not set"))

  lazy val instance = new Workspace(defaultLocation)

  instance setPreferenceIfNotSet (uniqueIDLocation, UUID.randomUUID.toString)
  instance setDefault ErrorArraySnipSize

  implicit def authenticationProvider = instance.authenticationProvider

}

class Workspace(val location: File) {

  import Workspace._
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

  @transient private lazy val configurationFile: ConfigurationFile = {
    val file = new File(location, preferences)
    if (file.createNewFile) file.setPosixMode("rw-------")
    new ConfigurationFile(file)
  }

  def clean = {
    tmpDir.recursiveDelete
  }

  def newDir(prefix: String = fixedDir): File = tmpDir.newDir(prefix)

  def newFile(prefix: String = fixedPrefix, suffix: String = fixedPostfix): File = tmpDir.newFile(prefix, suffix)

  def preferenceOption[T](location: ConfigurationLocation[T])(implicit fromString: ConfigurationString[T]): Option[T] = synchronized {
    val confVal = configurationFile.value(location.group, location.name)
    def v =
      if (!location.cyphered) confVal
      else confVal.map(decrypt(_, password))
    v.map(fromString.fromString) orElse location.default
  }

  def preference[T: ConfigurationString](location: ConfigurationLocation[T]): T = synchronized {
    preferenceOption(location) getOrElse (throw new UserBadDataError(s"No value found for $location and no default is defined for this property."))
  }

  def setPreference[T](location: ConfigurationLocation[T], value: T)(implicit configurationString: ConfigurationString[T]) = synchronized {
    val v = configurationString.toString(value)
    val prop = if (location.cyphered) encrypt(v) else v
    configurationFile.setValue(location.group, location.name, prop)
  }

  def setPreferenceIfNotSet[T](location: ConfigurationLocation[T], value: T)(implicit configurationString: ConfigurationString[T]) = synchronized {
    if (!preferenceIsSet(location)) setPreference(location, value)
  }

  def setDefault[T](location: ConfigurationLocation[T])(implicit configurationString: ConfigurationString[T]) = synchronized {
    if (!preferenceIsSet(location)) {
      def defaultOrException = location.default.getOrElse(throw new UserBadDataError(s"No default value set for location ${this}."))
      def prop = if (location.cyphered) encrypt(configurationString.toString(defaultOrException)) else configurationString.toString(defaultOrException)
      configurationFile.setCommentedValue(location.group, location.name, prop)
    }
  }

  def file(name: String): File = new File(location, name)

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

  def withTmpDir[T](f: File ⇒ T): T = {
    val file = newFile()
    try {
      file.mkdir()
      f(file)
    }
    finally file.recursiveDelete
  }

  def reset() = synchronized {
    persistentDir.recursiveDelete
    val uniqueId = preference(uniqueIDLocation)
    configurationFile.clear()
    _password = None
    setPreference(uniqueIDLocation, uniqueId)
  }

  def setPassword(password: String): Unit = synchronized {
    if (!passwordIsCorrect(password)) throw new UserBadDataError("Password is incorrect.")
    this._password = Some(password)
    setPreference(Workspace.passwordTest, passwordTestString)
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
      configurationFile.value(passwordTest.group, passwordTest.name) match {
        case None ⇒ true
        case Some(t) ⇒
          textEncryptor(password).decrypt(t) == passwordTestString
      }
    }
    catch {
      case e: Throwable ⇒
        Logger.getLogger(Workspace.getClass.getName).log(Level.FINE, "Incorrect password", e)
        false
    }
  }

  def preferenceIsSet[T](configurationLocation: ConfigurationLocation[T]) = configurationFile.value(configurationLocation.group, configurationLocation.name).isDefined
  def passwordChosen = configurationFile.value(passwordTest.group, passwordTest.name).isDefined
  def passwordHasBeenSet = _password.map(passwordIsCorrect).getOrElse(false)

  def persistent(name: String) = Persistent(persistentDir / name)

  def authenticationProvider = Decrypt(password)

  lazy val authentications = new Persistent(persistentDir / Workspace.authenticationsLocation) with Authentication

  lazy val keyStoreLocation = file("OMKeystore")
  lazy val keyStorePassword = "openmole"
  lazy val keyStore = Certificate.loadOrGenerate(keyStoreLocation, keyStorePassword)

}
