scalaVersion := "2.13.3"

libraryDependencies ++= ProjectConfig.projectDependencies

scalacOptions += "-Ymacro-annotations"

testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
