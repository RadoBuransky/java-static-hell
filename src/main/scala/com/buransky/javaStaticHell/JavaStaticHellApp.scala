package com.buransky.javaStaticHell

import java.io.{BufferedWriter, PrintWriter, File}
import java.util.jar.JarFile
import org.apache.bcel.classfile.ClassParser

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

/**
 * Application command line arguments.
 * fdp -Tsvg -o graph.svg ./graph.dot
 *
 * @param jarFilePath Path to jar file to be analysed.
 */
case class JavaStaticHellAppArgs(jarFilePath: File) {
  if (!jarFilePath.exists())
    throw new IllegalArgumentException(s"JAR file does not exist! [$jarFilePath]")
}

case class Dep(from: String, to: Iterable[String])

/**
 * Application entry point.
 */
object JavaStaticHellApp {
  def main(args: Array[String]) = {
    // Get application arguments
    val appArgs = getAppAgrs(args)

    // Buffer with all static dependencies
    val buffer = new ListBuffer[Dep]

    val packageFilter = "org.springframework"

    // Go through all entries
    val jarEntries = new JarFile(appArgs.jarFilePath).entries()
    while (jarEntries.hasMoreElements) {
      // Get next entry
      val jarEntry = jarEntries.nextElement()
      if (!jarEntry.isDirectory && jarEntry.getName.endsWith(".class")) {
        val classParser = new ClassParser(appArgs.jarFilePath.getAbsolutePath, jarEntry.getName)
        val javaClass = classParser.parse()
        if (javaClass.getClassName.startsWith(packageFilter)) {
          val classVisitor = new ClassVisitor(javaClass)
          val deps = classVisitor.staticDependencies().filter(d => d.startsWith(packageFilter) && d != javaClass.getClassName)
          if (deps.nonEmpty)
            buffer += Dep(javaClass.getClassName, deps)
        }
      }
    }

    // Recursively generate DOT files of individual levels
    generateAndRake(buffer, 1)
  }

  private def generateAndRake(deps: Iterable[Dep], level: Int): Unit = {
    // Generate DOT file
    generateDotFile(new File(s"./build/graph$level.dot"), deps)

    // Recurse
    val raked = rake(deps)
    if (raked.size < deps.size)
      generateAndRake(raked, level + 1)
  }

  private def rake(deps: Iterable[Dep]): Iterable[Dep] = {
    val allTo = deps.flatMap(_.to)
    deps.filter(d => allTo.contains(d.from))
  }

  private def generateDotFile(dotFile: File, deps: Iterable[Dep]): Unit = {
    val output = new BufferedWriter(new PrintWriter(dotFile))
    try {
      output.write("digraph javaStaticHell {\n")
      try {
        // First print all classes and label them
        val all = deps.toSeq.flatMap(dep => dep.to.seq ++ Seq(dep.from)).distinct
        all.foreach { dep =>
          val dotName = classNameForDot(dep)
          val dotLabel = labelForDot(dep)

          output.write(s"    $dotName [label=$dotLabel];\n")
        }

        // And now print all dependencies
        deps.foreach { dep =>
          val fromName = classNameForDot(dep.from)
          dep.to.foreach { depTo =>
            val toName = classNameForDot(depTo)
            output.write(s"    $fromName -> $toName;\n")
          }
        }
      }
      finally {
        output.write("}\n")
      }
    }
    finally {
      output.close()
    }

    Console.out.println(s"DOT file generated. [${dotFile.getAbsolutePath}]")
  }

  private def classNameForDot(className: String): String = className.replaceAll("[\\.$]", "")
  private def labelForDot(className: String): String = classNameForDot(className.substring(className.lastIndexOf('.') + 1))

  private def getAppAgrs(args: Array[String]) =
    JavaStaticHellAppArgs(new File(args(0)))
}
