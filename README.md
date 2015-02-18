# What's Embulk?

Embulk is a plugin-based parallel bulk data loader that helps **data transfer** between various **storages**, **databases**, **NoSQL** and **cloud services**.

You can release plugins to share your efforts of data cleaning, error handling, transaction control, and retrying. Packaging effrots into plugins **brings OSS-style development to the data scripts** which **was tend to be one-time adhoc scripts**.

[Embulk, an open-source plugin-based parallel bulk data loader](http://www.slideshare.net/frsyuki/embuk-making-data-integration-works-relaxed) at Slideshare

[![Embulk](https://gist.githubusercontent.com/frsyuki/f322a77ee2766a508ba9/raw/e8539b6b4fda1b3357e8c79d3966aa8148dbdbd3/embulk-overview.png)](http://www.slideshare.net/frsyuki/embuk-making-data-integration-works-relaxed/12)

# Document

* [Quick Start](#quick-start)
  * [Using plugins](#using-plugins)
  * [Using plugin bundle](#using-plugin-bundle)
  * [Releasing plugins to RubyGems](#releasing-plugins-to-rubygems)
  * [Resuming a failed transaction](#resuming-a-failed-transaction)
* [Embulk Development](#embulk-development)
  * [Build](#build)
  * [Release](#release)

## Quick Start

The single-file package is the simplest way to try Embulk. You can download the latest embulk-VERSION.jar from [the releases page](https://bintray.com/embulk/maven/embulk/view#files) and run it with java:

```
wget https://bintray.com/artifact/download/embulk/maven/embulk-0.4.3.jar -O embulk.jar
java -jar embulk.jar --help
```

Let's load a CSV file, for example. `embulk example` subcommand generates a csv file and config file for you.

```
java -jar embulk.jar example ./try1
java -jar embulk.jar guess   ./try1/example.yml -o config.yml
java -jar embulk.jar preview config.yml
java -jar embulk.jar run     config.yml
```

### Using plugins

You can use plugins to load data from/to various systems and file formats.
An example is [embulk-plugin-postgres-json](https://github.com/frsyuki/embulk-plugin-postgres-json) plugin. It outputs data into PostgreSQL server using "json" column type.

```
java -jar embulk.jar gem install embulk-plugin-postgres-json
java -jar embulk.jar gem list
```

You can search plugins on RubyGems: [search for "embulk"](https://rubygems.org/search?utf8=%E2%9C%93&query=embulk).

### Using plugin bundle

`embulk bundle` subcommand creates (or updates if already exists) a *plugin bundle* directory.
You can use the bundle using `-b <bundle_dir>` option. `embulk bundle` also generates some example plugins to \<bundle_dir>/embulk/\*.rb directory.

See generated \<bundle_dir>/Gemfile file how to plugin bundles work.

```
java -jar embulk.jar bundle ./embulk_bundle
java -jar embulk.jar guess  -b ./embulk_bundle ...
java -jar embulk.jar run    -b ./embulk_bundle ...
```

### Releasing plugins to RubyGems

TODO: documents

```
embulk-plugin-xyz
```

### Resuming a failed transaction

Embulk supports resuming failed transactions.
To enable resuming, you need to start transaction with `-r PATH` option:

```
java -jar embulk.jar run config.yml -r resume-state.yml
```

If the transaction fails, embulk stores state some states to the yaml file. You can retry the transaction using exactly same command:

```
java -jar embulk.jar run config.yml -r resume-state.yml
```

If you giveup to resume the transaction, you can use `embulk cleanup` subcommand to delete intermediate data:

```
java -jar embulk.jar cleanup config.yml -r resume-state.yml
```


## Embulk Development

### Build

```
./gradlew cli  # creates pkg/embulk-VERSION.jar
./gradlew gem  # creates pkg/embulk-VERSION.gem
```

You can see JaCoCo's test coverage report at `${project}/build/reports/tests/index.html`
You can see Findbug's report at `${project}/build/reports/findbug/main.html`  # FIXME coverage information is not included somehow

You can use `classpath` task to use `./bin/embulk` for development:

```
./gradlew classpath  # -x test: skip test
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

Update following files:

* embulk-docs/src/release/release-VERSION.rst (release note)
* build.gradle (version number)
* lib/embulk/version.rb (version number)

Then, build and upload using gradle:

```
./gradlew releaseCheck
./gradlew cli gem
./gradlew bintrayUpload
gem push pkg/embulk-....gem
open "https://bintray.com/embulk/maven/embulk"  # and upload pkg/embulk-....jar
```

See also:
* [Bintray](https://bintray.com)
* [How to acquire bintray API Keys](https://bintray.com/docs/usermanual/interacting/interacting_apikeys.html)

