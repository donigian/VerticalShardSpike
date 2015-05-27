
name := "VerticalShardSpike"

version := "0.1"

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  "org.postgresql" %  "postgresql"     % "9.3-1100-jdbc41",
  "com.typesafe"   %  "config"         % "1.3.0",
  "org.slf4j"      %  "slf4j-api"      % "1.7.5",
  "org.slf4j"      %  "slf4j-simple"   % "1.7.5",
  "org.clapper"    %% "grizzled-slf4j" % "1.0.2",
  "org.scalatest"  %  "scalatest_2.11" % "2.2.4" 	%	"test"
)

parallelExecution in Test := false