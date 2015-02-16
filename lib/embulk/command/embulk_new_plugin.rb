module Embulk
  def self.new_plugin(name, language, category)
    require 'embulk/data/package_data'
    require 'embulk/version'
    require 'fileutils'

    embulk_category = category
    embulk_category = :input if category == :file_input
    embulk_category = :output if category == :file_output

    project_name = "embulk-#{embulk_category}-#{name}"
    plugin_path = "lib/embulk/#{embulk_category}/#{name}.rb"

    if File.exist?(project_name)
      raise "./#{project_name} already exists. Please delete it first."
    end
    FileUtils.mkdir_p(project_name)

    puts "Creating #{project_name}/"

    success = false
    begin
      author = `git config user.name`.strip
      author = "YOUR_NAME" if author.empty?
      email = `git config user.email`.strip
      email = "YOUR_NAME" if email.empty?

      ruby_class_name = name.split('-').map {|a| a.capitalize }.join + category.to_s.split('_').map {|a| a.capitalize }.join + "Plugin"
      java_iface = category.to_s.split('_').map {|a| a.capitalize }.join
      java_class_name = name.split('-').map {|a| a.capitalize }.join + java_iface + "Plugin"
      display_name = name.split('-').map {|a| a.capitalize }.join(' ')
      display_category = category.to_s.gsub('_', ' ')

      description =
        case category
        when :input
          %[that loads records from #{display_name} so that any output plugins can receive the records. Search the output plugins by 'embulk-output' keyword.]
        when :file_input
          %[that reads files from #{display_name} and parses the file using any parser plugins. Search the parser plugins by 'embulk-parser' keyword.]
        when :parser
          %[that parses #{display_name} file format read by any file input plugins. Search the file input plugins by 'embulk-input file' keywords.]
        when :decoder
          %[that decodes files encoded by #{display_name} read by any file input plugins. Search the file input plugins by 'embulk-input file' keywords.]
        when :output
          %[that loads records to #{display_name} read by any input plugins. Search the input plugins by 'embulk-input' keyword.]
        when :file_output
          %[that stores files to #{display_name} formatted by any formatter plugins. Search the formatter plugins by 'embulk-formatter' keyword.]
        when :formtter
          %[that formats records using #{display_name} file format and so that any file output plugins can store the files. Search the file output plugins by 'embulk-output file' keywords.]
        when :encoder
          %[that encodes files using #{display_name} so that any file output plugins can store the files. Search the file output plugins by 'embulk-output file' keywords.]
        when :filter
          %[that converts records read by an input plugin before passing it to an output plugins. Search the input and plugins by 'embulk-input' and 'embulk-output' plugins.]
        end

      pkg = Embulk::PackageData.new("new", project_name, binding())

      pkg.cp_erb("README.md.erb", "README.md")
      pkg.cp("LICENSE.txt", "LICENSE.txt")
      pkg.cp_erb("gitignore.erb", ".gitignore")

      case language
      when :ruby
        pkg.cp("ruby/Rakefile", "Rakefile")
        pkg.cp("ruby/Gemfile", "Gemfile")
        pkg.cp_erb("ruby/gemspec.erb", "#{project_name}.gemspec")
        pkg.cp_erb("ruby/#{category}.rb.erb", plugin_path)

      when :java
        pkg.cp("java/gradle/wrapper/gradle-wrapper.jar", "gradle/wrapper/gradle-wrapper.jar")
        pkg.cp("java/gradle/wrapper/gradle-wrapper.properties", "gradle/wrapper/gradle-wrapper.properties")
        pkg.cp("java/gradlew.bat", "gradlew.bat")
        pkg.cp("java/gradlew", "gradlew")
        pkg.set_executable("gradlew")
        pkg.cp_erb("java/build.gradle.erb", "build.gradle")
        pkg.cp_erb("java/plugin_loader.rb.erb", plugin_path)
        pkg.cp_erb("java/#{category}.java.erb", "src/main/java/org/embulk/#{embulk_category}/#{java_class_name}.java")
        pkg.cp_erb("java/test.java.erb", "src/test/java/org/embulk/#{embulk_category}/Test#{java_class_name}.java")
      end

      success = true
      puts ""
    ensure
      FileUtils.rm_rf project_name unless success
    end
  end
end
