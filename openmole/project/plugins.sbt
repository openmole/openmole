resolvers += Classpaths.sbtPluginSnapshots

//resolvers += Resolver.url("scalasbt", new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases"))(Resolver.ivyStylePatterns)

resolvers += "openmole-public" at "http://maven.openmole.org/public"

resolvers += "Typesafe repository" at
  "http://repo.typesafe.com/typesafe/releases/"

resolvers ++= Seq(Resolver.sonatypeRepo("snapshots"),
  Resolver.url("scala-js-releases",
    url("http://dl.bintray.com/content/scala-js/scala-js-releases"))(
      Resolver.ivyStylePatterns))

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "0.6.0")

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.5.1")

addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % "0.3.0")

addSbtPlugin("org.openmole" % "openmole-buildsystem-plugin" % "1.3-SNAPSHOT")

addSbtPlugin("org.scala-lang.modules.scalajs" % "scalajs-sbt-plugin" % "0.5.6")

addSbtPlugin("com.lihaoyi" % "scalatex-sbt-plugin" % "0.1.1")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.3.2")
