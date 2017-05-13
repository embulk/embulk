Release 0.8.19
==================================

General Changes
------------------

* Fixed LocalFileInputPlugin to follow link if path prefix is symlink. (@hiroyuki-sato) [#585]

* Fixed CsvParserPlugin to refresh the number of skipped header lines for each file. [#567]

* Fixed embulk/guess/csv.rb to finish immidiately if CSV parser could not parse sample_lines. [#556]

* Fixed PageBuilder to avoid saving unexpected string values saving to stringReferences. [#598]

* Fixed PreviewExecutor to avoid to call SamplingPageOutput#finish twice. [#571]

* Fixed TempFileSpace to change temporary filename format for Windows safe. (@hiroyuki-sato) [#589]

* Added 'preview_sample_buffer_bytes' option for 'preview' command to make sampling buffer bytes configurable. [#572]

* Added 'guess_sample_buffer_bytes' option for 'guess' command to make sampling buffer bytes configurable. [#594]

* Updated snakeyaml from 1.14 to 1.18. (@hiroyuki-sato) [#575]

* Updated liquid from 3.0.6 to 4.0.0. (@hiroyuki-sato)  [#587]

* Minor fix:

  * Removed lib/embulk/command/embulk_generate_bin.rb that is not called anywhere. [#578]

  * Deprecated lib/embulk/version.rb 'Embulk::VERSION', and used the jar manifest (META-INF/MANIFEST.MF) to provide the Embulk version at runtime [#596, #597]

  * Rewrote the "example" subcommand in Java. [#558]

  * Rewrote the "selfupdate" subcommand in Java. [#563]

  * Rewrote the "migrate" subcommand in Java. [#568]

  * Rewrote the "new" subcommand in Java. [#569]

Release Date
------------------
2017-05-12
