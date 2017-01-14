package org.openmole.gui.plugin.environment.egi

trait API {
  //  def authentications(): Seq[AuthenticationData]
  def egiAuthentications(): Seq[EGIAuthenticationData]

  def addAuthentication(data: AuthenticationData): Unit

  def removeAuthentication(data: AuthenticationData): Unit

  def deleteAuthenticationKey(keyName: String): Unit

  def renameKey(keyName: String, newName: String): Unit

  def testAuthentication(data: AuthenticationData, vos: Seq[String] = Seq()): Seq[AuthenticationTest]
}
