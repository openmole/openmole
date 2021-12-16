resolvers += Resolver.sonatypeRepo("staging")

addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % "0.4.2")

addSbtPlugin("org.openmole" % "openmole-buildsystem-plugin" % "1.8-SNAPSHOT")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.5.1")

addSbtPlugin("org.openmole" % "scalajs-execnpm" % "0.10")

addSbtPlugin("org.openmole" % "scalatex-sbt-plugin" % "0.4.5")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.7.0")

addSbtPlugin("se.marcuslonnberg" % "sbt-docker" % "1.8.0")

//addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.8.2")

addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "1.0.0")

addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.0.0")
