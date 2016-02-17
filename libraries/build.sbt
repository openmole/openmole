
publishTo in ThisBuild <<= isSnapshot(if (_) Some("OpenMOLE Nexus" at "https://maven.openmole.org/snapshots") else Some("OpenMOLE Nexus" at "https://maven.openmole.org/releases"))

//enablePlugins(SbtOsgi)

