# External Variables

Embulk refers some variables from external environments: OS environment variables, Java properties, and Embulk's own system configs. One-place summary of the variables helps us developers as usages of the variables are distributed in the source code.

We basically should simplify and reduce usage of OS environment variables because they are inherited, and the inheritance easily confuse users.


## Embulk System Config Variables

### `max_threads`

To be described.

### `min_output_tasks`

To be described.

### `page_size`

To be described.

### `log_path` and `log_level`

To be described.

### `guess_plugins`

To be described.

### `use_global_ruby_runtime`

The JVM-wide singleton JRuby runtime is used to run Ruby plugins if `use_global_ruby_runtime` is `true`. It has been available since v0.6.3.

* https://github.com/embulk/embulk/pull/159

### `gem_home`

Gem's home and path settings are overridden by `gem_home`. It has been available since v0.8.2.

* https://github.com/embulk/embulk/pull/376

### `jruby_classpath`, `jruby_load_path`, `jruby_command_line_options`, `jruby_global_bundler_plugin_source_directory`, and `jruby_use_default_embulk_gem_home`

The JRuby runtime for plugins are initialized per these configurations. They are used only from `org.embulk.cli`, not guaranteed for external use. They are available since v0.8.32.

* https://github.com/embulk/embulk/pull/765


## Java Properties Related

### `user.home`

Plugins are loads from the directories under `${user.home}/.embulk`.

### `line.separator`

`line.separator` affects newline characters in the help messages.

### `java.io.tmpdir`

`java.io.tmpdir` determines the directory for temporary files by `org.embulk.exec.TempFileAllocator`.


## Environment Variables Related

### `GEM_HOME` and `GEM_PATH`

The environment variables `GEM_HOME` and `GEM_PATH` are overridden with `Gem.use_paths` always when Embulk is executed through CLI. The behavior is controlled directly by Embulk's system config `jruby_use_default_embulk_gem_home`. `jruby_use_default_embulk_gem_home` is always set `true` unless `-b` option is given. Even if `-b` is given, `GEM_HOME` and `GEM_PATH` are overridden by Bundler.

They are not overridden only when Embulk is executed through `EmbulkEmbed` (or deprecated `EmbulkService`).

They are directly overwritten as environment variables when executing subcommands `gem`, `bundle`, `exec`, and `irb`.

### `BUNDLE_GEMFILE`

The environment variable `BUNDLE_GEMFILE` is unconditionally overwritten whenever Ruby plugins are executed. If `-b` option is given, `BUNDLE_GEMFILE` is overwritten to `Gemfile` under the given directory. If `-b` is not specified, `BUNDLE_GEMFILE` is cleared.

### `M2_REPO`

To be described.
