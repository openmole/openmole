name := "toolxit-bibtex"

publishMavenStyle := true

publishArtifact in Test := false

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

pomIncludeRepository := { _ => false }

licenses := Seq("GPLv3" -> url("http://www.gnu.org/licenses/"))

homepage := Some(url("https://github.com/jopasserat/toolxit-bibtex"))

scmInfo := Some(ScmInfo(url("https://github.com/jopasserat/toolxit-bibtex.git"), "scm:git:git@github.com:jopasserat/toolxit-bibtex.git"))

pomExtra := (
  <developers>
    <developer>
      <id>jopasserat</id>
      <name>Jonathan Passerat-Palmbach</name>
    </developer>
  </developers>
)

scalariformSettings

releaseSettings

