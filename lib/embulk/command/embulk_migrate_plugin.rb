module Embulk
  def self.migrate_plugin(path)
    migrator = Migrator.new(path)

    if ms = migrator.match("**/build.gradle", /org\.embulk:embulk-core:([\d\.]+)?/)
      lang = :java
      from_ver = version(ms[0][1])
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

    migrator.replace("**/build.gradle", /org\.embulk:embulk-(?:core|standards):([\d\.]+)?/, Embulk::VERSION)
  end

  def self.migrate_ruby_plugin(migrator, from_ver)
    if from_ver <= version("0.1.0")
      # add add_development_dependency
      migrator.insert_line("**/*.gemspec", /([ \t]*\w+)\.add_development_dependency/) {|m|
        "#{m[1]}.add_development_dependency 'embulk', ['~> #{Embulk::VERSION}']"
      }
    else
      migrator.replace("**/*.gemspec", /add_(?:development_)?dependency\s+\W+embulk\W+\s+([\d\.]+)\W+/, Embulk::VERSION)
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
    end

    def modified_files
      @modified_files.keys
    end

    def match(glob, pattern)
      ms = Dir[File.join(@path, glob)].map do |file|
        File.read(file).match(pattern)
      end.compact
      return nil if ms.empty?
      ms
    end

    def replace(glob, pattern, text=nil)
      ms = Dir[File.join(@path, glob)].map do |file|
        data = File.read(file)
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

    def insert_line(glob, pattern, text=nil)
      ms = Dir[File.join(@path, glob)].map do |file|
        data = File.read(file)
        if m = data.match(pattern)
          ln = m.pre_match.split("\n").count
          replace = text || yield(m)
          lines = data.split("\n")
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
      File.read(File.join(@path, file))
    end

    def modify(file, data)
      if File.read(file) != data
        File.write(file, data)
        unless @modified_files.has_key?(file)
          puts "  Modified #{file.sub(/^#{Regexp.escape(@path)}/, '')}"
          @modified_files[file] = true
        end
      end
      nil
    end
  end
end
