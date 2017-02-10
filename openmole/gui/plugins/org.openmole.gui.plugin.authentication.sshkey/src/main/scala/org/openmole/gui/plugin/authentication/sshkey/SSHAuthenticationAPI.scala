package org.openmole.gui.plugin.authentication.sshkey

import org.openmole.gui.ext.data.Test

trait PrivateKeyAuthenticationAPI {
  //  def authentications(): Seq[AuthenticationData]

  def privateKeyAuthentications(): Seq[PrivateKeyAuthenticationData]

  def addAuthentication(data: PrivateKeyAuthenticationData): Unit

  def removeAuthentication(): Unit

  def testAuthentication(data: PrivateKeyAuthenticationData): Seq[Test]
  /*def deleteAuthenticationKey(keyName: String): Unit

  def renameKey(keyName: String, newName: String): Unit

  def testAuthentication(data: AuthenticationData, vos: Seq[String] = Seq()): Seq[AuthenticationTest]*/
}