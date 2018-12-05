package org.openmole.gui.plugin.authentication.sshlogin

import org.openmole.gui.ext.data._

trait LoginAuthenticationAPI extends PluginAPI {
  //  def authentications(): Seq[AuthenticationData]
  def loginAuthentications(): Seq[LoginAuthenticationData]

  def addAuthentication(data: LoginAuthenticationData): Unit

  def removeAuthentication(data: LoginAuthenticationData): Unit

  def testAuthentication(data: LoginAuthenticationData): Seq[Test]
  /*def deleteAuthenticationKey(keyName: String): Unit

  def renameKey(keyName: String, newName: String): Unit

  def testAuthentication(data: AuthenticationData, vos: Seq[String] = Seq()): Seq[AuthenticationTest]*/
}