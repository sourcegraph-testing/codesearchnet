/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * MinDeps.java
 * Copyright (C) 2017 FracPete
 */

package com.github.fracpete.deps4j;

import com.github.fracpete.processoutput4j.output.CollectingProcessOutput;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Class for determining the minimum required classes from a small set
 * of classes and an associated classpath.
 */
public class MinDeps {

  /** the java home directory to use. */
  protected File m_JavaHome;

  /** the jdeps binary. */
  protected File m_Jdeps;

  /** the classpath to use. */
  protected String m_ClassPath;

  /** the file with classes to determine the minimum dependencies for. */
  protected File m_ClassesFile;

  /** the file with additional class names to include (optional). */
  protected File m_AdditionalFile;

  /** packages to keep. */
  protected List<String> m_Packages;

  /** the output file for the determined class names (optional). */
  protected File m_OutputFile;
  
  /** the classes to inspect. */
  protected List<String> m_Classes;

  /** the optional resource files. */
  protected List<String> m_Resources;

  /** the dependent classes. */
  protected Set<String> m_DependentClasses;

  /** the full dependencies. */
  protected List<String> m_Dependencies;

  public MinDeps() {
    super();

    m_JavaHome         = (System.getenv("JAVA_HOME") != null ? new File(System.getenv("JAVA_HOME")) : null);
    m_ClassesFile      = null;
    m_ClassPath        = null;
    m_AdditionalFile   = null;
    m_OutputFile       = null;
    m_Packages         = new ArrayList<>();
    m_Classes          = new ArrayList<>();
    m_Resources        = new ArrayList<>();
    m_DependentClasses = new HashSet<>();
    m_Dependencies     = new ArrayList<>();

  }

  /**
   * Sets the java home directory.
   *
   * @param value	the directory
   */
  public void setJavaHome(File value) {
    m_JavaHome = value;
  }

  /**
   * Returns the java home directory.
   *
   * @return		the directory
   */
  public File getJavaHome() {
    return m_JavaHome;
  }

  /**
   * Sets the classpath to use.
   *
   * @param value	the classpath
   */
  public void setClassPath(String value) {
    m_ClassPath = value;
  }

  /**
   * Returns the classpath to use.
   *
   * @return		the classpath, null if not set
   */
  public String getClassPath() {
    return m_ClassPath;
  }

  /**
   * Sets the file with the class files to inspect.
   *
   * @param value	the file
   */
  public void setClassesFile(File value) {
    m_ClassesFile = value;
  }

  /**
   * Returns the file with the class files to inspect.
   *
   * @return		the file, null if not set
   */
  public File getClassesFile() {
    return m_ClassesFile;
  }

  /**
   * Sets the file with the additional classnames to include (optional).
   *
   * @param value	the file
   */
  public void setAdditionalFile(File value) {
    m_AdditionalFile = value;
  }

  /**
   * Returns the file with the additional classnames to include (optional).
   *
   * @return		the file, null if not set
   */
  public File getAdditionalFile() {
    return m_AdditionalFile;
  }

  /**
   * Sets the packages to keep.
   *
   * @param value	the packages
   */
  public void setPackages(List<String> value) {
    m_Packages.addAll(value);
  }

  /**
   * Returns the packages to keep.
   *
   * @return		the packages
   */
  public List<String> getPackages() {
    return m_Packages;
  }

  /**
   * Sets the file for storing the determined class names in (optional).
   * Uses stdout if not set.
   *
   * @param value	the file
   */
  public void setOutputFile(File value) {
    m_OutputFile = value;
  }

  /**
   * Returns the file for storing the determined class names in (optional).
   * Uses stdout if not set.
   *
   * @return		the file, null if not set
   */
  public File getOutputFile() {
    return m_OutputFile;
  }

  /**
   * Sets the commandline options.
   *
   * @param options	the options to use
   * @return		true if successful
   * @throws Exception	in case of an invalid option
   */
  public boolean setOptions(String[] options) throws Exception {
    ArgumentParser parser;
    Namespace ns;

    parser = ArgumentParsers.newArgumentParser(MinDeps.class.getName());
    parser.addArgument("--java-home")
      .type(Arguments.fileType().verifyExists().verifyIsDirectory())
      .dest("javahome")
      .required(true)
      .help("The java home directory of the JDK that includes the jdeps binary, default is taken from JAVA_HOME environment variable.");
    parser.addArgument("--class-path")
      .dest("classpath")
      .required(true)
      .help("The CLASSPATH to use for jdeps.");
    parser.addArgument("--classes")
      .type(Arguments.fileType().verifyExists().verifyIsFile().verifyCanRead())
      .dest("classes")
      .required(true)
      .help("The file containing the classes to determine the dependencies for. Empty lines and lines starting with # get ignored.");
    parser.addArgument("--additional")
      .type(Arguments.fileType())
      .setDefault(new File("."))
      .required(false)
      .dest("additional")
      .help("The file with additional class names to just include.");
    parser.addArgument("--output")
      .type(Arguments.fileType())
      .setDefault(new File("."))
      .required(false)
      .dest("output")
      .help("The file for storing the determined class names in.");
    parser.addArgument("package")
      .dest("packages")
      .required(true)
      .nargs("+")
      .help("The packages to keep, eg 'weka'.");

    try {
      ns = parser.parseArgs(options);
    }
    catch (ArgumentParserException e) {
      parser.handleError(e);
      return false;
    }

    setJavaHome(ns.get("javahome"));
    setClassPath(ns.getString("classpath"));
    setClassesFile(ns.get("classes"));
    setAdditionalFile(ns.get("additional"));
    setPackages(ns.getList("packages"));
    setOutputFile(ns.get("output"));

    return true;
  }

  /**
   * Initializes the execution.
   */
  protected void initialize() {
    m_DependentClasses.clear();
    m_Resources.clear();
    m_Classes.clear();
    m_Dependencies.clear();
  }

  /**
   * Reads the file into the the provided list.
   * Skips empty lines and lines starting with #.
   *
   * @param file	the file to read
   * @param lines 	the lines to add the content to
   * @return 		null if successful, otherwise error message
   */
  protected String readFile(File file, List<String> lines) {
    int		i;

    try {
      lines.addAll(Files.readAllLines(file.toPath()));
      i = 0;
      while (i < lines.size()) {
        if (lines.get(i).trim().isEmpty()) {
          lines.remove(i);
          continue;
	}
	if (lines.get(i).startsWith("#")) {
          lines.remove(i);
          continue;
	}
	i++;
      }
    }
    catch (Exception e) {
      return "Failed to read file: " + file + "\n" + e;
    }

    return null;
  }

  /**
   * Performs some checks.
   *
   * @return		null if successful, otherwise error message
   */
  protected String check() {
    String		error;

    if (!m_JavaHome.exists())
      return "Java home directory does not exist: " + m_JavaHome;
    if (!m_JavaHome.isDirectory())
      return "Java home does not point to a directory: " + m_JavaHome;
    if (System.getProperty("os.name").toLowerCase().contains("windows"))
      m_Jdeps = new File(m_JavaHome.getAbsolutePath() + File.separator + "bin" + File.separator + "jdeps.exe");
    else
      m_Jdeps = new File(m_JavaHome.getAbsolutePath() + File.separator + "bin" + File.separator + "jdeps");
    if (!m_Jdeps.exists())
      return "jdeps binary does not exist: " + m_Jdeps;

    if (!m_ClassesFile.exists())
      return "File with class names does not exist: " + m_ClassesFile;
    if (m_ClassesFile.isDirectory())
      return "File with class names points to directory: " + m_ClassesFile;

    // read classes
    error = readFile(m_ClassesFile, m_Classes);
    if (error != null)
      return error;

    // read resources
    if ((m_AdditionalFile != null) && m_AdditionalFile.exists() && (!m_AdditionalFile.isDirectory())) {
      error = readFile(m_AdditionalFile, m_Resources);
      if (error != null)
	return error;
    }

    return null;
  }

  /**
   * Filters the list of strings with a regular expression.
   *
   * @param lines	the list to filter
   * @param regexp	the regular expression to use
   * @param invert	whether to invert the matching sense
   * @return		the filtered list
   */
  protected List<String> filter(List<String> lines, String regexp, boolean invert) {
    List<String>	result;
    Pattern 		pattern;

    result  = new ArrayList<>();
    pattern = Pattern.compile(regexp);

    for (String line: lines) {
      if (invert) {
	if (!pattern.matcher(line).matches())
	  result.add(line);
      }
      else {
	if (pattern.matcher(line).matches())
	  result.add(line);
      }
    }

    return result;
  }

  /**
   * Builds the regular expression for the packages to keep.
   *
   * @return		the regexp
   */
  protected String packagesRegExp() {
    StringBuilder	result;
    int			i;
    String		pkg;

    result = new StringBuilder();
    result.append(".* (");
    for (i = 0; i < m_Packages.size(); i++) {
      if (i > 0)
        result.append("|");
      pkg = m_Packages.get(i);
      if (!pkg.endsWith("."))
        pkg = pkg + ".";
      pkg = pkg.replace(".", "\\.");
      result.append(pkg);
    }
    result.append(").*$");

    return result.toString();
  }

  /**
   * Determines the dependencies.
   *
   * @return		null if successful, otherwise error message
   */
  protected String determine() {
    String[] 			cmd;
    ProcessBuilder 		builder;
    CollectingProcessOutput 	output;
    List<String>		lines;
    int				i;
    String			line;

    for (String cls: m_Classes) {
      // progress
      System.err.println(cls);

      cmd = new String[]{
        m_Jdeps.getAbsolutePath(),
	"-cp",
	m_ClassPath,
	"-recursive",
	"-verbose:class",
	cls
      };
      builder = new ProcessBuilder();
      builder.command(cmd);
      output = new CollectingProcessOutput();
      try {
	output.monitor(builder);
      }
      catch (Exception e) {
        return "Failed to execute: " + builder.toString() + "\n" + e;
      }

      // filter output
      lines = new ArrayList<>(Arrays.asList(output.getStdOut().replace("\r", "").split("\n")));
      lines = filter(lines, packagesRegExp(), false);
      lines = filter(lines, ".*\\$.*", true);
      lines = filter(lines, ".*\\.jar\\)", true);

      // clean up
      for (i = 0; i < lines.size(); i++) {
        line = lines.get(i);
        line = line.replaceFirst(".* -> ", "");
        line = line.replaceFirst(" .*$", "");
        line = line.trim();
        lines.set(i, line);
      }

      // add to result
      m_DependentClasses.addAll(lines);
      System.err.println("--> " + m_DependentClasses.size());
    }

    return null;
  }

  /**
   * Determines the dependencies.
   *
   * @return		null if successful, otherwise error message
   */
  public String execute() {
    String		result;

    initialize();

    result = check();

    if (result == null)
      result = determine();

    if (result == null) {
      m_Dependencies = new ArrayList<>();
      m_Dependencies.addAll(m_Classes);
      for (String cls: m_DependentClasses) {
        if (!m_Dependencies.contains(cls))
	  m_Dependencies.add(cls);
      }
      for (String cls: m_Resources) {
        if (!m_Dependencies.contains(cls))
	  m_Dependencies.add(cls);
      }
      Collections.sort(m_Dependencies);
    }

    return result;
  }

  /**
   * Outputs the dependencies on stdout.
   */
  public void output() {
    if ((m_OutputFile == null || m_OutputFile.isDirectory())) {
      for (String dep : m_Dependencies)
	System.out.println(dep);
    }
    else {
      try {
	Files.write(m_OutputFile.toPath(), m_Dependencies, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
      }
      catch (Exception e) {
        System.err.println("Failed to write dependencies to: " + m_OutputFile);
        e.printStackTrace();
      }
    }
  }

  /**
   * Returns the dependencies collected.
   *
   * @return		the dependencies
   * @see		#execute()
   */
  public List<String> getDependencies() {
    return m_Dependencies;
  }

  public static void main(String[] args) throws Exception {
    MinDeps	mindeps;
    String	error;

    mindeps = new MinDeps();
    if (mindeps.setOptions(args)) {
      error = mindeps.execute();
      if (error != null) {
        System.err.println(error);
	System.exit(2);
      }
      else {
        mindeps.output();
      }
    }
    else {
      System.exit(1);
    }
  }
}
