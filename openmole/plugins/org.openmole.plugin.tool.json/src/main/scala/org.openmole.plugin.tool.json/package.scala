package org.openmole.plugin.tool

import org.json4s.JsonAST.JValue
import org.openmole.core.context._
import org.openmole.core.exception.UserBadDataError
import shapeless._

package object json {

  def toJSONValue(v: Any): org.json4s.JValue = {
    import org.json4s._

    v match {
      case v: Int      ⇒ JInt(v)
      case v: Long     ⇒ JLong(v)
      case v: String   ⇒ JString(v)
      case v: Float    ⇒ JDouble(v)
      case v: Double   ⇒ JDouble(v)
      case v: Array[_] ⇒ JArray(v.map(toJSONValue).toList)
      case _           ⇒ throw new UserBadDataError(s"Value $v of type ${v.getClass} is not convertible to JSON")
    }
  }

  def jValueToVariable(jValue: JValue, v: Val[_]): Variable[_] = {
    import org.json4s._
    import shapeless._

    val caseBoolean = TypeCase[Val[Boolean]]
    val caseInt = TypeCase[Val[Int]]
    val caseLong = TypeCase[Val[Long]]
    val caseDouble = TypeCase[Val[Double]]
    val caseString = TypeCase[Val[String]]

    val caseArrayBoolean = TypeCase[Val[Array[Boolean]]]
    val caseArrayInt = TypeCase[Val[Array[Int]]]
    val caseArrayLong = TypeCase[Val[Array[Long]]]
    val caseArrayDouble = TypeCase[Val[Array[Double]]]
    val caseArrayString = TypeCase[Val[Array[String]]]

    val caseArrayArrayBoolean = TypeCase[Val[Array[Array[Boolean]]]]
    val caseArrayArrayInt = TypeCase[Val[Array[Array[Int]]]]
    val caseArrayArrayLong = TypeCase[Val[Array[Array[Long]]]]
    val caseArrayArrayDouble = TypeCase[Val[Array[Array[Double]]]]
    val caseArrayArrayString = TypeCase[Val[Array[Array[String]]]]

    def cannotConvert = throw new UserBadDataError(s"Can not convert value of type $jValue to Int for OpenMOLE variable $v.")

    def jValueToInt(jv: JValue) =
      jv match {
        case jv: JDouble  ⇒ jv.num.intValue
        case jv: JInt     ⇒ jv.num.intValue
        case jv: JLong    ⇒ jv.num.intValue
        case jv: JDecimal ⇒ jv.num.intValue
        case _            ⇒ cannotConvert
      }

    def jValueToLong(jv: JValue) =
      jv match {
        case jv: JDouble  ⇒ jv.num.longValue
        case jv: JInt     ⇒ jv.num.longValue
        case jv: JLong    ⇒ jv.num.longValue
        case jv: JDecimal ⇒ jv.num.longValue
        case _            ⇒ cannotConvert
      }

    def jValueToDouble(jv: JValue) =
      jv match {
        case jv: JDouble  ⇒ jv.num.doubleValue
        case jv: JInt     ⇒ jv.num.doubleValue
        case jv: JLong    ⇒ jv.num.doubleValue
        case jv: JDecimal ⇒ jv.num.doubleValue
        case _            ⇒ cannotConvert
      }

    def jValueToString(jv: JValue) =
      jv match {
        case jv: JDouble  ⇒ jv.num.toString
        case jv: JInt     ⇒ jv.num.toString
        case jv: JLong    ⇒ jv.num.toString
        case jv: JDecimal ⇒ jv.num.toString
        case jv: JString  ⇒ jv.s
        case _            ⇒ cannotConvert
      }

    def jValueToBoolean(jv: JValue) =
      jv match {
        case jv: JBool ⇒ jv.value
        case _         ⇒ cannotConvert
      }

    def jValueToArray[T: Manifest](jv: JValue, convert: JValue ⇒ T) =
      jv match {
        case jv: JArray ⇒ jv.arr.map(convert).toArray[T]
        case _          ⇒ cannotConvert
      }

    (jValue, v) match {
      case (value: JArray, caseInt(v))               ⇒ Variable(v, jValueToInt(value.arr.head))
      case (value: JArray, caseLong(v))              ⇒ Variable(v, jValueToLong(value.arr.head))
      case (value: JArray, caseDouble(v))            ⇒ Variable(v, jValueToDouble(value.arr.head))
      case (value: JArray, caseString(v))            ⇒ Variable(v, jValueToString(value.arr.head))
      case (value: JArray, caseBoolean(v))           ⇒ Variable(v, jValueToBoolean(value.arr.head))

      case (value: JArray, caseArrayInt(v))          ⇒ Variable(v, jValueToArray(value, jValueToInt))
      case (value: JArray, caseArrayLong(v))         ⇒ Variable(v, jValueToArray(value, jValueToLong))
      case (value: JArray, caseArrayDouble(v))       ⇒ Variable(v, jValueToArray(value, jValueToDouble))
      case (value: JArray, caseArrayString(v))       ⇒ Variable(v, jValueToArray(value, jValueToString))
      case (value: JArray, caseArrayBoolean(v))      ⇒ Variable(v, jValueToArray(value, jValueToBoolean))

      case (value: JArray, caseArrayArrayInt(v))     ⇒ Variable(v, jValueToArray(value, jValueToArray(_, jValueToInt)))
      case (value: JArray, caseArrayArrayLong(v))    ⇒ Variable(v, jValueToArray(value, jValueToArray(_, jValueToLong)))
      case (value: JArray, caseArrayArrayDouble(v))  ⇒ Variable(v, jValueToArray(value, jValueToArray(_, jValueToDouble)))
      case (value: JArray, caseArrayArrayString(v))  ⇒ Variable(v, jValueToArray(value, jValueToArray(_, jValueToString)))
      case (value: JArray, caseArrayArrayBoolean(v)) ⇒ Variable(v, jValueToArray(value, jValueToArray(_, jValueToBoolean)))

      case _                                         ⇒ cannotConvert
    }

  }

}
