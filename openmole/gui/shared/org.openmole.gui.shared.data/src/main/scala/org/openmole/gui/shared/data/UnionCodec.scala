package org.openmole.gui.shared.data

/*
 * Copyright (C) 2025 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

object UnionCodec:

  import cats.syntax.functor.*
  import io.circe.{Encoder, Decoder, Json}
  import scala.quoted.*

  transparent inline given unionEncoder[X]: Encoder[X] = ${unionEncnoderImpl[X]}

  def unionEncnoderImpl[X: Type](using Quotes): Expr[Encoder[X]] =
    import quotes.reflect.*

    def handleType(tpe: TypeRepr): Expr[Any => Json] = // Use function type instead
      tpe.dealias.simplified match
        case OrType(l, r) =>
          // Recursively handle nested unions
          val leftEncoder = handleType(l)
          val rightEncoder = handleType(r)

          (l.asType, r.asType) match
            case ('[a], '[b]) =>
              '{
                (x: Any) =>
                  x match
                    case a: a => $leftEncoder(a)
                    case b: b => $rightEncoder(b)
              }

        case simpleType =>
          simpleType.asType match
            case '[t] =>
              Expr.summon[Encoder[t]] match
                case Some(encoder) =>
                  '{ (x: Any) => $encoder(x.asInstanceOf[t]) }
                case None => report.errorAndAbort(s"No encoder found for type ${simpleType.show}")

    TypeRepr.of[X].dealias match
      case OrType(l, r) =>
        val encoderFunc = handleType(TypeRepr.of[X])
        '{
          Encoder.instance[X]: x =>
            $encoderFunc(x)
        }.asExprOf[Encoder[X]]

      case other =>
        report.errorAndAbort(s"Expected union type, got: ${other.show}")


  transparent inline given unionDecoder[X]: Decoder[X] = ${ unionDecoderImpl[X] }

  def unionDecoderImpl[X: Type](using Quotes): Expr[Decoder[X]] =
    import quotes.reflect.*

    def handleType(tpe: TypeRepr): Expr[Decoder[Any]] =
      tpe.dealias.simplified match
        case OrType(l, r) =>
          // Recursively handle nested unions
          val leftDecoder = handleType(l)
          val rightDecoder = handleType(r)
          '{
            $leftDecoder.widen.or($rightDecoder.widen)
          }
        case simpleType =>
          simpleType.asType match
            case '[t] =>
              Expr.summon[Decoder[t]] match
                case Some(decoder) =>
                  '{ $decoder.widen[Any] }
                case None =>
                  report.errorAndAbort(s"No decoder found for type ${simpleType.show}")

    TypeRepr.of[X].dealias match
      case OrType(l, r) =>
        val decoder = handleType(TypeRepr.of[X])
        '{
          $decoder.asInstanceOf[Decoder[X]]
        }
      case other =>
        report.errorAndAbort(s"Expected union type, got: ${other.show}")