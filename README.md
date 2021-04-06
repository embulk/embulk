# What's Embulk?

Embulk is a parallel bulk data loader that **helps data transfer between various storages, databases, NoSQL and cloud services**.

**Embulk supports plugins** to add functions. You can [share the plugins](https://plugins.embulk.org/) to keep your custom scripts readable, maintainable, and reusable.

[![Embulk](https://gist.githubusercontent.com/frsyuki/f322a77ee2766a508ba9/raw/e8539b6b4fda1b3357e8c79d3966aa8148dbdbd3/embulk-overview.png)](http://www.slideshare.net/frsyuki/embuk-making-data-integration-works-relaxed/12)
[Embulk, an open-source plugin-based parallel bulk data loader](http://www.slideshare.net/frsyuki/embuk-making-data-integration-works-relaxed) at Slideshare

# Document

Embulk documents: https://www.embulk.org/

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

* [Scheduled bulk data loading to Elasticsearch + Kibana 5 from CSV files](https://www.embulk.org/recipes/scheduled-csv-load-to-elasticsearch-kibana5.html)

For further details, visit [Embulk documentation](https://www.embulk.org/).

## Upgrading to the latest version

Following command updates embulk itself to the specific released version.

```sh
embulk selfupdate x.y.z
```

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

### Release

#### Prerequisite: Sonatype OSSRH

You need an account in [Sonatype OSSRH](https://central.sonatype.org/pages/ossrh-guide.html), and configure it in your `~/.gradle/gradle.properties`.

```
ossrhUsername=(your Sonatype OSSRH username)
ossrhPassword=(your Sonatype OSSRH password)
```

#### Prerequisite: PGP signatures

You need your [PGP signatures to release artifacts into Maven Central](https://central.sonatype.org/pages/working-with-pgp-signatures.html), and [configure Gradle to use your key to sign](https://docs.gradle.org/current/userguide/signing_plugin.html).

```
signing.keyId=(the last 8 symbols of your keyId)
signing.password=(the passphrase used to protect your private key)
signing.secretKeyRingFile=(the absolute path to the secret key ring file containing your private key)
```

#### Release

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
