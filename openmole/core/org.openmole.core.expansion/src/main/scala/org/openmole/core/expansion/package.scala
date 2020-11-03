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

package expansion {

  import org.openmole.core.fileservice.FileService
  import org.openmole.core.workspace.TmpDirectory

  trait Validate {
    def apply(implicit newFile: TmpDirectory, fileService: FileService): Seq[Throwable]
    def ++(v: Validate) = Validate(this, v)
  }

  object Validate {

    class Parameters(implicit val tmpDirectory: TmpDirectory, implicit val fileService: FileService)

    case class LeafValidate(validate: Parameters ⇒ Seq[Throwable]) extends Validate {
      def apply(implicit newFile: TmpDirectory, fileService: FileService): Seq[Throwable] = validate(new Parameters())
    }

    case class SeqValidate(validate: Seq[Validate]) extends Validate {
      def apply(implicit newFile: TmpDirectory, fileService: FileService): Seq[Throwable] = validate.flatMap(_.apply)
    }

    def apply(f: Parameters ⇒ Seq[Throwable]): Validate = LeafValidate(f)
    def apply(vs: Validate*): Validate = SeqValidate(vs)

    def success = Validate { _ ⇒ Seq.empty }

    implicit def fromSeqValidate(v: Seq[Validate]) = apply(v: _*)
    implicit def fromThrowables(t: Seq[Throwable]) = Validate { _ ⇒ t }

    implicit def toIterable(v: Validate) =
      v match {
        case s: SeqValidate  ⇒ s.validate
        case l: LeafValidate ⇒ Iterable(l)
      }
  }

  trait ExpansionPackage {
    implicit def seqToSeqOfFromContext[T](s: Seq[T])(implicit toFromContext: ToFromContext[T, T]): Seq[FromContext[T]] = s.map(e ⇒ toFromContext(e))
    type Condition = expansion.Condition
    lazy val Condition = expansion.Condition
  }

}

package object expansion {
  type Condition = FromContext[Boolean]
}
