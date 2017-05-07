resolvers += Classpaths.sbtPluginSnapshots

//resolvers += Resolver.url("scalasbt", new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases"))(Resolver.ivyStylePatterns)

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("org.openmole" % "openmole-buildsystem-plugin" % "1.7-SNAPSHOT")

addSbtPlugin("com.eed3si9n" % "sbt-dirty-money" % "0.1.0")
