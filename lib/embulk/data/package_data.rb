module Embulk

  class PackageData
    if __FILE__ =~ /^classpath:/ || __FILE__.include?('!/')
      # data is in embulk-core jar
      resource_class = org.embulk.command.Runner.java_class
      JAVA_RESOURCE = true
      RESOURCE_URL = resource_class.resource("/embulk/data")
    else
      JAVA_RESOURCE = false
      FILE_BASE_PATH = File.join(Embulk.home('lib'), 'embulk', 'data')
    end

    def initialize(base_name, dest_dir, erb_binding=nil)
      require 'fileutils'
      @base_name = base_name
      @dest_dir = dest_dir
      @erb_binding = erb_binding
    end

    def path(src)
      if JAVA_RESOURCE
        "#{RESOURCE_URL}/#{@base_name}/#{src}"
      else
        File.join(FILE_BASE_PATH, @base_name, src)
      end
    end

    def content(src)
      File.read(path(src))
    end

    def erb(src)
      require 'erb'
      ERB.new(content(src), nil, '%').result(@erb_binding)
    end

    def cp(src, dest_name)
      path = dest_path_message(dest_name)
      FileUtils.cp path(src), path
    end

    def cp_erb(src, dest_name)
      path = dest_path_message(dest_name)
      File.open(path, "w") {|f| f.write erb(src) }
    end

    def dest_path(dest_name)
      File.join(@dest_dir, *dest_name.split('/'))
    end

    def dest_path_message(dest_name)
      path = dest_path(dest_name)
      puts "  Creating #{path}"
      FileUtils.mkdir_p File.dirname(path)
      path
    end

    def set_executable(dest_name)
      File.chmod(0755, dest_path(dest_name))
    end
  end

end
