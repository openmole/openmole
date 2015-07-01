package org.openmole.gui.plugin.environment.ssh

/*
 * Copyright (C) 16/06/2015 // mathieu.leclaire@openmole.org
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import fr.iscpif.gridscale.ssh.SSHPrivateKeyAuthentication
import org.openmole.gui.ext.data.AuthenticationData.{ PrivateKeyAuthenticationData, LoginPasswordAuthenticationData }
import org.openmole.gui.plugin.environment.ssh.client.{ SSHAuthenticationFactoryUI, SSHEnvironmentFactoryUI }
import org.openmole.gui.plugin.environment.ssh.ext.SSHEnvironmentData
import org.openmole.gui.plugin.environment.ssh.server.{ SSHPrivateKeyAuthenticationFactory, SSHLoginPasswordAuthenticationFactory, SSHEnvironmentFactory }
import org.openmole.gui.bootstrap.osgi._

class Activator extends OSGiActivator with ServerOSGiActivator {
  val data = SSHEnvironmentData()
  val lgpData = LoginPasswordAuthenticationData()
  val pkData = PrivateKeyAuthenticationData()

  override def factories = Seq(
    (data.getClass, new SSHEnvironmentFactory(data), new SSHEnvironmentFactoryUI)
  )

  override def authenticationFactories = Seq(
    (lgpData.getClass, new SSHLoginPasswordAuthenticationFactory, new SSHAuthenticationFactoryUI),
    (pkData.getClass, new SSHPrivateKeyAuthenticationFactory, new SSHAuthenticationFactoryUI)
  )

}