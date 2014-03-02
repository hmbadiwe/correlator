resolvers += Resolver.url(
  "plugin-releases",
  url("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/")
)(Resolver.ivyStylePatterns)

resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"


addSbtPlugin("com.typesafe.sbt" %% "sbt-git" % "0.6.2")

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.6.0")