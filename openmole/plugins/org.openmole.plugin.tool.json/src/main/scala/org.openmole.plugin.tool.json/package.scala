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

  /*
  def toJSONdictionary(v: Any): String = {

  }
  */

  def jValueToVariable(jValue: JValue, v: Val[_]): Variable[_] = {
    import org.json4s._
    import shapeless._

    def cannotConvert = throw new UserBadDataError(s"Can not fetch value of type $jValue to OpenMOLE variable $v")

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
      case (value: JArray, Val.caseInt(v)) ⇒ Variable(v, jValueToInt(value.arr.head))
      case (value: JArray, Val.caseLong(v)) ⇒ Variable(v, jValueToLong(value.arr.head))
      case (value: JArray, Val.caseDouble(v)) ⇒ Variable(v, jValueToDouble(value.arr.head))
      case (value: JArray, Val.caseString(v)) ⇒ Variable(v, jValueToString(value.arr.head))
      case (value: JArray, Val.caseBoolean(v)) ⇒ Variable(v, jValueToBoolean(value.arr.head))

      case (value: JArray, Val.caseArrayInt(v)) ⇒ Variable(v, jValueToArray(value, jValueToInt))
      case (value: JArray, Val.caseArrayLong(v)) ⇒ Variable(v, jValueToArray(value, jValueToLong))
      case (value: JArray, Val.caseArrayDouble(v)) ⇒ Variable(v, jValueToArray(value, jValueToDouble))
      case (value: JArray, Val.caseArrayString(v)) ⇒ Variable(v, jValueToArray(value, jValueToString))
      case (value: JArray, Val.caseArrayBoolean(v)) ⇒ Variable(v, jValueToArray(value, jValueToBoolean))

      case (value: JArray, Val.caseArrayArrayInt(v)) ⇒ Variable(v, jValueToArray(value, jValueToArray(_, jValueToInt)))
      case (value: JArray, Val.caseArrayArrayLong(v)) ⇒ Variable(v, jValueToArray(value, jValueToArray(_, jValueToLong)))
      case (value: JArray, Val.caseArrayArrayDouble(v)) ⇒ Variable(v, jValueToArray(value, jValueToArray(_, jValueToDouble)))
      case (value: JArray, Val.caseArrayArrayString(v)) ⇒ Variable(v, jValueToArray(value, jValueToArray(_, jValueToString)))
      case (value: JArray, Val.caseArrayArrayBoolean(v)) ⇒ Variable(v, jValueToArray(value, jValueToArray(_, jValueToBoolean)))

      case (value: JValue, Val.caseInt(v)) ⇒ Variable(v, jValueToInt(value))
      case (value: JValue, Val.caseLong(v)) ⇒ Variable(v, jValueToLong(value))
      case (value: JValue, Val.caseDouble(v)) ⇒ Variable(v, jValueToDouble(value))
      case (value: JValue, Val.caseString(v)) ⇒ Variable(v, jValueToString(value))
      case (value: JValue, Val.caseBoolean(v)) ⇒ Variable(v, jValueToBoolean(value))

      case _ ⇒ cannotConvert
    }

  }

}
