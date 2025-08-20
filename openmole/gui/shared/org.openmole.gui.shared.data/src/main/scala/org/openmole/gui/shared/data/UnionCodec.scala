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
    TypeRepr.of[X] match
      case OrType(l, r) =>
        (l.asType, r.asType) match
          case ('[a], '[b]) =>
            (Expr.summon[Encoder[a]], Expr.summon[Encoder[b]]) match
              case (Some(aInst), Some(bInst)) =>
                '{
                  Encoder.instance[X]:
                    case a: a => $aInst(a)
                    case b: b => $bInst(b)
                }.asExprOf[Encoder[X]]

  transparent inline given unionDecoder[X]: Decoder[X] = ${ unionDecoderImpl[X] }

  def unionDecoderImpl[X: Type](using Quotes): Expr[Decoder[X]] =
    import quotes.reflect.*
    TypeRepr.of[X] match
      case OrType(l, r) =>
        (l.asType, r.asType) match
          case ('[a], '[b]) =>
            (Expr.summon[Decoder[a]], Expr.summon[Decoder[b]]) match
              case (Some(aInst), Some(bInst)) =>
                '{
                  $aInst.widen.or($bInst.widen)
                }.asExprOf[Decoder[X]]