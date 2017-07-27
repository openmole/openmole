resolvers += Classpaths.sbtPluginSnapshots
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "0.6.0")

addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % "0.4.0")

addSbtPlugin("org.openmole" % "openmole-buildsystem-plugin" % "1.6-SNAPSHOT")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.18")

addSbtPlugin("ch.epfl.scala" % "sbt-web-scalajs-bundler" % "0.4.0")

addSbtPlugin("com.lihaoyi" % "scalatex-sbt-plugin" % "0.3.4")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.3.2")

addSbtPlugin("se.marcuslonnberg" % "sbt-docker" % "1.4.0")

