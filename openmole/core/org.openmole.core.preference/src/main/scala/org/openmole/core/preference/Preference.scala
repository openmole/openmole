package org.openmole.core.preference

import java.util.UUID

import org.openmole.tool.crypto._
import org.openmole.tool.file._
import org.openmole.core.exception._

object Preference:

  lazy val uniqueID = PreferenceLocation[String]("Global", "UniqueID", None)
  def passwordTest = PreferenceLocation.cyphered[String]("Preference", "passwordTest", Some(passwordTestString))
  def passwordTestString = "test"
  def location = "preferences"

  def memory() =
    val pref = new MemoryPreference
    pref setPreference (uniqueID, UUID.randomUUID.toString)
    pref

  def apply(file: File): Preference =
    val pref = FilePreference(ConfigurationFile(file))
    //if (!passwordIsCorrect(cypher, pref)) throw new UserBadDataError("Password is incorrect.")
    //setPasswordTest(pref, cypher)
    if !pref.isSet(uniqueID) then pref.setPreference(uniqueID, UUID.randomUUID.toString)
    pref

  def passwordIsCorrect(cypher: Cypher, preference: Preference) =
    util.Try(cypher.decrypt(preference.getRawPreference(passwordTest).getOrElse(passwordTestString)) == passwordTestString).getOrElse(false)

  def passwordChosen(preference: Preference) = preference.getRawPreference(passwordTest).isDefined

  def setPasswordTest(preference: Preference, cypher: Cypher) =
    implicit val _cypher = cypher
    preference.setPreference(passwordTest, passwordTestString)

  def stub() = memory()


trait Preference:
  //  def apply[T: ConfigurationString](location: ClearConfigurationLocation[T]): T
  //  def apply[T: ConfigurationString](location: CypheredConfigurationLocation[T])(implicit cypher: Cypher)
  //
  //  def rawPreference[T](location: ConfigurationLocation[T]): Option[String]
  //
  //  def preferenceOption[T](location: ClearConfigurationLocation[T])(implicit fromString: ConfigurationString[T]): Option[T]
  //  def preferenceOption[T](location: CypheredConfigurationLocation[T])(implicit fromString: ConfigurationString[T], cypher: Cypher): Option[T]
  //
  //  def setPreference[T](location: ClearConfigurationLocation[T], value: T)(implicit configurationString: ConfigurationString[T])
  //  def setPreference[T](location: CypheredConfigurationLocation[T], value: T)(implicit configurationString: ConfigurationString[T], cypher: Cypher)
  //
  //  def isSet[T](location: ConfigurationLocation[T]): Boolean

  def apply[T: ConfigurationString](location: ClearPreferenceLocation[T]): T =
    preferenceOption(location) getOrElse (throw new UserBadDataError(s"No value found for $location and no default is defined for this property."))

  def apply[T: ConfigurationString](location: CypheredPreferenceLocation[T])(implicit cypher: Cypher) =
    preferenceOption(location) getOrElse (throw new UserBadDataError(s"No value found for $location and no default is defined for this property."))

  def preferenceOption[T: ConfigurationString](location: PreferenceLocation[T])(implicit cypher: Cypher): Option[T] =
    location match
      case c: ClearPreferenceLocation[T]    => preferenceOption(c)
      case c: CypheredPreferenceLocation[T] => preferenceOption(c)

  def preferenceOption[T](location: ClearPreferenceLocation[T])(implicit fromString: ConfigurationString[T]): Option[T] = synchronized:
    val confVal = getRawPreference(location)
    confVal.map(fromString.fromString) orElse location.default

  def preferenceOption[T](location: CypheredPreferenceLocation[T])(implicit fromString: ConfigurationString[T], cypher: Cypher): Option[T] = synchronized:
    val confVal = getRawPreference(location)
    def v = confVal.map(cypher.decrypt(_))
    v.map(fromString.fromString) orElse location.default

  def setPreference[T: ConfigurationString](location: PreferenceLocation[T], value: T)(implicit cypher: Cypher): Unit =
    location match
      case c: ClearPreferenceLocation[T]    => setPreference(c, value)
      case c: CypheredPreferenceLocation[T] => setPreference(c, value)

  def setPreference[T](location: ClearPreferenceLocation[T], value: T)(implicit configurationString: ConfigurationString[T]) = synchronized:
    val v = configurationString.toString(value)
    setRawPreference(location, v)

  def setPreference[T](location: CypheredPreferenceLocation[T], value: T)(implicit configurationString: ConfigurationString[T], cypher: Cypher) = synchronized:
    val v = configurationString.toString(value)
    val prop = cypher.encrypt(v)
    setRawPreference(location, prop)

  def updatePreference[T: ConfigurationString](location: PreferenceLocation[T])(value: Option[T] => Option[T])(implicit cypher: Cypher) = synchronized {
    val v = preferenceOption(location)
    val newValue = value(v)

    newValue match
      case None => clearPreference(location)
      case Some(v) => setPreference(location, v)

    newValue
  }

  def isSet[T](location: PreferenceLocation[T]) = synchronized { getRawPreference(location).isDefined }

  def clearPreference[T](location: PreferenceLocation[T]): Unit
  def clear(): Unit

  protected def setRawPreference(location: PreferenceLocation[_], value: String): Unit
  protected def getRawPreference[T](location: PreferenceLocation[T]): Option[String]


case class FilePreference(configurationFile: ConfigurationFile) extends Preference:

  protected def getRawPreference[T](location: PreferenceLocation[T]) = synchronized { configurationFile.value(location.group, location.name) }

  protected def setRawPreference(location: PreferenceLocation[_], value: String) = synchronized {
    configurationFile.setValue(location.group, location.name, value)
  }

  def clearPreference[T](location: PreferenceLocation[T]) = synchronized { configurationFile.clearValue(location.group, location.name) }

  def clear() =
    val uniqueId = getRawPreference(Preference.uniqueID)
    try configurationFile.clear()
    finally uniqueId.foreach(prop => setPreference(Preference.uniqueID, prop))
    //      _password = None
    //      setPreference(Workspace.uniqueIDLocation, uniqueId)
    //    }


class MemoryPreference() extends Preference:

  lazy val map = collection.mutable.Map[(String, String), String]()

  override def clearPreference[T](location: PreferenceLocation[T]): Unit = synchronized {
    map.remove((location.group, location.name))
  }

  override def clear(): Unit = synchronized(map.clear())

  override protected def setRawPreference(location: PreferenceLocation[_], value: String): Unit = synchronized {
    map((location.group, location.name)) = value
  }

  override protected def getRawPreference[T](location: PreferenceLocation[T]): Option[String] = synchronized {
    map.get((location.group, location.name))
  }
