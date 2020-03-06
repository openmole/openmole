package org.openmole.core.workflow

import org.openmole.core.expansion.FromContext
import org.openmole.core.fileservice.FileService
import org.openmole.core.workspace.TmpDirectory

package object validation {
  trait Validate {
    def apply(implicit newFile: TmpDirectory, fileService: FileService): Seq[Throwable]
    def ++(v: Validate) = Validate(this, v)
  }

  object Validate {
    case class Parameters(implicit val newFile: TmpDirectory, implicit val fileService: FileService)

    def apply(f: Parameters ⇒ Seq[Throwable]): Validate = new Validate {
      def apply(implicit newFile: TmpDirectory, fileService: FileService) = f(Parameters())
    }

    def apply(vs: Validate*): Validate = new Validate {
      override def apply(implicit newFile: TmpDirectory, fileService: FileService): Seq[Throwable] = vs.flatMap(_.apply)
    }

    def success = Validate { _ ⇒ Seq.empty }

    implicit def fromThrowables(t: Seq[Throwable]) = Validate { _ ⇒ t }
  }

  trait ValidationPackage
}
