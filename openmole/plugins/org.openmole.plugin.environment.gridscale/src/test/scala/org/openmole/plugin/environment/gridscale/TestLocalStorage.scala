package org.openmole.plugin.environment.gridscale
import gridscale.local
import org.openmole.plugin.environment.batch.storage._
import org.openmole.tool.file._

object TestLocalStorage extends App {
  implicit val intp = local.LocalInterpreter()

  val l = LocalStorage("/tmp/")
  val src = File("/tmp/testsrc.txt")
  src.content = "youpi"
  val dest = implicitly[StorageInterface[LocalStorage]].child(l, l.root, "testdest.gz")
  implicitly[StorageInterface[LocalStorage]].upload(l, src, dest)
}
