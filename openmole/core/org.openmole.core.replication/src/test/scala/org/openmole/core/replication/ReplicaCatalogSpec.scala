package org.openmole.core.replication

import org.scalatest._
import org.openmole.tool.file._
import org.openmole.tool.hash._
import org.openmole.core.preference._
import org.openmole.core.workspace._

class ReplicaCatalogSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers:

  "ReplicaCatalog" should "store replicas" in:
    val workspace =
      val dir = java.io.File.createTempFile("test", "")
      dir.delete()
      dir.mkdirs()
      Workspace(dir)

    given Preference = Preference.memory()
    val catalog = ReplicaCatalog(workspace)
    given TmpDirectory = TmpDirectory(workspace)

    val file = TmpDirectory.newFile("replica", ".txt")
    file.content = "This is a test"

    val hash: String = file.hash().toString()
    val storageID = "test"
    val path = "replica/test"

    val replica = catalog.uploadAndGet(path, s => true, s => (), file, hash, storageID)

    catalog.forHashes(Seq(hash), Seq(storageID)).head.source should equal(file.getCanonicalPath)
    catalog.forPaths(Seq(path), Seq(storageID)).head.hash should equal(hash)

    catalog.uploadAndGet(path, s => true, s => (), file, hash, storageID).id should equal(replica.id)
    catalog.remove(replica.id)

    catalog.forHashes(Seq(hash), Seq(storageID)).isEmpty should equal(true)

