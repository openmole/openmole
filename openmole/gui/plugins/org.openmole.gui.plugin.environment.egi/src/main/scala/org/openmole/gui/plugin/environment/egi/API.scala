package org.openmole.gui.plugin.environment.egi

import org.openmole.core.workspace.{ Decrypt, Workspace }

trait API {
  //  def authentications(): Seq[AuthenticationData]
  def egiAuthentications(): Seq[EGIAuthenticationData]

  def addAuthentication(data: EGIAuthenticationData): Unit

  def removeAuthentication(): Unit

  def renameKey(keyName: String, newName: String): Unit

  def testAuthentication(data: EGIAuthenticationData, vos: Seq[String] = Seq()): Seq[AuthenticationTest]
  /*def deleteAuthenticationKey(keyName: String): Unit

  def renameKey(keyName: String, newName: String): Unit

  def testAuthentication(data: AuthenticationData, vos: Seq[String] = Seq()): Seq[AuthenticationTest]*/
}
