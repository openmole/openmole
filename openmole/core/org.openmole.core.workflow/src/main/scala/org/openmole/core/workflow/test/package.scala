package org.openmole.core.workflow.test

import org.openmole.core.format.ScriptSourceData
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import org.openmole.core.serializer.SerializerService
import org.openmole.core.workflow.mole.MoleExecution
import org.openmole.core.workflow.task.TaskExecutionBuildContext
import org.openmole.tool.file.*

def serializeDeserialize[T](o: T) =
  val builder = new ByteArrayOutputStream()
  serializer.serialize(o, builder)
  serializer.deserialize[T](new ByteArrayInputStream(builder.toByteArray))

export Stubs.*

implicit def noScriptSourceData: ScriptSourceData = ScriptSourceData.NoData


def withTmpFile[T](f: java.io.File => T): T =
  val dir = java.io.File.createTempFile("tmp", null)
  try f(dir)
  finally dir.recursiveDelete