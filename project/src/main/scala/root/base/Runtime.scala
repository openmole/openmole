package root.base

import sbt._
import root.libraries._

package object runtime extends BaseDefaults {
  implicit val artifactPrefix = Some("org.openmole.runtime")

  override def dir = super.dir / "runtime"

  lazy val all = Project("base-runtime", dir) aggregate (dbserver)

  lazy val dbserver = OsgiProject("dbserver") dependsOn (db4o, xstream, misc.replication)
}