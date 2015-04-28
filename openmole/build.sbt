
publishTo in ThisBuild <<= isSnapshot(if (_) Some("OpenMOLE Nexus" at "http://maven.openmole.org/snapshots") else Some("OpenMOLE Nexus" at "http://maven.openmole.org/releases"))


