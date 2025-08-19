package org.openmole.gui.plugin.authentication.sshkey

import org.openmole.gui.shared.data.*
import org.openmole.gui.shared.api.*

import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import io.circe.generic.auto.*

object PrivateKeyAuthenticationAPI:

  lazy val privateKeyAuthentications: TapirEndpoint[Unit, Seq[PrivateKeyAuthenticationData]] =
    endpoint.get.in("ssh" / "privatekey-authentications").out(jsonBody[Seq[PrivateKeyAuthenticationData]]).errorOut(jsonBody[ErrorData])

  lazy val addAuthentication: TapirEndpoint[PrivateKeyAuthenticationData, Unit] =
    endpoint.post.in("ssh" / "add-privatekey-authentication").in(jsonBody[PrivateKeyAuthenticationData]).errorOut(jsonBody[ErrorData])

  lazy val removeAuthentication: TapirEndpoint[(PrivateKeyAuthenticationData, Boolean), Unit] =
    endpoint.post.in("ssh" / "remove-privatekey-authentication").in(jsonBody[(PrivateKeyAuthenticationData, Boolean)]).errorOut(jsonBody[ErrorData])

  lazy val testAuthentication: TapirEndpoint[PrivateKeyAuthenticationData, Seq[Test]] =
    endpoint.post.in("ssh" / "test-privatekey-authentication").in(jsonBody[PrivateKeyAuthenticationData]).out(jsonBody[Seq[Test]]).errorOut(jsonBody[ErrorData])

