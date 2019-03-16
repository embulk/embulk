# What's Embulk?

Embulk is a parallel bulk data loader that **helps data transfer between various storages, databases, NoSQL and cloud services**.

**Embulk supports plugins** to add functions. You can [share the plugins](http://www.embulk.org/plugins/) to keep your custom scripts readable, maintainable, and reusable.

[![Embulk](https://gist.githubusercontent.com/frsyuki/f322a77ee2766a508ba9/raw/e8539b6b4fda1b3357e8c79d3966aa8148dbdbd3/embulk-overview.png)](http://www.slideshare.net/frsyuki/embuk-making-data-integration-works-relaxed/12)
[Embulk, an open-source plugin-based parallel bulk data loader](http://www.slideshare.net/frsyuki/embuk-making-data-integration-works-relaxed) at Slideshare

# Document

Embulk documents: http://www.embulk.org/docs/

# Mailing list

* [Embulk-announce](https://groups.google.com/forum/#!forum/embulk-announce): Embulk core members post important updates such as **key releases**, **compatibility information**, and **feedback requests to users**.

## Quick Start

### Linux & Mac & BSD

Embulk is a Java application. Please make sure that Java SE Runtime Environment (JRE) is installed. Embulk v0.8 series runs on Java 7, and Embulk v0.9 series runs on Java 8. Java 9 is not supported in any version for the time being.

Following 4 commands install embulk to your home directory:

```sh
curl --create-dirs -o ~/.embulk/bin/embulk -L "https://dl.embulk.org/embulk-latest.jar"
chmod +x ~/.embulk/bin/embulk
echo 'export PATH="$HOME/.embulk/bin:$PATH"' >> ~/.bashrc
source ~/.bashrc
```

Next step: [Running example in 4 commands](#running-example)

### Windows

Embulk is a Java application. Please make sure that Java SE Runtime Environment (JRE) is installed. Embulk v0.8 series runs on Java 7, and Embulk v0.9 series runs on Java 8. Java 9 is not supported in any version for the time being.

You can download `embulk.bat` using this command on cmd.exe or PowerShell.exe.

[Bintray no longer supports TLS 1.1 since June, 2018](https://jfrog.com/knowledge-base/why-am-i-failing-to-work-with-jfrog-saas-service-with-tls-1-0-1-1/) although [PowerShell's `Invoke-WebRequest` uses only SSL 3.0 and TLS 1.1 by default](https://social.technet.microsoft.com/Forums/en-US/00b78ac4-cadb-4566-b175-beb9d34a9093/how-to-use-tls-11-or-12-for-invokewebrequest). You'll need a tweak for PowerShell to use TLS 1.2, such as:

```
PowerShell -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::TLS12; Invoke-WebRequest http://dl.embulk.org/embulk-latest.jar -OutFile embulk.bat}"
```

Next step: [Running example in 4 commands](#running-example)

### Running example

`embulk example` command generates a sample CSV file so that you can try embulk quickly:

```
embulk example ./try1
embulk guess   ./try1/seed.yml -o config.yml
embulk preview config.yml
embulk run     config.yml
```

Next step: [Using plugins](#using-plugins)

### Using plugins

You can use plugins to load data from/to various systems and file formats. Here is the list of publicly released plugins: [list of plugins by category](https://plugins.embulk.org/).

An example is [embulk-output-command](https://github.com/embulk/embulk-output-command) plugin. It executes an external command to output the records.

To install plugins, you can use `embulk gem install <name>` command:

```
embulk gem install embulk-output-command
embulk gem list
```

Embulk bundles some built-in plugins such as `embulk-encoder-gzip` or `embulk-formatter-csv`. You can use those plugins with following configuration file:

```yaml
in:
  type: file
  path_prefix: "./try1/csv/sample_"
  ...
out:
  type: command
  command: "cat - > task.$INDEX.$SEQID.csv.gz"
  encoders:
    - {type: gzip}
  formatter:
    type: csv
```

### Resuming a failed transaction

Embulk supports resuming failed transactions.
To enable resuming, you need to start transaction with `-r PATH` option:

```
embulk run config.yml -r resume-state.yml
```

If the transaction fails, embulk stores state some states to the yaml file. You can retry the transaction using exactly same command:

```
embulk run config.yml -r resume-state.yml
```

If you give up on resuming the transaction, you can use `embulk cleanup` subcommand to delete intermediate data:

```
embulk cleanup config.yml -r resume-state.yml
```

### Using plugin bundle

`embulk mkbundle` subcommand creates a isolated bundle of plugins. You can install plugins (gems) to the bundle directory instead of ~/.embulk directory. This makes it easy to manage versions of plugins.
To use the bundle, add `-b <bundle_dir>` option to `guess`, `preview`, or `run` subcommand. `embulk mkbundle` also generates some example plugins to \<bundle_dir>/embulk/\*.rb directory.

See the generated \<bundle_dir>/Gemfile file how to plugin bundles work.

```
embulk mkbundle ./embulk_bundle  # please edit ./embulk_bundle/Gemfile to add plugins. Detailed usage is written in the Gemfile
embulk guess -b ./embulk_bundle ...
embulk run   -b ./embulk_bundle ...
```

## Use cases

* [Scheduled bulk data loading to Elasticsearch + Kibana 5 from CSV files](http://www.embulk.org/docs/recipe/scheduled-csv-load-to-elasticsearch-kibana5.html)

For further details, visit [Embulk documentation](http://www.embulk.org/docs/).

## Upgrading to the latest version

Following command updates embulk itself to the latest released version.

```sh
embulk selfupdate
```

Following command updates embulk itself to the specific released version.

```sh
embulk selfupdate x.y.z
```

Older versions are available at [dl.embulk.org](http://dl.embulk.org).


## Embulk Development

### Build

```
./gradlew cli  # creates pkg/embulk-VERSION.jar
```

You can see JaCoCo's test coverage report at `${project}/build/reports/tests/index.html`
You can see Findbug's report at `${project}/build/reports/findbug/main.html`  # FIXME coverage information is not included somehow

You can use `classpath` task to use `bundle exec ./bin/embulk` for development:

```
./gradlew -t classpath  # -x test: skip test
./bin/embulk
```

To deploy artifacts to your local maven repository at ~/.m2/repository/:

```
./gradlew install
```

To compile the source code of embulk-core project only:

```
./gradlew :embulk-core:compileJava
```

Task `dependencies` shows dependency tree of embulk-core project:

```
./gradlew :embulk-core:dependencies
```

### Update JRuby

Modify `jrubyVersion` in `build.gradle` to update JRuby of Embulk.

### Documents

Embulk uses Sphinx, YARD (Ruby API) and JavaDoc (Java API) for document generation.

```
brew install python
pip install sphinx
./gradlew site
# documents are: embulk-docs/build/html
```

### Release

You need to add your bintray account information to ~/.gradle/gradle.properties

```
bintray_user=(bintray user name)
bintray_api_key=(bintray api key)
```

Modify `version` in `build.gradle` at a detached commit to bump Embulk version up.

```
git checkout --detach master
(Remove "-SNAPSHOT" in "version" in build.gradle.)
git add build.gradle
git commit -m "Release vX.Y.Z"
git tag -a vX.Y.Z
(Write the release note for vX.Y.Z in the tag annotation.)
./gradlew clean && ./gradlew release
git push -u origin vX.Y.Z
```

See also:
* [Bintray](https://bintray.com)
* [How to acquire bintray API Keys](https://bintray.com/docs/usermanual/interacting/interacting_editingyouruserprofile.html#anchorAPIKEY)
