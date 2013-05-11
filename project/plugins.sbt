resolvers += Classpaths.sbtPluginSnapshots

resolvers += Resolver.url("scalasbt", new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases"))(Resolver.ivyStylePatterns)

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "0.5.4")

addSbtPlugin("com.typesafe.sbt" %% "sbt-osgi" % "0.6.0-SNAPSHOT")

addSbtPlugin("com.github.mpeltonen" %% "sbt-idea" % "1.4.0")

addSbtPlugin("net.virtual-void" %% "sbt-dependency-graph" % "0.7.1")