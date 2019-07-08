package org.openmole.core.workflow

import org.openmole.core.expansion.FromContext
import org.openmole.core.fileservice.FileService
import org.openmole.core.workspace.NewFile

package object validation {
  trait Validate {
    def apply(implicit newFile: NewFile, fileService: FileService): Seq[Throwable]
  }

  object Validate {
    case class Parameters(implicit val newFile: NewFile, implicit val fileService: FileService)

    def apply(f: Parameters ⇒ Seq[Throwable]): Validate = new Validate {
      def apply(implicit newFile: NewFile, fileService: FileService) = f(Parameters())
    }

    def apply(vs: Validate*): Validate = new Validate {
      override def apply(implicit newFile: NewFile, fileService: FileService): Seq[Throwable] = vs.flatMap(_.apply)
    }

    def success = Validate { _ ⇒ Seq.empty }

    implicit def fromThrowables(t: Seq[Throwable]) = Validate { _ ⇒ t }
  }

  trait ValidationPackage
}
