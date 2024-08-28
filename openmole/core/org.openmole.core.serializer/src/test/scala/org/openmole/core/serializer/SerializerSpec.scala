package org.openmole.core.serializer

import org.scalatest.*
import org.openmole.tool.file.*
import org.openmole.tool.stream.{StringInputStream, StringOutputStream}

import fury.*

object SerializerSpec:
  case class C1(f: File)
  case class C2(f2: File, c1: C1)

class SerializerSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers:

  "Serializer" should "list files" in:
    val f1 = File("/tmp/test1")
    val f2 = File("/tmp/test2")

    import SerializerSpec.*
    val c = C2(f2, C1(f1))

    val files = FurySerializerService().listFiles(c)
    files should contain(f1)
    files should contain(f2)
    files.size should be(2)

  "Serializer" should "be able to serialize and deserialize an object" in:
    val f1 = File("/tmp/test1")
    val f2 = File("/tmp/test2")

    import SerializerSpec.*
    val c = C2(f2, C1(f1))
    val service = FurySerializerService()

    val os = new StringOutputStream()
    service.serialize(c, os)
    val after = service.deserialize[C2](new StringInputStream(os.builder.toString))
    (c == after) should be(true)

