package org.openmole.core

import org.openmole.core.context.*
import org.openmole.core.exception.UserBadDataError

package object json:

  import com.fasterxml.jackson.core.json.JsonReadFeature
  import com.fasterxml.jackson.databind.json.JsonMapper
  import com.fasterxml.jackson.module.scala.DefaultScalaModule
  import org.json4s.JsonAST.{JObject, JValue}

  // Allow NaN value for numbers in JSON files
  org.json4s.jackson.JsonMethods.mapper.enable(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS.mappedFeature())

  import io.circe.*

  given Encoder[java.io.File] = f => Json.fromString(f.getAbsolutePath)
  given Decoder[java.io.File] = j => j.as[String].map(new java.io.File(_))

  def variablesToJObject(variables: Seq[Variable[?]], default: Option[Any => org.json4s.JValue] = None, file: Option[java.io.File => org.json4s.JValue] = None) =
    JObject(variables.toList.map { v => v.name -> toJSONValue(v.value, Some(v), default = default, file = file) })

  def variablesToJValues(variables: Seq[Variable[?]], default: Option[Any => org.json4s.JValue] = None, file: Option[java.io.File => org.json4s.JValue] = None): Seq[JValue] =
    variables.toList.map { v => toJSONValue(v.value, Some(v), default = default, file = file) }

  def toJSONValue(v: Any, variable: Option[Variable[?]] = None, default: Option[Any => org.json4s.JValue] = None, file: Option[java.io.File => org.json4s.JValue] = None): org.json4s.JValue =
    import org.json4s.*

    v match
      case v: Int          => JInt(v)
      case v: Long         => JLong(v)
      case v: String       => JString(v)
      case v: Float        => JDouble(v)
      case v: Double       => JDouble(v)
      case v: Boolean      => JBool(v)
      case v: Array[?]     => JArray(v.map(v => toJSONValue(v, variable, default = default, file = file)).toList)
      case v: java.io.File => file.map(_(v)).getOrElse(JString(v.getAbsolutePath))
      case v: Seq[_]       => JArray(v.map(v => toJSONValue(v, variable, default = default, file = file)).toList)
      case _               =>
        default match
          case None =>
            variable match
              case Some(variable) => throw new UserBadDataError(s"Value $v of type ${v.getClass} from variable $variable is not convertible to JSON")
              case None => throw new UserBadDataError(s"Value $v of type ${v.getClass} is not convertible to JSON")
          case Some(serialize) => serialize(v)

  def cannotConvertFromJSON[T: Manifest](jValue: JValue) = throw new UserBadDataError(s"Can not fetch value of type $jValue to type ${manifest[T]}")

  def jValueToVariable(
    jValue: JValue,
    v: Val[?],
    unwrapArrays: Boolean = false,
    default: Option[(JValue, Class[?]) => Any] = None,
    file: Option[JValue => java.io.File] = None): Variable[?] =
    import org.json4s.*

    def jValueToInt(jv: JValue) =
      jv match
        case jv: JDouble  => jv.num.intValue
        case jv: JInt     => jv.num.intValue
        case jv: JLong    => jv.num.intValue
        case jv: JDecimal => jv.num.intValue
        case jv: JString  => jv.s.toInt
        case _            => cannotConvertFromJSON[Int](jv)

    def jValueToLong(jv: JValue) =
      jv match
        case jv: JDouble  => jv.num.longValue
        case jv: JInt     => jv.num.longValue
        case jv: JLong    => jv.num.longValue
        case jv: JDecimal => jv.num.longValue
        case jv: JString  => jv.s.toLong
        case _            => cannotConvertFromJSON[Long](jv)

    def jValueToDouble(jv: JValue) =
      jv match
        case jv: JDouble  => jv.num.doubleValue
        case jv: JInt     => jv.num.doubleValue
        case jv: JLong    => jv.num.doubleValue
        case jv: JDecimal => jv.num.doubleValue
        case jv: JString  => jv.s.toDouble
        case _            => cannotConvertFromJSON[Double](jv)

    def jValueToString(jv: JValue) =
      jv match
        case jv: JDouble  => jv.num.toString
        case jv: JInt     => jv.num.toString
        case jv: JLong    => jv.num.toString
        case jv: JDecimal => jv.num.toString
        case jv: JString  => jv.s
        case _            => cannotConvertFromJSON[String](jv)

    def jValueToBoolean(jv: JValue) =
      jv match
        case jv: JBool => jv.value
        case _         => cannotConvertFromJSON[Boolean](jv)

    def jValueToFile(jv: JValue) =
      file.map(_(jv)).getOrElse:
        new java.io.File(jValueToString(jv))

    (jValue, v) match
      case (value: JArray, Val.caseInt(v)) if unwrapArrays     => Variable(v, jValueToInt(value.arr.head))
      case (value: JArray, Val.caseLong(v)) if unwrapArrays    => Variable(v, jValueToLong(value.arr.head))
      case (value: JArray, Val.caseDouble(v)) if unwrapArrays  => Variable(v, jValueToDouble(value.arr.head))
      case (value: JArray, Val.caseString(v)) if unwrapArrays  => Variable(v, jValueToString(value.arr.head))
      case (value: JArray, Val.caseBoolean(v)) if unwrapArrays => Variable(v, jValueToBoolean(value.arr.head))

      case (value: JArray, v) =>
        import scala.jdk.CollectionConverters._

        def jValueToValue(value: Any, arrayType: Class[?]) =
          (value, arrayType) match
            case (value: JValue, c) if c == classOf[Double] => jValueToDouble(value)
            case (value: JValue, c) if c == classOf[Int] => jValueToInt(value)
            case (value: JValue, c) if c == classOf[Long] => jValueToLong(value)
            case (value: JValue, c) if c == classOf[Boolean] => jValueToBoolean(value)
            case (value: JValue, c) if c == classOf[String] => jValueToString(value)
            case (value: JValue, c) if c == classOf[java.io.File] => jValueToFile(value)
            case (jValue, c) =>
              (jValue, default) match
                case (value: JValue, Some(serializer)) => serializer(value, arrayType)
                case _ => throw new UserBadDataError(s"Can not fetch value of type $jValue to type ${c}")

        given Variable.ConstructArray[JArray] =
          new Variable.ConstructArray[JArray]:
            def size(t: JArray) = t.arr.size
            def iterable(t: JArray) = t.arr.asJava.asInstanceOf[java.lang.Iterable[Any]]

        Variable.constructArray(v, value, jValueToValue)

      case (value: JValue, Val.caseInt(v))     => Variable(v, jValueToInt(value))
      case (value: JValue, Val.caseLong(v))    => Variable(v, jValueToLong(value))
      case (value: JValue, Val.caseDouble(v))  => Variable(v, jValueToDouble(value))
      case (value: JValue, Val.caseString(v))  => Variable(v, jValueToString(value))
      case (value: JValue, Val.caseBoolean(v)) => Variable(v, jValueToBoolean(value))
      case (value: JValue, Val.caseFile(v))    => Variable(v, jValueToFile(value))
      case (value, v) =>
        (value, v, default) match
          case (value: JValue, v, Some(serializer)) => Variable.unsecureUntyped(v, serializer(value, v.`type`.runtimeClass))
          case _                                    => throw new UserBadDataError(s"Can not fetch value of type $jValue to OpenMOLE variable ${v}")


  lazy val objectMapper =
    import org.json4s.jackson.Json4sScalaModule
    import com.fasterxml.jackson.core.*
    import com.fasterxml.jackson.core.json.*
    import com.fasterxml.jackson.databind.*
    import com.fasterxml.jackson.module.scala.*

    val m = JsonMapper.builder()
      .addModule(DefaultScalaModule)
      .configure(JsonParser.Feature.USE_FAST_DOUBLE_PARSER, true)
      .configure(JsonParser.Feature.USE_FAST_BIG_NUMBER_PARSER, true)
      .configure(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS, true)
      .build()

    m.registerModule(new Json4sScalaModule)
    m

  def anyToJValue(a: Any): org.json4s.JValue =
    jsonParser.parse(objectMapper.writeValueAsString(a))

  def jValueToAny(value: JValue, clazz: Class[?]): Any =
    import org.json4s.jackson.JsonMethods.*
    objectMapper.readValue(compact(render(value)), clazz)


  lazy val jsonParser =
    import com.fasterxml.jackson.databind.*

    val jsonMethods = new org.json4s.jackson.JsonMethods:
      override def mapper = objectMapper

    jsonMethods


//  object jsoniter:
//    import com.jsoniter.*
//    export com.jsoniter.JsonIterator
//    import com.jsoniter.spi.DecodingMode
//
//    com.jsoniter.JsonIterator.setMode(DecodingMode.STATIC_MODE)
//
//    def withJsonIterator[T](is: java.io.InputStream)(f: JsonIterator => T) =
//      org.openmole.tool.thread.withThreadClassLoader(classOf[com.jsoniter.JsonIterator].getClassLoader):
//        val it = JsonIterator.parse(is, org.openmole.tool.stream.DefaultBufferSize)
//        try f(it)
//        finally it.close()
//
//    type JAny = com.jsoniter.any.Any
//
//    def cannotConvertFromJSON[T: Manifest](value: JAny) = throw new UserBadDataError(s"Can not fetch value of type $value to type ${manifest[T]}")
//
//    def anyToAny(value: JAny, clazz: Class[?]): Any =
//      clazz match
//        case c if c == classOf[String] => value.toString
//        case c if c == classOf[Int] => value.toInt
//        case c if c == classOf[Long] => value.toLong
//        case c if c == classOf[Double] => value.toDouble
//        case c if c == classOf[Boolean] => value.toBoolean
//        case c if c == classOf[java.io.File] => new java.io.File(value.toString)
//        case _ => JsonIterator.deserialize(value.toString, clazz)
//
//    def anyToVariable(
//      any: JAny,
//      v: Val[?],
//      unwrapArrays: Boolean = false,
//      default: Option[(JAny, Class[?]) => Any] = None,
//      file: Option[JAny => java.io.File] = None): Variable[?] =
//
//      def anyToInt(a: JAny): Int =
//        a.valueType match
//          case ValueType.NUMBER => a.toInt
//          case ValueType.STRING => a.toString.toInt
//          case _ => cannotConvertFromJSON[Int](a)
//
//      def anyToLong(a: JAny): Long =
//        a.valueType match
//          case ValueType.NUMBER => a.toLong
//          case ValueType.STRING => a.toString.toLong
//          case _ => cannotConvertFromJSON[Long](a)
//
//      def anyToDouble(a: JAny): Double =
//        a.valueType match
//          case ValueType.NUMBER => a.toDouble
//          case ValueType.STRING => a.toString.toDouble
//          case _ => cannotConvertFromJSON[Double](a)
//
//      def anyToString(a: JAny): String =
//        a.valueType match
//          case ValueType.STRING => a.toString
//          case ValueType.NUMBER => a.toString
//          case ValueType.BOOLEAN => a.toBoolean.toString
//          case _ => cannotConvertFromJSON[String](a)
//
//      def anyToBoolean(a: JAny): Boolean =
//        a.valueType match
//          case ValueType.BOOLEAN => a.toBoolean
//          case _ => cannotConvertFromJSON[Boolean](a)
//
//      def anyToFile(a: JAny): java.io.File =
//        file.map(_(a)).getOrElse(new java.io.File(anyToString(a)))
//
//      (any, v) match
//        // ---------- unwrap single-element arrays ----------
//        case (a, Val.caseInt(v)) if unwrapArrays && a.valueType == ValueType.ARRAY =>
//          Variable(v, anyToInt(a.iterator.next()))
//
//        case (a, Val.caseLong(v)) if unwrapArrays && a.valueType == ValueType.ARRAY =>
//          Variable(v, anyToLong(a.iterator.next()))
//
//        case (a, Val.caseDouble(v)) if unwrapArrays && a.valueType == ValueType.ARRAY =>
//          Variable(v, anyToDouble(a.iterator.next()))
//
//        case (a, Val.caseString(v)) if unwrapArrays && a.valueType == ValueType.ARRAY =>
//          Variable(v, anyToString(a.iterator.next()))
//
//        case (a, Val.caseBoolean(v)) if unwrapArrays && a.valueType == ValueType.ARRAY =>
//          Variable(v, anyToBoolean(a.iterator.next()))
//
//        // ---------- array case ----------
//        case (a, v) if a.valueType == ValueType.ARRAY =>
//
//          def anyToValue(value: Any, arrayType: Class[?]): Any =
//            (value, arrayType) match
//              case (value: JAny, c) if c == classOf[Double] => anyToDouble(value)
//              case (value: JAny, c) if c == classOf[Int] => anyToInt(value)
//              case (value: JAny, c) if c == classOf[Long] => anyToLong(value)
//              case (value: JAny, c) if c == classOf[Boolean] => anyToBoolean(value)
//              case (value: JAny, c) if c == classOf[String] => anyToString(value)
//              case (value: JAny, c) if c == classOf[java.io.File] => anyToFile(value)
//              case _ =>
//                (value, default) match
//                  case (value: JAny, Some(serializer)) => serializer(value, arrayType)
//                  case (value, _) =>
//                    throw new UserBadDataError(s"Can not fetch value of type $value to type $arrayType")
//
//          given Variable.ConstructArray[JAny] =
//            new Variable.ConstructArray[JAny]:
//              def size(t: JAny): Int = t.size
//
//              def iterable(t: JAny): java.lang.Iterable[Any] =
//                new java.lang.Iterable[Any]:
//                  def iterator() = t.iterator().asInstanceOf[java.util.Iterator[Any]]
//
//          Variable.constructArray(v, a, anyToValue)
//
//        // ---------- scalar cases ----------
//        case (a, Val.caseInt(v)) => Variable(v, anyToInt(a))
//        case (a, Val.caseLong(v)) => Variable(v, anyToLong(a))
//        case (a, Val.caseDouble(v)) => Variable(v, anyToDouble(a))
//        case (a, Val.caseString(v)) => Variable(v, anyToString(a))
//        case (a, Val.caseBoolean(v)) => Variable(v, anyToBoolean(a))
//        case (a, Val.caseFile(v)) => Variable(v, anyToFile(a))
//
//        // ---------- fallback ----------
//        case (a, v) =>
//          default match
//            case Some(serializer) =>
//              Variable.unsecureUntyped(v, serializer(a, v.`type`.runtimeClass))
//            case None =>
//              throw new UserBadDataError(s"Can not fetch value of type $a to OpenMOLE variable $v")

