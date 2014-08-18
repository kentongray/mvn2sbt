package pl.jozwik.mvn2sbt.pom2sbt

import java.io.File

import org.maven.{Plugin, Configuration4}
import pl.jozwik.mvn2sbt.PomToSbtPluginConverter

import scala.xml.Node


class GwtPluginConverter extends PomToSbtPluginConverter {

  import pl.jozwik.mvn2sbt.PluginConverter._

  protected [pom2sbt] def configurationToSet(confHead: Configuration4, rootDir: File)(implicit plugin:Plugin): Set[String] = {
    val versionOption = plugin.version.map(version =>s"""gwtVersion := "$version" """)
    val moduleOption = toOption(confHead,"module",s => s"""gwtModules := List("$s")""" )
    val extraArgsOption = toOption(confHead,"extraJvmArgs",s => s"""javaOptions in Gwt += "$s" """)
    Set(extraArgsOption,moduleOption,versionOption).flatten
  }


  private def toOption(confHead: Configuration4,name:String,f:(String)=>String) = {
    val node = findElement(confHead, name)
    val moduleName = node.map(v => v.value.asInstanceOf[Node].text)
    moduleName.map(s => f(s))
  }
}