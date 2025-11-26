
package openmole

import sbt.Keys._
import sbt._

object common {

  def sjs(name: String) = name + "_sjs1"

  def scala3VersionValue = "3.7.4"
  def scalajsVersion = "1.20.1"

  def scalaXMLVersion = "2.4.0"
  def gridscaleVersion = CommitVersion("001e0bfc30", "2.63")
  def mgoVersion = CommitVersion("6521cf3681b7aef7bc63583463bdce8b92d9b3d4", "3.68")
  def sshjVersion = "0.40.0"
  def containerVersion = "1.33"
  def bouncyCastleVersion = "1.82"
  def scalaTagsVersion = "0.13.1"
  def laminarVersion = "0.14.2"
  def netLogo5Version = "5.3.1"
  def netLogo6Version = "6.4.0-d2e6005"
  def netLogo7Version = "7.0.2"
  def scalaAutowireVersion = "0.3.3"
  def sourcecodeVersion = "0.4.2"
  def scaladgetVersion = "1.11.0"
  def plotlyVersion = "1.8.1"
  def sortableVersion = "0.7.2"
  def json4sVersion = "4.0.6"
  def jacksonVersion = "2.17.2"
  def circeVersion = "0.14.14"
  def circeYamlVersion = "0.16.1"
  def catsVersion = "2.13.0"
  def catsEffectVersion = "3.6.2"
  def catsParseVersion = "1.1.0"
  def squantsVersion = "1.8.3"
  def xstreamVersion = "1.4.21"
  def scalaURIVersion = "1.1.1"
  def scoptVersion = "4.1.0"
  def spatialsamplingVersion = "0.3"
  def logbackVersion = "1.2.3"
  def h2Version = "2.1.214"
  def shapelessVersion = "3.4.1"
  def jasyptVersion = "1.9.3"
  def monocleVersion = "3.2.0"
  def configuration2Version = "2.8.0"
  def compressVersion = "1.27.1"
  def scalazVersion = "7.3.7"
  def mathVersion = "3.6.1"
  def execVersion = "1.4.0"
  def asmVersion = "9.4"
  def guavaVersion = "31.1-jre"
  def scalaMetaVersion = "4.13.2"
  def scalaSTMVersion = "0.11.1"
  def jlineVersion = "3.25.1"
  def txtmarkVersion = "0.13"
  def slf4jVersion = "2.0.9"
  def foryVersion = "0.11.0"
  def jgitVersion = "7.0.0.202409031743-r"
  def gearsVersion = "0.2.0"
  def ulidCreatorVersion = "5.2.3"

  def http4sVersion = "0.23.30"
  def http4sBlaseSeverVersion = "0.23.17"
  def tapirVersion = "1.11.42"

  def xzVersion = "1.10"

  def lang3Version = "3.12.0"

  def codecVersion = "1.18.0"

  def scalajsLoggingVersion = "1.1.1"
  def scalaJSDomVersion = "2.8.0"
 
  case class CommitVersion(commit: String, version: String)
}

