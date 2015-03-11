resolvers += Classpaths.sbtPluginSnapshots

resolvers += "openmole-public" at "http://maven.openmole.org/public"

resolvers += "Typesafe repository" at
  "http://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "0.6.0")

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.5.1")

addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % "0.3.1")

addSbtPlugin("org.openmole" % "openmole-buildsystem-plugin" % "1.4-SNAPSHOT")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.1")

addSbtPlugin("com.lihaoyi" % "scalatex-sbt-plugin" % "0.1.1")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.3.2")
