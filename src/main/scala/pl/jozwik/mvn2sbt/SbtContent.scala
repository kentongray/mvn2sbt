package pl.jozwik.mvn2sbt

import java.io.{Writer, File}
import com.typesafe.scalalogging.slf4j.LazyLogging
import scala.collection.JavaConversions._

object SbtContent {
  def toPath(path: File, rootDir: File) = {
    val dir = path.getAbsolutePath
    val root = rootDir.getAbsolutePath
    val diff = dir.substring(root.length)
    if (diff.startsWith(File.separator)) {
      diff.substring(1)
    } else {
      diff
    }
  }
}

case class SbtContent(private val projects: Seq[Project], private val hierarchy: Map[MavenDependency, ProjectInformation], private val rootDir: File) extends LazyLogging {

  import SbtContent._

  def write(buildSbtWriter: Writer, pluginsSbtWriter: Writer) {
    buildSbtWriter.write(
      """
        |
        |def ProjectName(name: String,path:String): Project =  Project(name, file(path))
        |
      """.stripMargin)
    projects.foreach { p =>
      val projectOutput = createProject(p)
      val (buildSbt, pluginsSbt) = projectOutput
      buildSbtWriter.write(buildSbt)
      pluginsSbtWriter.write(pluginsSbt)
    }

  }

  private def createProject(p: Project) = {
    val projectName = p.mavenDependency.artifactId
    val information = hierarchy(p.mavenDependency)
    val path = toPath(information.projectPath, rootDir)

    val (dependsOn, libraries) = splitToDependsOnLibraries(p, information)

    val (settings, plugins, pluginDependencies) = toPlugins(information.plugins)

    val dependencies = toDependencies(pluginDependencies ++ libraries)

    val dependsOnString = toDependsOnString(dependsOn, information)



    (createBuildSbt(p, projectName, path, dependencies, dependsOnString, settings), plugins)
  }


  private def splitToDependsOnLibraries(p: Project, information: ProjectInformation) = p.dependencies.partition { d =>
    val m = d.mavenDependency
    val contains = hierarchy.contains(m)
    val parentMatch = hierarchy(p.mavenDependency).parent match {
      case Some(parent) =>
        parent == m
      case _ => false
    }
    contains || parentMatch
  }

  private def toPlugins(plugins: Seq[PluginEnum]): (Seq[String], String, Seq[Dependency]) = {
    plugins.foldLeft((Seq[String](), "", Seq[Dependency]())) { (tuple, p) =>
      val (accSett, accPlug, accPluginDependencies) = tuple
      (s"${p.getSbtSetting}" +: accSett,
        accPlug + s"""
         |${p.getPlugin}
         |
         |${p.getExtraRepository}
         |
       """.stripMargin, p.getDependencies ++ accPluginDependencies)
    }
  }


  def toDependencies(libraries: Seq[Dependency]) = libraries.map { d =>
    val md = d.mavenDependency
    val scope = d.scope match {
      case Scope.compile => ""
      case x => s""" % "$x" """
    }
    s"""  "${md.groupId}" % "${md.artifactId}" % "${md.versionId}" $scope"""

  }.mkString("", ",\n   ", "")


  private def toDependsOnString(dependsOn: Seq[Dependency], information: ProjectInformation) = dependsOn.map { d =>
    val test = d.scope match {
      case Scope.test => """% "test -> test""""
      case _ => ""
    }
    s"""`${hierarchy(d.mavenDependency).projectPath.getName}`$test"""
  }.mkString(",")

  private def createBuildSbt(p: Project, projectName: String, path: String, dependencies: String, dependsOnString: String, settings: Seq[String]) = {

    if (path.isEmpty) {
      s"""|
        |version := "${p.mavenDependency.versionId}"
        |
        |name := "${p.mavenDependency.artifactId}"
        |
        |organization := "${p.mavenDependency.groupId}"
        |
        |libraryDependencies in Global ++= Seq($dependencies
        |)
        |
        |${settings.mkString("", "\n\n", "")}
        """.stripMargin
    } else {
      val s = settings.mkString(".settings(", ").settings(", ")")
      s"""
      |
      |lazy val `$projectName` = ProjectName("$projectName","$path").settings(
      |  libraryDependencies ++= Seq($dependencies
      |    ),
      |    name := "${p.mavenDependency.artifactId}",
      |    version := "${p.mavenDependency.versionId}",
      |    organization := "${p.mavenDependency.groupId}"
      |)$s.dependsOn($dependsOnString)
      |
    """.stripMargin
    }
  }

}