package org.openmole.gui.plugin.authentication.sshkey

import org.openmole.gui.ext.data.Test

trait PrivateKeyAuthenticationAPI {

  def privateKeyAuthentications(): Seq[PrivateKeyAuthenticationData]

  def addAuthentication(data: PrivateKeyAuthenticationData): Unit

  def removeAuthentication(data: PrivateKeyAuthenticationData): Unit

  def testAuthentication(data: PrivateKeyAuthenticationData): Seq[Test]
}