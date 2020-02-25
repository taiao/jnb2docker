/*
 * Main.java
 * Copyright (C) 2020 University of Waikato, Hamilton, NZ
 */

package com.github.fracpete.jnb2docker;

import com.github.fracpete.resourceextractor4j.IOUtils;
import com.github.fracpete.simpleargparse4j.ArgumentParser;
import com.github.fracpete.simpleargparse4j.ArgumentParserException;
import com.github.fracpete.simpleargparse4j.Namespace;
import com.github.fracpete.simpleargparse4j.Option.Type;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Command-line application for turning Java Jupyter notebooks into Docker images.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 */
public class Main {

  public static final String MAVEN_MAGIC = "%maven ";

  /** the alternative maven installation. */
  protected File m_MavenHome;

  /** the maven user settings to use. */
  protected File m_MavenUserSettings;

  /** the alternative java installation. */
  protected File m_JavaHome;

  /** the JVM options. */
  protected List<String> m_JVM;

  /** the notebook to convert. */
  protected File m_Input;

  /** the output directory. */
  protected File m_OutputDir;

  /** for logging. */
  protected Logger m_Logger;

  /** whether help got requested. */
  protected boolean m_HelpRequested;

  /** the notebook. */
  protected transient JsonObject m_Notebook;

  /** the collected dependencies. */
  protected transient List<String> m_Dependencies;

  /**
   * Initializes the object.
   */
  public Main() {
    initialize();
  }

  /**
   * Initializes the members.
   */
  protected void initialize() {
    m_MavenHome         = null;
    m_MavenUserSettings = null;
    m_JavaHome          = null;
    m_Input             = null;
    m_OutputDir         = null;
    m_JVM               = null;
    m_HelpRequested     = false;
    m_Notebook          = null;
    m_Dependencies      = null;
  }

  /**
   * Returns the logger instance to use.
   *
   * @return		the logger
   */
  protected Logger getLogger() {
    if (m_Logger == null)
      m_Logger = Logger.getLogger(getClass().getName());
    return m_Logger;
  }

  /**
   * Sets the alternative maven installation to use.
   *
   * @param dir		the top-level directory (above "bin")
   * @return		itself
   */
  public Main mavenHome(File dir) {
    m_MavenHome = dir;
    return this;
  }

  /**
   * Returns the alternative maven installation to use.
   *
   * @return		the directory, null to use bundled one
   */
  public File getMavenHome() {
    return m_MavenHome;
  }

  /**
   * Sets the alternative maven user settings to use.
   *
   * @param dir		the XML file, null to use default ($HOME/.m2/settings.xml)
   * @return		itself
   */
  public Main mavenUserSettings(File dir) {
    m_MavenUserSettings = dir;
    return this;
  }

  /**
   * Returns the alternative maven user settings to use.
   *
   * @return		the file, null to use default ($HOME/.m2/settings.xml)
   */
  public File getMavenUserSettings() {
    return m_MavenUserSettings;
  }

  /**
   * Sets the alternative java installation to use.
   *
   * @param dir		the top-level directory (above "bin")
   * @return		itself
   */
  public Main javaHome(File dir) {
    m_JavaHome = dir;
    return this;
  }

  /**
   * Returns the alternative java installation to use.
   *
   * @return		the directory, null if using one that class was started with
   */
  public File getJavaHome() {
    return m_JavaHome;
  }

  /**
   * Sets the notebook to convert.
   *
   * @param input	the notebook
   * @return		itself
   */
  public Main input(File input) {
    m_Input = input;
    return this;
  }

  /**
   * Returns the notebook to convert.
   *
   * @return		the notebook, null if none set
   */
  public File getInput() {
    return m_Input;
  }

  /**
   * Sets the output directory for the bootstrapped application.
   *
   * @param dir		the directory
   * @return		itself
   */
  public Main outputDir(File dir) {
    m_OutputDir = dir;
    return this;
  }

  /**
   * Returns the output directory for the bootstrapped application.
   *
   * @return		the directory, null if none set
   */
  public File getOutputDir() {
    return m_OutputDir;
  }

  /**
   * Sets the JVM options to use for launching the main class.
   *
   * @param options	the options, can be null
   * @return		itself
   */
  public Main jvm(List<String> options) {
    m_JVM = options;
    return this;
  }

  /**
   * Sets the JVM options to use for launching the main class.
   *
   * @param options	the options, can be null
   * @return		itself
   */
  public Main jvm(String... options) {
    if (options != null)
      m_JVM = new ArrayList<>(Arrays.asList(options));
    else
      m_JVM = null;
    return this;
  }

  /**
   * Returns the JVM options.
   *
   * @return		the options, can be null
   */
  public List<String> getJvm() {
    return m_JVM;
  }

  /**
   * Configures and returns the commandline parser.
   *
   * @return		the parser
   */
  protected ArgumentParser getParser() {
    ArgumentParser 		parser;

    parser = new ArgumentParser("Converts Java Jupyter notebooks into Docker images.");
    parser.addOption("-m", "--maven_home")
      .required(false)
      .type(Type.EXISTING_DIR)
      .dest("maven_home")
      .help("The directory with a local Maven installation to use instead of the bundled one.");
    parser.addOption("-u", "--maven_user_settings")
      .required(false)
      .type(Type.EXISTING_FILE)
      .dest("maven_user_settings")
      .help("The file with the maven user settings to use other than $HOME/.m2/settings.xml.");
    parser.addOption("-j", "--java_home")
      .required(false)
      .type(Type.EXISTING_DIR)
      .dest("java_home")
      .help("The Java home to use for the Maven execution.");
    parser.addOption("-v", "--jvm")
      .required(false)
      .multiple(true)
      .dest("jvm")
      .help("The parameters to pass to the JVM before launching the application.");
    parser.addOption("-i", "--input")
      .required(true)
      .type(Type.EXISTING_FILE)
      .dest("input")
      .help("The Java Jupyter notebook to convert.");
    parser.addOption("-o", "--output_dir")
      .required(true)
      .type(Type.DIRECTORY)
      .dest("output_dir")
      .help("The directory to output the bootstrapped application in.");

    return parser;
  }

  /**
   * Sets the parsed options.
   *
   * @param ns		the parsed options
   * @return		if successfully set
   */
  protected boolean setOptions(Namespace ns) {
    mavenHome(ns.getFile("maven_home"));
    mavenUserSettings(ns.getFile("maven_user_settings"));
    javaHome(ns.getFile("java_home"));
    input(ns.getFile("input"));
    outputDir(ns.getFile("output_dir"));
    jvm(ns.getList("jvm"));
    return true;
  }

  /**
   * Returns whether help got requested when setting the options.
   *
   * @return		true if help got requested
   */
  public boolean getHelpRequested() {
    return m_HelpRequested;
  }

  /**
   * Parses the options and configures the object.
   *
   * @param options	the command-line options
   * @return		true if successfully set (or help requested)
   */
  public boolean setOptions(String[] options) {
    ArgumentParser parser;
    Namespace 		ns;

    m_HelpRequested = false;
    parser          = getParser();
    try {
      ns = parser.parseArgs(options);
    }
    catch (ArgumentParserException e) {
      parser.handleError(e);
      m_HelpRequested = parser.getHelpRequested();
      return m_HelpRequested;
    }

    return setOptions(ns);
  }

  /**
   * Initialize the notebook.
   *
   * @return		null if successful, otherwise error message
   */
  protected String initNotebook() {
    JsonParser 		jp;
    JsonElement 	je;
    FileReader		freader;
    BufferedReader	breader;

    if (m_Input == null)
      return "No Java Jupyter notebook provided!";
    if (!m_Input.exists())
      return "Java Jupyter notebook does not exist: " + m_Input;
    if (m_Input.isDirectory())
      return "Java Jupyter notebook points to a directory: " + m_Input;

    freader = null;
    breader = null;
    try {
      freader = new FileReader(m_Input.getAbsolutePath());
      breader = new BufferedReader(freader);
      jp = new JsonParser();
      je = jp.parse(breader);
      if (je instanceof JsonObject)
        m_Notebook = (JsonObject) je;
      else
        return "Expected Java Jupyter notebook to be a JSON object, instead got: " + je.getClass().getName();
    }
    catch (Exception e) {
      getLogger().log(Level.SEVERE, "Failed to parse Java Jupyter notebook: " + m_Input, e);
      return "Failed to parse Java Jupyter notebook: " + m_Input;
    }
    finally {
      IOUtils.closeQuietly(breader);
      IOUtils.closeQuietly(freader);
    }

    return null;
  }

  /**
   * Collects the dependencies ("%maven ...") from the code cells.
   *
   * @return		null if successfully collected, otherwise error message
   */
  protected String initDependencies() {
    JsonArray		cells;
    JsonObject		cell;
    JsonArray		code;
    String		codeLine;
    String		dep;

    m_Dependencies = new ArrayList<>();

    if (!m_Notebook.has("cells"))
      return "Java Jupyter Notebook does not contain root array 'cells'?";

    cells = m_Notebook.getAsJsonArray("cells");
    for (JsonElement cellEl: cells) {
      if (cellEl instanceof JsonObject) {
        cell = cellEl.getAsJsonObject();
        if (cell.has("cell_type") && cell.get("cell_type").getAsString().equals("code") && cell.has("source")) {
          code = cell.get("source").getAsJsonArray();
          for (JsonElement codeLineEl: code) {
            codeLine = codeLineEl.getAsString().trim();
            if (codeLine.startsWith(MAVEN_MAGIC)) {
              dep = codeLine.substring(MAVEN_MAGIC.length(), codeLine.length()).trim();
              m_Dependencies.add(dep);
	    }
	  }
	}
      }
    }

    if (m_Dependencies.size() > 0)
      getLogger().info("Dependencies: " + m_Dependencies);

    return null;
  }

  /**
   * Performs the bootstrapping.
   *
   * @return		null if successful, otherwise error message
   */
  protected String doExecute() {
    String 	result;

    // load notebook
    if ((result = initNotebook()) != null)
      return result;

    // retrieve dependencies
    if ((result = initDependencies()) != null)
      return result;

    // TODO
    // - generate lib directory with bootstrapp
    // - generate class
    // - compile class against libraries
    // - generate Dockerfile
    // - output instructions for compiling docker image

    return null;
  }

  /**
   * Performs the bootstrapping.
   *
   * @return		null if successful, otherwise error message
   */
  public String execute() {
    String		result;

    result = doExecute();
    if (result != null)
      getLogger().severe(result);

    return result;
  }

  /**
   * Executes the bootstrapping with the specified command-line arguments.
   *
   * @param args	the options to use
   */
  public static void main(String[] args) {
    Main main = new Main();

    if (!main.setOptions(args)) {
      System.err.println("Failed to parse options!");
      System.exit(1);
    }
    else if (main.getHelpRequested()) {
      System.exit(0);
    }

    String result = main.execute();
    if (result != null) {
      System.err.println("Failed to perform bootstrapping:\n" + result);
      System.exit(2);
    }
  }
}
