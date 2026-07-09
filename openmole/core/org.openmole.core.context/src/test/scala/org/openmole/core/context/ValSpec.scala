package org.openmole.core.context

import org.scalatest.*

object ValSpec:
  class C[T]


class ValSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers:

  "ValTypeName" should "parse the following" in:
    def testEqual(t: String) =
      ValType.TypeName.parse(t).name() shouldEqual t

    testEqual("scala.collection.immutable.Map[java.lang.String, List[Int]]")
    testEqual("Array[Array[Int]]")
    testEqual("Array[Map[Array[Int], String]]")


  it should "build the following" in:
    val vt = ValType[Array[Int]]

    vt.compileName.arrayLevel shouldEqual 1
    vt.compileName.name() shouldEqual "Array[Int]"

    def f[A: ValTag as a, B: ValTag as b]: ValTag[A => B] = implicitly

    type I = ValSpec.C[Int]
    val valTag: ValTag[I] = ValTag[I]
    valTag shouldEqual ValTag[ValSpec.C[Int]]

  it should "produce the correct manifest" in:
    import org.openmole.tool.types.TypeTool
    val runtimeManifest = TypeTool.toManifest("scala.collection.immutable.Map[Int, Array[Double]]", this.getClass.getClassLoader)

    val vt = ValType[Map[Int, Array[Double]]]
    vt.manifest shouldEqual manifest[Map[Int, Array[Double]]]

