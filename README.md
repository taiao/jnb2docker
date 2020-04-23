# jnb2docker
Converts Java Jupyter notebooks (using the [IJava](https://github.com/SpencerPark/IJava) 
kernel) into Docker images.

## Coding conventions
Under the hood, [JShell](https://docs.oracle.com/javase/9/tools/jshell.htm) is
being used to execute the code from the notebook. However, JShell requires
a certain coding style for it to work, not just any Java code that can be 
compiled with `javac`. Statements that normally don't require surrounding in
curly brackets need to be coded with such, otherwise `jshell` won't know
that there is more code to come. 

This code works:

```java
if (condition) {
  dosomething;
} else {
  dosomethingelse;
}
``` 

This does not:

```java
if (condition)
  dosomething;
else
  dosomethingelse;
``` 

This one does not work either:

```java
if (condition) {
  dosomething;
} 
else {
  dosomethingelse;
}
``` 

In order to extract dependencies, you can use the following *line magics* in 
your Notebook:

* `%maven ...` -- for specifying a single maven dependency, e.g.:

   ```
   %maven nz.ac.waikato.cms.weka:weka-dev:3.9.4
   ```
   
* `%jars ...` -- for specifying external jars, e.g. a single one:

   ```
   %jars /some/where/multisearch-weka-package-2020.2.17.jar
   ```
   
   Or all jars in a directory:

   ```
   %jars C:/some/where/*.jar
   ```


## Command-line

```commandline
Converts Java Jupyter notebooks into Docker images.


Usage: [--help] [-m MAVEN_HOME] [-u MAVEN_USER_SETTINGS]
       [-j JAVA_HOME] [-v JVM...] -i INPUT
       -b DOCKER_BASE_IMAGE [-I DOCKER_INSTRUCTIONS]
       -o OUTPUT_DIR

Options:
-m, --maven_home MAVEN_HOME
	The directory with a local Maven installation to use instead of the
	bundled one.

-u, --maven_user_settings MAVEN_USER_SETTINGS
	The file with the maven user settings to use other than
	$HOME/.m2/settings.xml.

-j, --java_home JAVA_HOME
	The Java home to use for the Maven execution.

-v, --jvm JVM
	The parameters to pass to the JVM before launching the application.

-i, --input INPUT
	The Java Jupyter notebook to convert.

-b, --docker_base_image DOCKER_BASE_IMAGE
	The docker base image to use, e.g. 'openjdk:11-jdk-slim-buster'.

-I, --docker_instructions DOCKER_INSTRUCTIONS
	File with additional docker instructions to use for generating the
	Dockerfile.

-o, --output_dir OUTPUT_DIR
	The directory to output the bootstrapped application, JShell script and
	Dockerfile in.
```

## Example

For this example we use the [weka_filter_pipeline.ipynb](src/jupyter/weka_filter_pipeline.ipynb)
notebook and the additional [weka_filter_pipeline.dockerfile](src/jupyter/weka_filter_pipeline.dockerfile)
Docker instructions. This notebook contains a simple Weka filter setup, using
the [InterquartileRange](https://weka.sourceforge.io/doc.dev/weka/filters/unsupervised/attribute/InterquartileRange.html)
filter to remove outliers and extreme values from an input file and saving the cleaned 
dataset as a new file.

The command-lines for this example assume this directory structure:

```
/some/where
|
+- data
|  |
|  +- jnb2docker   // contains the jar
|  |
|  +- notebooks
|  |  |
|  |  +- weka_filter_pipeline.ipynb       // actual notebook
|  |  |
|  |  +- weka_filter_pipeline.dockerfile  // additional Dockerfile instructions
|  |
|  +- in
|  |  |
|  |  +- bolts.arff   // raw dataset to filter
|  |
|  +- out
|
+- output
|  |
|  +- wekaiqrcleaner  // will contain all the generated data, including "Dockerfile"
```

For our `Dockerfile`, we use the `openjdk:11-jdk-slim-buster` base image (`-b`), which
contains an OpenJDK 11 installation on top of a [Debian "buster"](https://www.debian.org/releases/buster/)
image. The `weka_filter_pipeline.ipynb` notebook (`-i`) then gets turned into code
for [JShell](https://docs.oracle.com/javase/9/tools/jshell.htm) using the 
following command-line:

```commandline
java -jar /some/where/data/jnb2docker/jnb2docker-0.0.3-spring-boot.jar \
  -i /some/where/data/notebooks/weka_filter_pipeline.ipynb \ 
  -o /some/where/output/wekaiqrcleaner \
  -b openjdk:11-jdk-slim-buster \
  -I /some/where/data/notebooks/weka_filter_pipeline.dockerfile  
```

Now we build the docker image called `wekaiqrcleaner` from the `Dockerfile`
that has been generated in the output directory `/some/where/output/wekaiqrcleaner` 
(`-o` option in previous command-line):

```
cd /some/where/output/wekaiqrcleaner
sudo docker build -t wekaiqrcleaner .
```

With the image built, we can now push the raw ARFF file through for cleaning.
For this to work, we map the in/out directories from our directory structure
into the Docker container (using the `-v` option) and we supply the input
and output files via the `INPUT` and `OUTPUT` environment variables (using 
the `-e` option). In order to see a few more messages, we also turn on the
debugging output that is part of the notebook, using the `VERBOSE` environment
variable:

```
sudo docker run -ti \
  -v /some/where/data/in:/data/in \
  -v /some/where/data/out:/data/out \
  -e INPUT=/data/in/bolts.arff \
  -e OUTPUT=/data/out/bolts-clean.arff \
  -e VERBOSE=true \
  wekaiqrcleaner
```

From the debugging messages you can see that the initial dataset with 40 rows
of data gets reduced to 36 rows.

**Disclaimer:** This is just a simple notebook tailored to the UCI dataset
*bolts.arff*.


## Releases

* [0.0.4](https://github.com/fracpete/jnb2docker/releases/download/jnb2docker-0.0.4/jnb2docker-0.0.4-spring-boot.jar)
* [0.0.3](https://github.com/fracpete/jnb2docker/releases/download/jnb2docker-0.0.3/jnb2docker-0.0.3-spring-boot.jar)
* [0.0.2](https://github.com/fracpete/jnb2docker/releases/download/jnb2docker-0.0.2/jnb2docker-0.0.2-spring-boot.jar)
* [0.0.1](https://github.com/fracpete/jnb2docker/releases/download/jnb2docker-0.0.1/jnb2docker-0.0.1-spring-boot.jar)


## Maven

```xml
    <dependency>
      <groupId>com.github.fracpete</groupId>
      <artifactId>jnb2docker</artifactId>
      <version>0.0.4</version>
    </dependency>
```
