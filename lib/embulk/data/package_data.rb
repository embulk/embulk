module Embulk

  class PackageData
    def initialize(base_name, dest_dir, erb_binding=nil)
      require 'fileutils'
      @base_name = base_name
      @dest_dir = dest_dir
      @erb_binding = erb_binding
    end

    def path(src)
      Embulk.lib_path("embulk/data/#{@base_name}/#{src}")
    end

    def content(src)
      File.read(path(src))
    end

    def bincontent(src)
      File.binread(path(src))
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
