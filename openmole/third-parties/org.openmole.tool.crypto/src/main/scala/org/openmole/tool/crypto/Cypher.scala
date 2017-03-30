package org.openmole.tool.crypto

import org.jasypt.util.text._

object Cypher {

  object NoneTextEncryptor extends TextEncryptor {
    def decrypt(encryptedMessage: String) = encryptedMessage
    def encrypt(message: String) = message
  }

  def textEncryptor(password: String): TextEncryptor =
    if (!password.trim.isEmpty) {
      val textEncryptor = new BasicTextEncryptor
      textEncryptor.setPassword(password)
      textEncryptor
    }
    else NoneTextEncryptor

  //def passwordHasBeenSet = _password.map(passwordIsCorrect).getOrElse(false)

  //  def setPassword(password: String)(preference: Preference): Unit = synchronized {
  //    if (!passwordIsCorrect(password)) throw new UserBadDataError("Password is incorrect.")
  //    this._password = Some(password)
  //    preference.setPreference(Workspace.passwordTest, passwordTestString)
  //  }

  def apply(password: String): Cypher = Cypher(Some(password))

}

case class Cypher(private val password: Option[String]) {

  //  private[workspace] def password = {
  //    _password match {
  //      case None    ⇒ EventDispatcher.trigger(this, Workspace.PasswordRequired)
  //      case Some(p) ⇒
  //    }
  //    _password.getOrElse(throw new UserBadDataError("Password is not set."))
  //  }

  lazy val encryptor = password match {
    case Some(p) ⇒ Cypher.textEncryptor(p)
    case None    ⇒ Cypher.NoneTextEncryptor
  }

  def encrypt(s: String) = encryptor.encrypt(s)
  def decrypt(s: String) = if (s.isEmpty) s else encryptor.decrypt(s)

}
