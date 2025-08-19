package org.openmole.gui.plugin.authentication.miniclust

import org.openmole.core.services.Services
import org.openmole.gui.client.ext.*
import org.openmole.gui.shared.api.*
import org.openmole.gui.shared.data.*

import scala.concurrent.Future

trait MiniclustAuthenticationAPI:
  def loginAuthentications()(using basePath: BasePath, notificationAPI: NotificationService): Future[Seq[MiniclustAuthenticationData]]
  def addAuthentication(data: MiniclustAuthenticationData)(using basePath: BasePath, notificationAPI: NotificationService): Future[Unit]
  def removeAuthentication(data: MiniclustAuthenticationData)(using basePath: BasePath, notificationAPI: NotificationService): Future[Unit]
  def testAuthentication(data: MiniclustAuthenticationData)(using basePath: BasePath, notificationAPI: NotificationService): Future[Seq[Test]]
  
class MiniclustAuthenticationServerAPI extends MiniclustAuthenticationAPI:
  def sttp = STTPInterpreter()
  
  override def loginAuthentications()(using basePath: BasePath, notificationAPI: NotificationService): Future[Seq[MiniclustAuthenticationData]] = sttp.toRequest(MiniclustAuthenticationRESTAPI.loginAuthentications)(())
  override def addAuthentication(data: MiniclustAuthenticationData)(using basePath: BasePath, notificationAPI: NotificationService): Future[Unit] = sttp.toRequest(MiniclustAuthenticationRESTAPI.addAuthentication)(data)
  override def removeAuthentication(data: MiniclustAuthenticationData)(using basePath: BasePath, notificationAPI: NotificationService): Future[Unit] = sttp.toRequest(MiniclustAuthenticationRESTAPI.removeAuthentication)(data)
  override def testAuthentication(data: MiniclustAuthenticationData)(using basePath: BasePath, notificationAPI: NotificationService): Future[Seq[Test]] =sttp.toRequest(MiniclustAuthenticationRESTAPI.testAuthentication)(data)
