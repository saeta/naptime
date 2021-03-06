/*
 * Copyright 2016 Coursera Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import sbt._
import sbt.Keys
import de.heikoseeberger.sbtheader.HeaderKey.headers
import de.heikoseeberger.sbtheader.license.Apache2_0


object NaptimeBuild extends Build with NamedDependencies with PluginVersionProvider {

  def playVersion = "2.4.4" // Play version is defined here, and in project/plugins.sbt
  def akkaVersion = "2.3.13" // Akka version must match the one Play is built with.
  def courierVersion = "2.0.2"

  lazy val root = project
    .in(file("."))
    .settings(org.coursera.naptime.sbt.Sonatype.settings)
    .aggregate(naptime, graphql, models, testing, pegasus)

  lazy val naptime = configure(project)
    .in(file("naptime"))
    .dependsOn(models)

  lazy val graphql = configure(project)
    .in(file("naptime-graphql"))
    .dependsOn(naptime % "test->test;compile->compile")

  lazy val models = configure(project)
    .in(file("naptime-models"))
    .dependsOn(pegasus)

  lazy val testing = configure(project)
    .in(file("naptime-testing"))
    .dependsOn(naptime)

  lazy val pegasus = configure(project)
    .in(file("naptime-pegasus"))

  lazy val examples = configure(project)
    .in(file("examples"))
    .dependsOn(naptime, testing, graphql)

  lazy val plugin = project
    .in(file("naptime-sbt-plugin"))
    .settings(org.coursera.naptime.sbt.Sonatype.settings)

  lazy val testSettings = Seq(
    Keys.testFrameworks := Seq(sbt.TestFrameworks.JUnit),
    Keys.testOptions += Tests.Argument(TestFrameworks.JUnit, "-v", "-q", "-a"))

  lazy val headerSettings = Seq(
    headers := Map(
      "scala" -> Apache2_0("2016", "Coursera Inc."),
      "conf" -> Apache2_0("2016", "Coursera Inc.", "#"),
      "courier" -> Apache2_0("2016", "Coursera Inc.")
    )
  )


  private[this] def configure(project: Project): Project = {
    project
      .enablePlugins(play.sbt.PlayScala)
      .disablePlugins(play.sbt.PlayLayoutPlugin)
      .settings(testSettings)
      .settings(headerSettings)
      .settings(org.coursera.naptime.sbt.Sonatype.settings)
  }

}
