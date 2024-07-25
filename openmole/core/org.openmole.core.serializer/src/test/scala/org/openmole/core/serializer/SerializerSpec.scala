package org.openmole.core.serializer

import org.scalatest.*
import org.openmole.tool.file.*

object SerializerSpec:
  class C1(val f: File)
  class C2(val f2: File, val c1: C1)

class SerializerSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers:

  "Serilizer" should "list files" in:
    val f1 = File("/tmp/test1")
    val f2 = File("/tmp/test2")

    import SerializerSpec.*
    val c = new C2(f2, new C1(f1))

    val files = SerializerService().listFiles(c)
    files should contain(f1)
    files should contain(f2)
    files.size should be(2)


