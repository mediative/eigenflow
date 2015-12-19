resolvers += Resolver.url("YPG-Data SBT Plugins", url("https://dl.bintray.com/ypg-data/sbt-plugins"))(Resolver.ivyStylePatterns)

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.1")
addSbtPlugin("com.mediative.sbt" % "sbt-mediative-core" % "0.1.1")
addSbtPlugin("com.mediative.sbt" % "sbt-mediative-oss" % "0.1.1")
