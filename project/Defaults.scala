import com.typesafe.sbt.osgi.OsgiKeys
import com.typesafe.sbt.osgi.SbtOsgi
import sbt._
import Keys._

/**
 * Created with IntelliJ IDEA.
 * User: luft
 * Date: 3/17/13
 * Time: 6:43 PM
 * To change this template use File | Settings | File Templates.
 */
trait Defaults extends Build {
  val eclipseBuddyPolicy = SettingKey[Option[String]]("OSGi.eclipseBuddyPolicy", "The eclipse buddy policy thing.")

  override lazy val settings = super.settings ++
    Seq(version := "0.8-SNAPSHOT", organization := "org.openmole", scalaVersion := "2.10.1")

  def OSGIProject(artifactId: String, buddyPolicy: Option[String] = None, exports: Seq[String] = Seq()) = {
    val exportedPackages = if (exports.isEmpty) Seq(artifactId + ".*") else exports
    val additional = buddyPolicy.map(v => Map("Eclipse-BuddyPolicy" -> v)).getOrElse(Map())
    Project.defaultSettings ++
      SbtOsgi.osgiSettings ++
      Seq(name:= artifactId,
        OsgiKeys.exportPackage := exportedPackages,
        OsgiKeys.additionalHeaders := additional
      )
  }
}
