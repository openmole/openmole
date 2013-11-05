resolvers += Classpaths.sbtPluginSnapshots

resolvers += Resolver.url("scalasbt", new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases"))(Resolver.ivyStylePatterns)

resolvers += "openmole-public" at "http://maven.openmole.org/public"

//addSbtPlugin("org.clapper" % "sbt-izpack" % "0.3.4.2")

resolvers += Resolver.url(
  "sbt-plugin-releases",
  new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/")
)(Resolver.ivyStylePatterns)

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "0.6.0")

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.5.1")

addSbtPlugin("org.openmole" % "openmole-buildsystem-plugin" % "0.10.4")

addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % "0.3.0")
