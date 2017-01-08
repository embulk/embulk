module Embulk
  def self.migrate_plugin(path)
    migrator = Migrator.new(path)

    if ms = migrator.match("**/build.gradle", /org\.embulk:embulk-core:([\d\.\+]+)?/)
      lang = :java
      from_ver = version(ms[0][1].gsub(/\++/, '0'))   # replaces "0.8.+" to "0.8.0"
      puts "Detected Java plugin for Embulk #{from_ver}..."

    elsif ms = migrator.match("**/*.gemspec", /add_(?:development_)?dependency\s+\W+embulk\W+\s+([\d\.]+)\W+/)
      lang = :ruby
      from_ver = version(ms[0][1])
      puts "Detected Ruby plugin for Embulk #{from_ver}..."

    elsif ms = migrator.match("**/*.gemspec", /embulk-/)
      lang = :ruby
      from_ver = version("0.1.0")
      puts "Detected Ruby plugin for unknown Embulk version..."

    else
      raise "Failed to detect plugin language and dependency version"
    end

    case lang
    when :java
      migrate_java_plugin(migrator, from_ver)
    when :ruby
      migrate_ruby_plugin(migrator, from_ver)
    end

    if migrator.modified_files.empty?
      puts "Done. No files are modified."
    else
      puts "Done. Please check modifieid files."
    end
  end

  def self.migrate_java_plugin(migrator, from_ver)
    if from_ver < version("0.7.0")
      # rename CommitReport to TaskReport
      migrator.replace("**/*.java", /(CommitReport)/, "TaskReport")
      migrator.replace("**/*.java", /(commitReport)/, "taskReport")
    end

    # upgrade gradle version
    if migrator.match("gradle/wrapper/gradle-wrapper.properties", /gradle-[23]\.\d+(\.\d+)?-/)
      # gradle < 3.2.1
      require 'embulk/data/package_data'
      data = PackageData.new("new", migrator.path)
      migrator.write "gradle/wrapper/gradle-wrapper.properties", data.content("java/gradle/wrapper/gradle-wrapper.properties")
      migrator.write "gradle/wrapper/gradle-wrapper.jar", data.bincontent("java/gradle/wrapper/gradle-wrapper.jar")
    end

    # add jsonColumn method
    if !migrator.match("**/*.java", /void\s+jsonColumn/) && ms = migrator.match("**/*.java", /^(\W+).*?void\s+timestampColumn/)
      indent = ms.first[1]
      replace =  <<EOF

#{indent}public void jsonColumn(Column column) {
#{indent}    throw new UnsupportedOperationException("This plugin doesn't support json type. Please try to upgrade version of the plugin using 'embulk gem update' command. If the latest version still doesn't support json type, please contact plugin developers, or change configuration of input plugin not to use json type.");
#{indent}}

#{indent}@Override
EOF
      migrator.replace("**/*.java", /(\r?\n)(\W+).*?void\s+timestampColumn/, replace)
    end

    # add sourceCompatibility and targetCompatibility
    unless migrator.match("build.gradle", /targetCompatibility/)
      migrator.insert_line("build.gradle", /^([ \t]*)dependencies\s*{/) {|m|
        "#{m[1]}targetCompatibility = 1.7\n"
      }
    end
    unless migrator.match("build.gradle", /sourceCompatibility/)
      migrator.insert_line("build.gradle", /^([ \t]*)targetCompatibility/) {|m|
        "#{m[1]}sourceCompatibility = 1.7"
      }
    end

    # add checkstyle
    unless migrator.match("build.gradle", /id\s+(?<quote>["'])checkstyle\k<quote>/)
      migrator.insert_line("build.gradle", /^([ \t]*)id( +)(["'])java["']/) {|m|
        "#{m[1]}id#{m[2]}#{m[3]}checkstyle#{m[3]}"
      }
      migrator.write "config/checkstyle/checkstyle.xml", migrator.new_data.content("java/config/checkstyle/checkstyle.xml")
    end

    unless migrator.match("build.gradle", /checkstyle\s+{/)
      migrator.write "config/checkstyle/default.xml", migrator.new_data.content("java/config/checkstyle/default.xml")
      migrator.insert_line("build.gradle", /^([ \t]*)task\s+gem\W.*{/) {|m|
        <<-EOF
#{m[1]}checkstyle {
#{m[1]}    configFile = file("${project.rootDir}/config/checkstyle/checkstyle.xml")
#{m[1]}    toolVersion = '6.14.1'
#{m[1]}}
#{m[1]}checkstyleMain {
#{m[1]}    configFile = file("${project.rootDir}/config/checkstyle/default.xml")
#{m[1]}    ignoreFailures = true
#{m[1]}}
#{m[1]}checkstyleTest {
#{m[1]}    configFile = file("${project.rootDir}/config/checkstyle/default.xml")
#{m[1]}    ignoreFailures = true
#{m[1]}}
#{m[1]}task checkstyle(type: Checkstyle) {
#{m[1]}    classpath = sourceSets.main.output + sourceSets.test.output
#{m[1]}    source = sourceSets.main.allJava + sourceSets.test.allJava
#{m[1]}}
EOF
      }
    end

    #
    # add rules...
    ##

    # update version at the end
    migrator.replace("**/build.gradle", /org\.embulk:embulk-(?:core|standards):([\d\.\+]+)?/, Embulk::VERSION)
  end

  def self.migrate_ruby_plugin(migrator, from_ver)
    #
    # add rules...
    ##

    migrator.write(".ruby-version", "jruby-9.1.5.0")

    # update version at the end
    if from_ver <= version("0.1.0")
      # add add_development_dependency
      migrator.insert_line("**/*.gemspec", /([ \t]*\w+)\.add_development_dependency/) {|m|
        "#{m[1]}.add_development_dependency 'embulk', ['>= #{Embulk::VERSION}']"
      }
    else
      unless migrator.replace("**/*.gemspec", /add_(?:development_)?dependency\s+\W+embulk\W+\s*(\~\>\s*[\d\.]+)\W+/, ">= #{Embulk::VERSION}")
        migrator.replace("**/*.gemspec", /add_(?:development_)?dependency\s+\W+embulk\W+\s*([\d\.]+)\W+/, Embulk::VERSION)
      end
    end
  end

  private

  def self.version(str)
    Gem::Version.new(str)
  end

  class Migrator
    def initialize(path)
      @path = path
      @modified_files = {}

      require 'fileutils'
      require 'embulk/data/package_data'
      @new_data = PackageData.new("new", path)
    end

    attr_reader :path
    attr_reader :new_data

    def modified_files
      @modified_files.keys
    end

    def match(glob, pattern)
      ms = Dir[File.join(@path, glob)].map do |file|
        read(file).match(pattern)
      end.compact
      return nil if ms.empty?
      ms
    end

    def replace(glob, pattern, text=nil)
      ms = Dir[File.join(@path, glob)].map do |file|
        data = read(file)
        first = nil
        pos = 0
        while pos < data.length
          m = data.match(pattern, pos)
          break unless m
          first ||= m
          replace = text || yield(m)
          data = m.pre_match + data[m.begin(0)..(m.begin(1)-1)] + replace + data[m.end(1)..(m.end(0)-1)] + m.post_match
          pos = m.begin(1) + replace.length + (m.end(0) - m.end(1))
        end
        if first
          modify(file, data)
        end
        first
      end.compact
      return nil if ms.empty?
      ms
    end

    def insert_line(glob, pattern, text: nil)
      ms = Dir[File.join(@path, glob)].map do |file|
        data = read(file)
        if m = data.match(pattern)
          ln = m.pre_match.split("\n").count
          replace = text || yield(m)
          lines = data.split("\n", -1)  # preserve the last empty line
          lines.insert(ln + 1, replace)
          data = lines.join("\n")
          modify(file, data)
          m
        end
      end.compact
      return nil if ms.empty?
      ms
    end

    def data(file)
      read(File.join(@path, file))
    end

    def write(file, data)
      dst = File.join(@path, file)
      FileUtils.mkdir_p File.dirname(dst)
      modify(dst, data)
    end

    private

    def modify(path, data)
      orig = read(path) rescue nil
      if orig != data
        File.write(path, data)
        unless @modified_files.has_key?(path)
          if orig
            puts "  Modified #{path.sub(/^#{Regexp.escape(@path)}/, '')}"
          else
            puts "  Created #{path.sub(/^#{Regexp.escape(@path)}/, '')}"
          end
          @modified_files[path] = true
        end
      end
      nil
    end

    def read(path)
      # external_encoding: assumes source code is written in UTF-8.
      # internal_encoding: process files using UTF-8 so that modified data (data written to the file) becomes UTF-8.
      File.read(path, external_encoding: 'UTF-8', internal_encoding: 'UTF-8')
    end
  end
end
