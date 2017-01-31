package org.openmole.gui.plugin.environment.egi

import org.openmole.gui.ext.data.Test

trait EGIAuthenticationAPI {
  //  def authentications(): Seq[AuthenticationData]
  def egiAuthentications(): Seq[EGIAuthenticationData]

  def addAuthentication(data: EGIAuthenticationData): Unit

  def removeAuthentication(): Unit

  def renameKey(keyName: String, newName: String): Unit

  def testAuthentication(data: EGIAuthenticationData): Seq[EGIAuthenticationTest]
  /*def deleteAuthenticationKey(keyName: String): Unit

  def renameKey(keyName: String, newName: String): Unit

  def testAuthentication(data: AuthenticationData, vos: Seq[String] = Seq()): Seq[AuthenticationTest]*/
}
