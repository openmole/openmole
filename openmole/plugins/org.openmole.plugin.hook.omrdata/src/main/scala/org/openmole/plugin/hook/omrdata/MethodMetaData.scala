package org.openmole.plugin.hook.omrdata

import io.circe.*

case class MethodMetaData[T](plugin: T => String)(using val encoder: Encoder[T])

