import com.typesafe.sbt.packager.docker.{Cmd, ExecCmd}
import sbt.Resolver.bintrayRepo

organization in ThisBuild := "com.example"
version in ThisBuild := "1.0-SNAPSHOT"
lazy val buildVersion = sys.props.getOrElse("buildVersion", "1.0.0-SNAPSHOT")

// the Scala version that will be used for cross-compiled libraries
scalaVersion in ThisBuild := "2.12.4"

val macwire = "com.softwaremill.macwire" %% "macros" % "2.3.0" % "provided"

val scalaTest = "org.scalatest" %% "scalatest" % "3.0.4" % Test

val locatorDNS = "com.lightbend" %% "lagom14-scala-service-locator-dns" % "2.3.0"

lazy val `hello` = (project in file("."))
  .aggregate(`hello-api`, `hello-impl`, `hello-stream-api`, `hello-stream-impl`)

lazy val `hello-api` = (project in file("hello-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi
    )
  )

lazy val `hello-impl` = (project in file("hello-impl"))
  .enablePlugins(LagomScala)
  .settings(
    version := buildVersion,
    version in Docker := buildVersion,
    dockerBaseImage := "openjdk:8-jre-alpine",
    dockerRepository := Some("hello"),
    dockerUpdateLatest := true,
      dockerEntrypoint ++= """-Dhttp.address="$(eval "echo $HELLO_BIND_IP")" -Dhttp.port="$(eval "echo $HELLO_BIND_PORT")" -Dakka.remote.netty.tcp.hostname="$(eval "echo $AKKA_REMOTING_HOST")" -Dakka.remote.netty.tcp.bind-hostname="$(eval "echo $AKKA_REMOTING_BIND_HOST")" -Dakka.remote.netty.tcp.port="$(eval "echo $AKKA_REMOTING_PORT")" -Dakka.remote.netty.tcp.bind-port="$(eval "echo $AKKA_REMOTING_BIND_PORT")" $(IFS=','; I=0; for NODE in $AKKA_SEED_NODES; do echo "-Dakka.cluster.seed-nodes.$I=akka.tcp://hello@$NODE"; I=$(expr $I + 1); done)""".split(" ").toSeq,
    dockerCommands :=
      dockerCommands.value.flatMap {
        case ExecCmd("ENTRYPOINT", args @ _*) => Seq(Cmd("ENTRYPOINT", args.mkString(" ")))
        case c @ Cmd("FROM", _) => Seq(c, ExecCmd("RUN", "/bin/sh", "-c", "apk add --no-cache bash && ln -sf /bin/bash /bin/sh"))
        case v => Seq(v)
      },
    resolvers += bintrayRepo("hajile", "maven"),
    resolvers += bintrayRepo("hseeberger", "maven"),
    libraryDependencies ++= Seq(
      lagomScaladslPersistenceCassandra,
      lagomScaladslKafkaBroker,
      lagomScaladslTestKit,
      locatorDNS,
      macwire,
      scalaTest
    )
  )
  .settings(lagomForkedTestSettings: _*)
  .dependsOn(`hello-api`)

lazy val `hello-stream-api` = (project in file("hello-stream-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi
    )
  )

lazy val `hello-stream-impl` = (project in file("hello-stream-impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslTestKit,
      macwire,
      scalaTest
    )
  )
  .dependsOn(`hello-stream-api`, `hello-api`)
