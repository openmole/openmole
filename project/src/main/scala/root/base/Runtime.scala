package root.base

import sbt._
import Keys._
import root.libraries._
import root.thirdparties._

package object runtime extends BaseDefaults {
  implicit val artifactPrefix = Some("org.openmole.runtime")

  override def dir = super.dir / "runtime"

  lazy val all = Project("base-runtime", dir) aggregate (dbserver, runtime)

  lazy val dbserver = OsgiProject("dbserver") dependsOn (db4o, xstream, misc.replication)

  lazy val runtime = OsgiProject("runtime") dependsOn (core.implementation, core.batch, core.serializer,
    misc.logging, scalaLang, scopt, misc.hashService, misc.eventDispatcher, misc.exception) settings
    (libraryDependencies += "org.eclipse.core" % "org.eclipse.equinox.app" % "1.3.100.v20120522-1841" % "provided")
}