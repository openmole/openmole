/**
 * Created by Romain Reuillon on 22/09/16.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.openmole.core

package argument:

  import org.openmole.core.context.Val
  import org.openmole.core.fileservice.FileService
  import org.openmole.core.workspace.TmpDirectory
  import org.openmole.tool.cache.KeyValueCache

  sealed trait Validate:
    def apply(inputs: Seq[Val[_]])(using TmpDirectory, FileService, KeyValueCache): Seq[Throwable]
    def ++(v: Validate) = Validate.++(this, v)

  object Validate:

    class Parameters(val inputs: Seq[Val[_]])(implicit val tmpDirectory: TmpDirectory, val fileService: FileService, val cache: KeyValueCache)

    case class LeafValidate(validate: Parameters ⇒ Seq[Throwable]) extends Validate:
      def apply(inputs: Seq[Val[_]])(implicit newFile: TmpDirectory, fileService: FileService, cache: KeyValueCache): Seq[Throwable] = validate(new Parameters(inputs))

    case class SeqValidate(validate: Seq[Validate]) extends Validate:
      def apply(inputs: Seq[Val[_]])(implicit newFile: TmpDirectory, fileService: FileService, cache: KeyValueCache): Seq[Throwable] = validate.flatMap(_.apply(inputs))

    def apply(f: Parameters ⇒ Seq[Throwable]): Validate = LeafValidate(f)
    def apply(vs: Validate*): Validate = SeqValidate(vs)

    def withExtraInputs(v: Validate, extraInputs: Seq[Val[_]] => Seq[Val[_]]): Validate = new Validate:
      def apply(inputs: Seq[Val[_]])(implicit newFile: TmpDirectory, fileService: FileService, cache: KeyValueCache): Seq[Throwable] = v(inputs ++ extraInputs(inputs))

    case object success extends Validate:
      def apply(inputs: Seq[Val[_]])(implicit newFile: TmpDirectory, fileService: FileService, cache: KeyValueCache): Seq[Throwable] = Seq()

    def ++(v1: Validate, v2: Validate) =
      (v1, v2) match {
        case (Validate.success, Validate.success) ⇒ Validate.success
        case (v, Validate.success)                ⇒ v
        case (Validate.success, v)                ⇒ v
        case (v1, v2)                             ⇒ SeqValidate(toIterable(v1).toSeq ++ toIterable(v2))
      }

    implicit def fromSeqValidate(v: Seq[Validate]): Validate = apply(v: _*)
    implicit def fromThrowables(t: Seq[Throwable]): Validate = Validate { _ ⇒ t }

    implicit def toIterable(v: Validate): Iterable[Validate] =
      v match
        case s: SeqValidate  ⇒ s.validate
        case l: LeafValidate ⇒ Iterable(l)
        case success         ⇒ Iterable.empty

  trait ExpansionPackage:
    implicit def seqToSeqOfFromContext[T](s: Seq[T])(implicit toFromContext: ToFromContext[T, T]): Seq[FromContext[T]] = s.map(e ⇒ toFromContext.convert(e))
    implicit def optionalArgumentToOption[T](optionalArgument: OptionalArgument[T]): Option[T] = optionalArgument.option

    type Condition = argument.Condition
    lazy val Condition = argument.Condition

    class ExpressionClass[T]:
      def apply[S](s: S)(implicit expandable: Expandable[S, T]) = expandable.expand(s)

    def Expression[T] = new ExpressionClass[T]

  type Condition = FromContext[Boolean]

