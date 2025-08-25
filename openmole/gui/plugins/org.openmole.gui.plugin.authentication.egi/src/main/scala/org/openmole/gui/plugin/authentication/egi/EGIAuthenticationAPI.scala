package org.openmole.gui.plugin.authentication.egi

import org.openmole.gui.shared.data.{ErrorData, Test}
import org.openmole.gui.shared.api.*

import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import io.circe.generic.auto.*

object EGIAuthenticationAPI:

  lazy val egiAuthentications =
    endpoint.get.in("egi" / "authentications").out(jsonBody[Seq[EGIAuthenticationData]]).errorOut(jsonBody[ErrorData])

  lazy val addAuthentication =
    endpoint.post.in("egi" / "add-authentication").in(jsonBody[EGIAuthenticationData]).errorOut(jsonBody[ErrorData])
    
  lazy val removeAuthentications =
    endpoint.post.in("egi" / "remove-authentications").in(jsonBody[(EGIAuthenticationData, Boolean)]).errorOut(jsonBody[ErrorData])
    
  lazy val setVOTests =
    endpoint.post.in("egi" / "set-vo-tests").in(jsonBody[Seq[String]]).errorOut(jsonBody[ErrorData])
  
  lazy val getVOTests =
    endpoint.get.in("egi" / "get-vo-tests").out(jsonBody[Seq[String]]).errorOut(jsonBody[ErrorData])

  lazy val testAuthentication =
    endpoint.post.in("egi" / "test-authentication").in(jsonBody[EGIAuthenticationData]).out(jsonBody[Seq[Test]]).errorOut(jsonBody[ErrorData])

