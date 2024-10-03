package org.openmole.gui.client.core.git

/*
 * Copyright (C) 2024 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * Created by Romain Reuillon on 28/11/16.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */


import com.raquo.laminar.api.L.*
import org.openmole.gui.client.ext.*
import org.openmole.gui.shared.api.*
import org.openmole.gui.shared.data.*
import scaladget.bootstrapnative.bsn.*
import scaladget.tools.*

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.annotation.*


class GitPrivateKeyAuthenticationFactory(api: ServerAPI) extends AuthenticationPluginFactory:
  type AuthType = GitPrivateKeyAuthenticationData
  def buildEmpty = new GitPrivateKeyAuthenticationGUI(api, GitPrivateKeyAuthenticationData.empty)
  def build(data: AuthType) = new GitPrivateKeyAuthenticationGUI(api, data)
  def name = "Git SSH Private key"
  def getData(using basePath: BasePath, notificationAPI: NotificationService): Future[Seq[AuthType]] = api.gitAuthentications()
  def test(data: AuthType)(using basePath: BasePath, notificationAPI: NotificationService) = api.testGitAuthentication(data)
  def remove(data: AuthType)(using basePath: BasePath, notificationAPI: NotificationService) = api.removeGitAuthentication(data, true)


class GitPrivateKeyAuthenticationGUI(api: ServerAPI, data: GitPrivateKeyAuthenticationData) extends AuthenticationPlugin[GitPrivateKeyAuthenticationData]:

  val passwordStyle: HESetters = Seq(
    width := "130",
    `type` := "password"
  )

  val privateKeyUploader = AuthenticationUploaderUI(data.privateKey, data.directory)
  val passwordInput = inputTag(data.password).amend(placeholder := "Password", `type` := "password")

  def name = data.privateKeyPath.map(_.name).getOrElse("No Name")

  def panel(using api: ServerAPI, basePath: BasePath, notificationAPI: NotificationService) = div(
    flexColumn, width := "400px", height := "220",
    div(cls := "verticalFormItem", div("Password", width := "150px"), passwordInput),
    div(cls := "verticalFormItem", div("Private key", width := "150px"), display.flex, div(privateKeyUploader.view.amend(flexRow, justifyContent.flexEnd), width := "100%"))
  )

  def save(using basePath: BasePath, notificationAPI: NotificationService) =
    api.removeGitAuthentication(data, false).andThen: _ =>
      val data =
        GitPrivateKeyAuthenticationData(
          privateKey = privateKeyUploader.file.now(),
          password = passwordInput.ref.value,
          directory = privateKeyUploader.directory
        )

      api.addGitAuthentication(data)

