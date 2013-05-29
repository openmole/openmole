resolvers += Classpaths.sbtPluginSnapshots

resolvers += Resolver.url("scalasbt", new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases"))(Resolver.ivyStylePatterns)

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "0.5.4")

addSbtPlugin("com.github.mpeltonen" %% "sbt-idea" % "1.4.0")

addSbtPlugin("net.virtual-void" %% "sbt-dependency-graph" % "0.7.1")

addSbtPlugin("org.openmole" % "openmole-buildsystem-plugin" % "0.9-SNAPSHOT")
