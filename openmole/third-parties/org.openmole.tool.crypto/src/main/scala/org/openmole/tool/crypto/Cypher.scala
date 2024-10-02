package org.openmole.tool.crypto

import org.jasypt.util.text.*

import java.util.Base64

object Cypher:

  object Obfuscator extends TextEncryptor:
    def decrypt(encryptedMessage: String) =
      val decodedBytes = Base64.getDecoder.decode(encryptedMessage)
      new String(decodedBytes)

    def encrypt(message: String) = Base64.getEncoder.encodeToString(message.getBytes)

  def textEncryptor(password: String): TextEncryptor =
    if !password.trim.isEmpty
    then
      val textEncryptor = new BasicTextEncryptor
      textEncryptor.setPassword(password)
      textEncryptor
    else Obfuscator

  def apply(password: String): Cypher = Cypher(Some(password))


case class Cypher(private val password: Option[String]):
  lazy val encryptor =
    password match
      case Some(p) ⇒ Cypher.textEncryptor(p)
      case None    ⇒ Cypher.Obfuscator

  def encrypt(s: String) = encryptor.encrypt(s)
  def decrypt(s: String) = if s == null || s.isEmpty then s else encryptor.decrypt(s)


