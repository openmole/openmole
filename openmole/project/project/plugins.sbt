addSbtPlugin("com.typesafe.sbt" % "sbt-scalariform" % "1.2.0")

resolvers ++= Seq(Resolver.sonatypeRepo("snapshots"),
Resolver.sonatypeRepo("releases"),
  Resolver.url("scala-js-releases",
    url("http://dl.bintray.com/content/scala-js/scala-js-releases"))(
      Resolver.ivyStylePatterns))

addSbtPlugin("fr.iscpif" %% "jsmanager" % "0.2.0")