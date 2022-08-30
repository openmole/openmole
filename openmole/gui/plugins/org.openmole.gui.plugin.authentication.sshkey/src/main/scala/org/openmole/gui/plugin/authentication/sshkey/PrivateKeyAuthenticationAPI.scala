package org.openmole.gui.plugin.authentication.sshkey

import org.openmole.gui.ext.data.*
import org.openmole.gui.ext.api.*

trait PrivateKeyAuthenticationAPI extends RESTAPI {

  val privateKeyAuthentications: Endpoint[Unit, Seq[PrivateKeyAuthenticationData]] =
    endpoint(get(path / "ssh" / "privatekey-authentications"), ok(jsonResponse[Seq[PrivateKeyAuthenticationData]]))

  val addAuthentication: Endpoint[PrivateKeyAuthenticationData, Unit] =
    endpoint(post(path / "ssh" / "add-privatekey-authentication", jsonRequest[PrivateKeyAuthenticationData]), ok(jsonResponse[Unit]))

  val removeAuthentication: Endpoint[PrivateKeyAuthenticationData, Unit] =
    endpoint(post(path / "ssh" / "remove-privatekey-authentication", jsonRequest[PrivateKeyAuthenticationData]), ok(jsonResponse[Unit]))

  val testAuthentication: Endpoint[PrivateKeyAuthenticationData, Seq[Test]] =
    endpoint(post(path / "ssh" / "test-privatekey-authentication", jsonRequest[PrivateKeyAuthenticationData]), ok(jsonResponse[Seq[Test]]))

}