package org.openmole.gui.plugin.authentication.egi

import org.openmole.gui.shared.data.{ErrorData, Test}
import org.openmole.gui.shared.api.*

trait EGIAuthenticationAPI extends RESTAPI:

  val egiAuthentications =
    errorEndpoint(get(path / "egi" / "authentications"), ok(jsonResponse[Seq[EGIAuthenticationData]]))

  val addAuthentication =
    errorEndpoint(post(path / "egi" / "add-authentication", jsonRequest[EGIAuthenticationData]), ok(jsonResponse[Unit]))

  val removeAuthentications =
    errorEndpoint(post(path / "egi" / "remove-authentications", jsonRequest[EGIAuthenticationData]), ok(jsonResponse[Unit]))

  val setVOTests =
    errorEndpoint(post(path / "egi" / "set-vo-tests", jsonRequest[Seq[String]]), ok(jsonResponse[Unit]))

  val getVOTests =
    errorEndpoint(get(path / "egi" / "get-vo-tests"), ok(jsonResponse[Seq[String]]))

  val testAuthentication =
    errorEndpoint(post(path / "egi" / "test-authentication", jsonRequest[EGIAuthenticationData]), ok(jsonResponse[Seq[Test]]))

