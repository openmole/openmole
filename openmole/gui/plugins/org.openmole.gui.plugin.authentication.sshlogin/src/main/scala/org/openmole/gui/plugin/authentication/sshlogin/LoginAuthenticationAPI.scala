package org.openmole.gui.plugin.authentication.sshlogin

import org.openmole.gui.shared.data.*
import org.openmole.gui.ext.api.*

trait LoginAuthenticationAPI extends RESTAPI {

  //def loginAuthentications(): Seq[LoginAuthenticationData]
  val loginAuthentications: Endpoint[Unit, Seq[LoginAuthenticationData]] =
    endpoint(get(path / "ssh" / "login-authentications"), ok(jsonResponse[Seq[LoginAuthenticationData]]))

  //def addAuthentication(data: LoginAuthenticationData): Unit
  val addAuthentication: Endpoint[LoginAuthenticationData, Unit] =
    endpoint(post(path / "ssh" / "add-login-authentication", jsonRequest[LoginAuthenticationData]), ok(jsonResponse[Unit]))

  //def removeAuthentication(data: LoginAuthenticationData): Unit
  val removeAuthentication: Endpoint[LoginAuthenticationData, Unit] =
    endpoint(post(path / "ssh" / "remove-login-authentication", jsonRequest[LoginAuthenticationData]), ok(jsonResponse[Unit]))

  //def testAuthentication(data: LoginAuthenticationData): Seq[Test]
  val testAuthentication: Endpoint[LoginAuthenticationData, Seq[Test]] =
    endpoint(post(path / "ssh" / "test-login-authentication", jsonRequest[LoginAuthenticationData]), ok(jsonResponse[Seq[Test]]))

}
