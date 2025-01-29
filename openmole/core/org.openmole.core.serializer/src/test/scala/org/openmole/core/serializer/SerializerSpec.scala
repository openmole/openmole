package org.openmole.core.serializer

import org.scalatest.*
import org.openmole.tool.file.*
import org.openmole.tool.stream.{StringInputStream, StringOutputStream}
import org.openmole.core.fileservice
import org.openmole.core.fileservice.FileService
import org.openmole.core.fileservice.FileService.FileWithGC
import org.openmole.core.workspace.TmpDirectory
import org.openmole.tool.cache.KeyValueCache

object SerializerSpec:
  def test(using FileService) =
    val f1 = File("/tmp/test1")
    val f2 = File("/tmp/test2")
    val fgc = FileWithGC("/tmp/filewithgc", summon[FileService])
    TestClass(f2, C1(f1), fgc, classOf[MetaClass], TestEnum.T1)

  val pluginFile = File("/tmp/plugin")

  class MetaClass
  case class C1(f1: File) extends plugin.Plugins:
    override def plugins: Seq[File] = Seq(pluginFile)

  case class TestClass(f2: File, c1: C1, fgc: File, c: Class[MetaClass], t: TestEnum)

  enum TestEnum:
    case T1, T2



class SerializerSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers:
  given FileService = FileService.stub()

  "Serializer" should "list files" in:
    import SerializerSpec.*
    val c = SerializerSpec.test

    val files = SerializerService().listFiles(c)
    files should contain(c.c1.f1)
    files should contain(c.f2)
    files should contain(c.fgc)
    files.size shouldEqual 3

  "Serializer" should "be able to serialize and deserialize an object" in:
    import SerializerSpec.*
    val c = SerializerSpec.test
    val service = SerializerService()

    val os = new StringOutputStream()
    service.serialize(c, os)
    val after = service.deserialize[TestClass](new StringInputStream(os.builder.toString))
    (c == after) should be(true)


  "Serializer" should "be able to replace files" in:
    import SerializerSpec.*
    val c = SerializerSpec.test
    val service = SerializerService()

    val os = new StringOutputStream()
    service.serialize(c, os)

    val replaceF1 = File("/tmp/replace-test1")
    val replaceF2 = File("/tmp/replace-test2")
    val replaceFGC = File("/tmp/replace-fgc")

    val replace = Map(c.f2.getCanonicalPath -> replaceF2, c.c1.f1.getCanonicalPath -> replaceF1, c.fgc.getCanonicalPath -> replaceFGC)

    val result = service.deserializeReplaceFiles[SerializerSpec.TestClass](new StringInputStream(os.builder.toString), replace)

    (result.f2 == replaceF2) shouldEqual true
    (result.c1.f1 == replaceF1) shouldEqual true
    (result.fgc == replaceFGC) shouldEqual true


  "Serializer" should "be able to list plugins" in:
    given TmpDirectory = TmpDirectory.stub()
    given KeyValueCache = KeyValueCache()

    import SerializerSpec.*
    val c = SerializerSpec.test
    val service = SerializerService()
    val plugins = service.listPluginsAndFiles(c).plugins
    plugins should contain(pluginFile)

