package org.openmole.core.tools.service

import java.util.UUID

import org.openmole.tool.network.LocalHostName

object RuntimeInfo {
  @transient lazy val localRuntimeInfo =
    RuntimeInfo(LocalHostName.localHostName.getOrElse("fake:" + UUID.randomUUID().toString))

}

case class RuntimeInfo(hostName: String)
