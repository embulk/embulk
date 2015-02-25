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
          %[Loads records from #{display_name}.]
        when :file_input
          %[Reads files stored on #{display_name}.]
        when :parser
          %[Parses #{display_name} files read by other file input plugins.]
        when :decoder
          %[Decodes #{display_name}-encoded files read by other file input plugins.]
        when :output
          %[Dumps records to #{display_name}.]
        when :file_output
          %[Stores files on #{display_name}.]
        when :formtter
          %[Formats #{display_name} files for other file output plugins.]
        when :encoder
          %[Encodes files using #{display_name} for other file output plugins.]
        when :filter
          %[#{display_name}]
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
