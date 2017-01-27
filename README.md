# What's Embulk?

Embulk is a parallel bulk data loader that **helps data transfer between various storages, databases, NoSQL and cloud services**.

**Embulk supports plugins** to add functions. You can [share the plugins](http://www.embulk.org/plugins/) to keep your custom scripts readable, maintainable, and reusable.

[![Embulk](https://gist.githubusercontent.com/frsyuki/f322a77ee2766a508ba9/raw/e8539b6b4fda1b3357e8c79d3966aa8148dbdbd3/embulk-overview.png)](http://www.slideshare.net/frsyuki/embuk-making-data-integration-works-relaxed/12)
[Embulk, an open-source plugin-based parallel bulk data loader](http://www.slideshare.net/frsyuki/embuk-making-data-integration-works-relaxed) at Slideshare

# Document

Embulk documents: http://www.embulk.org/docs/

## Quick Start

### Linux & Mac & BSD

Embulk is a Java application. Please make sure that [Java](http://www.oracle.com/technetwork/java/javase/downloads/index.html) is installed.

Following 4 commands install embulk to your home directory:

```sh
curl --create-dirs -o ~/.embulk/bin/embulk -L "http://dl.embulk.org/embulk-latest.jar"
chmod +x ~/.embulk/bin/embulk
echo 'export PATH="$HOME/.embulk/bin:$PATH"' >> ~/.bashrc
source ~/.bashrc
```

Next step: [Running example in 4 commands](#running-example)

### Windows

Embulk is a Java application. Please make sure that [Java](http://www.oracle.com/technetwork/java/javase/downloads/index.html) is installed.

You can download `embulk.bat` using this command on cmd.exe or PowerShell.exe:

```
PowerShell -Command "& {Invoke-WebRequest http://dl.embulk.org/embulk-latest.jar -OutFile embulk.bat}"
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

You can use plugins to load data from/to various systems and file formats. Here is the list of publicly released plugins: [list of plugins by category](http://www.embulk.org/plugins/).

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

* [Scheduled bulk data loading to Elasticsearch + Kibana 4 from CSV files](http://www.embulk.org/docs/recipe/scheduled-csv-load-to-elasticsearch-kibana4.html)

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
./gradlew gem  # creates pkg/embulk-VERSION.gem
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

### Documents

Embulk uses Sphinx, YARD (Ruby API) and JavaDoc (Java API) for document generation.

```
brew install python
pip install sphinx
gem install yard
./gradlew site
# documents are: embulk-docs/build/html
```

### Release

You need to add your bintray account information to ~/.gradle/gradle.properties

```
bintray_user=(bintray user name)
bintray_api_key=(bintray api key)
```

Run following commands and follow its instruction:

```
./gradlew setVersion -Pto=$VERSION
```

```
./gradlew releaseCheck
./gradlew clean cli gem && ./gradlew release
git commit -am v$VERSION
git tag v$VERSION
```

See also:
* [Bintray](https://bintray.com)
* [How to acquire bintray API Keys](https://bintray.com/docs/usermanual/interacting/interacting_editingyouruserprofile.html#anchorAPIKEY)

