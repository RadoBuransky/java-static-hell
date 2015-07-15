package com.buransky.javaStaticHell

import java.io.{BufferedWriter, PrintWriter, File}
import java.util.jar.JarFile
import org.apache.bcel.classfile.{ClassFormatException, ClassParser}

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
case class Depth(className: String, direct: Int, indirect: Int)

/**
 * Application entry point.
 */
object JavaStaticHellApp {
  def main(args: Array[String]) = {
    // Get application arguments
    val appArgs = getAppAgrs(args)

    // Buffer with all static dependencies
    val buffer = new ListBuffer[Dep]

    // Go through all entries
    val jarEntries = new JarFile(appArgs.jarFilePath).entries()
    while (jarEntries.hasMoreElements) {
      // Get next entry
      val jarEntry = jarEntries.nextElement()
      if (!jarEntry.isDirectory && jarEntry.getName.endsWith(".class")) {
        val classParser = new ClassParser(appArgs.jarFilePath.getAbsolutePath, jarEntry.getName)
        try {
          val javaClass = classParser.parse()
          if (filterClass(javaClass.getClassName)) {
            val classVisitor = new ClassVisitor(javaClass)
            val deps = classVisitor.staticDependencies().filter(d => filterClass(d) && d != javaClass.getClassName)
            if (deps.nonEmpty)
              buffer += Dep(javaClass.getClassName, deps)
          }
        }
        catch {
          case ex: ClassFormatException =>
            Console.err.println(s"Parser error! [${jarEntry.getName} $ex]")
        }
      }
    }

    // Recursively generate DOT files of individual levels
    generateAndRake(buffer, 1)

    // Print dependency depth to CSV
    printDepths(distinctClassNames(buffer).map(classNameToDepth(_, buffer)))
  }

  private def filterClass(className: String): Boolean =
    className.startsWith("com.avitech") || className.startsWith("com.pixelpark")

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

  private def distinctClassNames(deps: Iterable[Dep]): Seq[String] =
    deps.toSeq.flatMap(dep => dep.to.seq ++ Seq(dep.from)).distinct

  private def classNameToDepth(className: String, deps: Iterable[Dep]): Depth =
    Depth(className, directDeps(className, deps), indirectDeps(className, deps))

  private def generateDotFile(dotFile: File, deps: Iterable[Dep]): Unit = {
    val output = new BufferedWriter(new PrintWriter(dotFile))
    try {
      output.write("digraph javaStaticHell {\n")
      try {
        // First print all classes and label them
        val all = distinctClassNames(deps)

        // Get all depths
        val depths = all.map(classNameToDepth(_, deps))

        all.foreach { dep =>
          val dotName = classNameForDot(dep)
          val dotLabel = labelForDot(depths.find(_.className == dep).head)

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

  private def printDepths(depths: Iterable[Depth]): Unit = {
    Console.out.println("")
    Console.out.println("CSV dependency depths (class name, indirect count, direct count):")
    depths.toSeq.sortBy(d => (-1 * d.indirect, -1 * d.direct)).foreach { depth =>
      Console.out.println(s"${depth.className};${depth.indirect};${depth.direct};")
    }
  }

  private def directDeps(className: String, all: Iterable[Dep]): Int = all.count(_.to.exists(_ == className))
  private def indirectDeps(rootClassName: String, all: Iterable[Dep]): Int = {
    def rec(className: String, acc: Set[String]): Int = {
      if (!acc.contains(className)) {
        val directDeps = all.filter(_.to.exists(_ == className))
        val accWithClassName = acc + className
        directDeps.size + directDeps.map(directDep => rec(directDep.from, accWithClassName)).sum
      }
      else
        0
    }

    rec(rootClassName, Set.empty)
  }

  private def classNameForDot(className: String): String = className.replaceAll("[\\.$]", "")
  private def labelForDot(depth: Depth): String = {
    val label = classNameForDot(depth.className.substring(depth.className.lastIndexOf('.') + 1))
    label + s"_${depth.direct}_${depth.indirect}"
  }

  private def getAppAgrs(args: Array[String]) =
    JavaStaticHellAppArgs(new File(args(0)))
}
