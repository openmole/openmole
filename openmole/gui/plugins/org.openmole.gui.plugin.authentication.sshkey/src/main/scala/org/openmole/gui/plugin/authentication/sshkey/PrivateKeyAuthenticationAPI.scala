package org.openmole.gui.plugin.authentication.sshkey

import org.openmole.gui.shared.data.*
import org.openmole.gui.shared.api.*

trait PrivateKeyAuthenticationAPI extends RESTAPI:

  val privateKeyAuthentications: ErrorEndpoint[Unit, Seq[PrivateKeyAuthenticationData]] =
    errorEndpoint(get(path / "ssh" / "privatekey-authentications"), ok(jsonResponse[Seq[PrivateKeyAuthenticationData]]))

  val addAuthentication: ErrorEndpoint[PrivateKeyAuthenticationData, Unit] =
    errorEndpoint(post(path / "ssh" / "add-privatekey-authentication", jsonRequest[PrivateKeyAuthenticationData]), ok(jsonResponse[Unit]))

  val removeAuthentication: ErrorEndpoint[PrivateKeyAuthenticationData, Unit] =
    errorEndpoint(post(path / "ssh" / "remove-privatekey-authentication", jsonRequest[PrivateKeyAuthenticationData]), ok(jsonResponse[Unit]))

  val testAuthentication: ErrorEndpoint[PrivateKeyAuthenticationData, Seq[Test]] =
    errorEndpoint(post(path / "ssh" / "test-privatekey-authentication", jsonRequest[PrivateKeyAuthenticationData]), ok(jsonResponse[Seq[Test]]))

