package org.openmole.core.workflow.test

import org.openmole.core.script.ScriptSourceData

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import org.openmole.core.serializer.SerializerService


def serializeDeserialize[T](o: T) =
  val builder = new ByteArrayOutputStream()
  serializer.serialize(o, builder)
  serializer.deserialize[T](new ByteArrayInputStream(builder.toByteArray))

export Stubs.*

implicit def noScriptSourceData: ScriptSourceData = ScriptSourceData.NoData

