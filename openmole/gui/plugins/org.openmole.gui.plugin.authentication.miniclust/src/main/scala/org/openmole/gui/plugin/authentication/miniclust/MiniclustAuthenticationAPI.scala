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

object MiniclustAuthenticationServerAPI:
  class APIClientImpl(val settings: ClientSettings) extends MiniclustAuthenticationRESTAPI with APIClient
  def PluginFetch = Fetch(new APIClientImpl(_))

class MiniclustAuthenticationServerAPI extends MiniclustAuthenticationAPI:
  import MiniclustAuthenticationServerAPI.PluginFetch
  override def loginAuthentications()(using basePath: BasePath, notificationAPI: NotificationService): Future[Seq[MiniclustAuthenticationData]] = PluginFetch.futureError(_.loginAuthentications(()).future)
  override def addAuthentication(data: MiniclustAuthenticationData)(using basePath: BasePath, notificationAPI: NotificationService): Future[Unit] = PluginFetch.futureError(_.addAuthentication(data).future)
  override def removeAuthentication(data: MiniclustAuthenticationData)(using basePath: BasePath, notificationAPI: NotificationService): Future[Unit] = PluginFetch.futureError(_.removeAuthentication(data).future)
  override def testAuthentication(data: MiniclustAuthenticationData)(using basePath: BasePath, notificationAPI: NotificationService): Future[Seq[Test]] = PluginFetch.futureError(_.testAuthentication(data).future)
