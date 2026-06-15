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
//    val vt = ValType[Array[Int]]
//
//    vt.compileName.arrayLevel shouldEqual 1
//    vt.compileName.name() shouldEqual "Array[Int]"
//
//    def f[A: ValTag as a, B: ValTag as b] =
//      import org.openmole.tool.types.*
//      import a.*
//      import b.*
//      ValTag[A => B]()

    type I = ValSpec.C[Int]
    inline def s[T](using inline t: T) = t
    def iTag: ValTag[I] = s //implicitly[ValTag[ValSpec.C[Int]]]
    val valTag: ValTag[I] = ValTag[I]
    println(valTag.m)
    iTag.m shouldEqual manifest[ValSpec.C[I]]
