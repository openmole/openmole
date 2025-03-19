package org.openmole.core.workflow.execution

import org.openmole.tool.network.LocalHostName

import java.util.UUID

object RuntimeInfo:
  @transient lazy val localRuntimeInfo =
    RuntimeInfo(LocalHostName.localHostName.getOrElse("fake:" + UUID.randomUUID().toString))

case class RuntimeInfo(hostName: String)
