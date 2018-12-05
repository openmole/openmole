package org.openmole.gui.plugin.authentication.egi

import org.openmole.gui.ext.data.{ PluginAPI, Test }

trait EGIAuthenticationAPI extends PluginAPI {
  //  def authentications(): Seq[AuthenticationData]
  def egiAuthentications(): Seq[EGIAuthenticationData]

  def addAuthentication(data: EGIAuthenticationData): Unit

  def removeAuthentication(): Unit

  def testAuthentication(data: EGIAuthenticationData): Seq[Test]

  def setVOTest(vos: Seq[String]): Unit

  def geVOTest(): Seq[String]

  /*def deleteAuthenticationKey(keyName: String): Unit

  def renameKey(keyName: String, newName: String): Unit

  def testAuthentication(data: AuthenticationData, vos: Seq[String] = Seq()): Seq[AuthenticationTest]*/
}
