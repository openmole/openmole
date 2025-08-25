package org.openmole.gui.plugin.authentication.sshlogin

import org.openmole.gui.client.ext.*
import org.openmole.gui.shared.data.*
import org.openmole.gui.shared.api.*
import org.openmole.core.services.Services
import scala.concurrent.Future

trait LoginAuthenticationAPI:
  def loginAuthentications()(using basePath: BasePath, notificationAPI: NotificationService): Future[Seq[LoginAuthenticationData]]
  def addAuthentication(data: LoginAuthenticationData)(using basePath: BasePath, notificationAPI: NotificationService): Future[Unit]
  def removeAuthentication(data: LoginAuthenticationData)(using basePath: BasePath, notificationAPI: NotificationService): Future[Unit]
  def testAuthentication(data: LoginAuthenticationData)(using basePath: BasePath, notificationAPI: NotificationService): Future[Seq[Test]]

class LoginAuthenticationServerAPI extends LoginAuthenticationAPI:
  def sttp = STTPInterpreter()
  override def loginAuthentications()(using basePath: BasePath, notificationAPI: NotificationService): Future[Seq[LoginAuthenticationData]] = sttp.toRequest(LoginAuthenticationRESTAPI.loginAuthentications)(())
  override def addAuthentication(data: LoginAuthenticationData)(using basePath: BasePath, notificationAPI: NotificationService): Future[Unit] = sttp.toRequest(LoginAuthenticationRESTAPI.addAuthentication)(data)
  override def removeAuthentication(data: LoginAuthenticationData)(using basePath: BasePath, notificationAPI: NotificationService): Future[Unit] = sttp.toRequest(LoginAuthenticationRESTAPI.removeAuthentication)(data)
  override def testAuthentication(data: LoginAuthenticationData)(using basePath: BasePath, notificationAPI: NotificationService): Future[Seq[Test]] = sttp.toRequest(LoginAuthenticationRESTAPI.testAuthentication)(data)

class LoginAuthenticationStubAPI extends LoginAuthenticationAPI:
  
  override def loginAuthentications()(using basePath: BasePath, notificationAPI: NotificationService): Future[Seq[LoginAuthenticationData]] = Future.successful {
    Seq(
      LoginAuthenticationData(
        "stub",
        "stub",
        "stub.stub.stub"
      ),
      LoginAuthenticationData(
        "stub2",
        "stub2",
        "stub2.stub.stub"
      )
    )
  }

  override def addAuthentication(data: LoginAuthenticationData)(using basePath: BasePath, notificationAPI: NotificationService): Future[Unit] = Future.successful(())
  override def removeAuthentication(data: LoginAuthenticationData)(using basePath: BasePath, notificationAPI: NotificationService): Future[Unit] = Future.successful(())
  override def testAuthentication(data: LoginAuthenticationData)(using basePath: BasePath, notificationAPI: NotificationService): Future[Seq[Test]] = Future.successful(Seq())


