package org.openmole.plugin.tool

import com.fasterxml.jackson.core.json.JsonReadFeature
import org.json4s.JsonAST.{JObject, JValue}
import org.openmole.core.context.*
import org.openmole.core.exception.UserBadDataError

package object json:
  // Allow NaN value for numbers in JSON files
  org.json4s.jackson.JsonMethods.mapper.enable(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS.mappedFeature())

  import io.circe.*

  given Encoder[java.io.File] = f => Json.fromString(f.getAbsolutePath)
  given Decoder[java.io.File] = j => j.as[String].map(new java.io.File(_))

  def variablesToJValue(variables: Seq[Variable[_]]) =
    JObject(variables.toList.map { v ⇒ v.name -> toJSONValue(v.value, Some(v)) })

  def toJSONValue(v: Any, variable: Option[Variable[_]] = None): org.json4s.JValue = {
    import org.json4s._

    v match {
      case v: Int          ⇒ JInt(v)
      case v: Long         ⇒ JLong(v)
      case v: String       ⇒ JString(v)
      case v: Float        ⇒ JDouble(v)
      case v: Double       ⇒ JDouble(v)
      case v: Boolean      ⇒ JBool(v)
      case v: Array[_]     ⇒ JArray(v.map(v => toJSONValue(v, variable)).toList)
      case v: java.io.File ⇒ JString(v.getAbsolutePath)
      case v: Seq[_]       ⇒ JArray(v.map(v => toJSONValue(v, variable)).toList)
      case _               ⇒ 
        variable match 
          case Some(variable) => throw new UserBadDataError(s"Value $v of type ${v.getClass} from variable $variable is not convertible to JSON")
          case None => throw new UserBadDataError(s"Value $v of type ${v.getClass} is not convertible to JSON")
    }
  }

  def jValueToVariable(jValue: JValue, v: Val[_], unwrapArrays: Boolean = false): Variable[_] = {
    import org.json4s._

    def cannotConvert[T: Manifest](jValue: JValue) = throw new UserBadDataError(s"Can not fetch value of type $jValue to type ${manifest[T]}")

    def jValueToInt(jv: JValue) =
      jv match {
        case jv: JDouble  ⇒ jv.num.intValue
        case jv: JInt     ⇒ jv.num.intValue
        case jv: JLong    ⇒ jv.num.intValue
        case jv: JDecimal ⇒ jv.num.intValue
        case _            ⇒ cannotConvert[Int](jv)
      }

    def jValueToLong(jv: JValue) =
      jv match {
        case jv: JDouble  ⇒ jv.num.longValue
        case jv: JInt     ⇒ jv.num.longValue
        case jv: JLong    ⇒ jv.num.longValue
        case jv: JDecimal ⇒ jv.num.longValue
        case _            ⇒ cannotConvert[Long](jv)
      }

    def jValueToDouble(jv: JValue) =
      jv match {
        case jv: JDouble  ⇒ jv.num.doubleValue
        case jv: JInt     ⇒ jv.num.doubleValue
        case jv: JLong    ⇒ jv.num.doubleValue
        case jv: JDecimal ⇒ jv.num.doubleValue
        case _            ⇒ cannotConvert[Double](jv)
      }

    def jValueToString(jv: JValue) =
      jv match {
        case jv: JDouble  ⇒ jv.num.toString
        case jv: JInt     ⇒ jv.num.toString
        case jv: JLong    ⇒ jv.num.toString
        case jv: JDecimal ⇒ jv.num.toString
        case jv: JString  ⇒ jv.s
        case _            ⇒ cannotConvert[String](jv)
      }

    def jValueToBoolean(jv: JValue) =
      jv match {
        case jv: JBool ⇒ jv.value
        case _         ⇒ cannotConvert[Boolean](jv)
      }

    (jValue, v) match {
      case (value: JArray, Val.caseInt(v)) if unwrapArrays     ⇒ Variable(v, jValueToInt(value.arr.head))
      case (value: JArray, Val.caseLong(v)) if unwrapArrays    ⇒ Variable(v, jValueToLong(value.arr.head))
      case (value: JArray, Val.caseDouble(v)) if unwrapArrays  ⇒ Variable(v, jValueToDouble(value.arr.head))
      case (value: JArray, Val.caseString(v)) if unwrapArrays  ⇒ Variable(v, jValueToString(value.arr.head))
      case (value: JArray, Val.caseBoolean(v)) if unwrapArrays ⇒ Variable(v, jValueToBoolean(value.arr.head))

      case (value: JArray, v) ⇒
        import scala.jdk.CollectionConverters._

        def jValueToValue(value: Any, arrayType: Class[_]) =
          (value, arrayType) match {
            case (value: JValue, c) if c == classOf[Double] ⇒ jValueToDouble(value)
            case (value: JValue, c) if c == classOf[Int] ⇒ jValueToInt(value)
            case (value: JValue, c) if c == classOf[Long] ⇒ jValueToLong(value)
            case (value: JValue, c) if c == classOf[Boolean] ⇒ jValueToBoolean(value)
            case (value: JValue, c) if c == classOf[String] ⇒ jValueToString(value)
            case c ⇒ throw new UserBadDataError(s"Can not fetch value of type $jValue to type ${c}")
          }

        implicit def jArrayConstruct: Variable.ConstructArray[JArray] =
          new Variable.ConstructArray[JArray] {
            def size(t: JArray) = t.arr.size
            def iterable(t: JArray) = t.arr.asJava.asInstanceOf[java.lang.Iterable[Any]]
          }

        Variable.constructArray(v, value, jValueToValue)

      case (value: JValue, Val.caseInt(v))     ⇒ Variable(v, jValueToInt(value))
      case (value: JValue, Val.caseLong(v))    ⇒ Variable(v, jValueToLong(value))
      case (value: JValue, Val.caseDouble(v))  ⇒ Variable(v, jValueToDouble(value))
      case (value: JValue, Val.caseString(v))  ⇒ Variable(v, jValueToString(value))
      case (value: JValue, Val.caseBoolean(v)) ⇒ Variable(v, jValueToBoolean(value))
      case (value: JValue, Val.caseFile(v))    ⇒ Variable(v, new java.io.File(jValueToString(value)))

      case _                                   ⇒ throw new UserBadDataError(s"Can not fetch value of type $jValue to OpenMOLE variable ${v}")
    }

  }
  