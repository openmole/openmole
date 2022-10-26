package org.openmole.plugin.tool.methoddata

import io.circe.*

case class MethodData[T](name: T => String)(using val encoder: Encoder[T])

