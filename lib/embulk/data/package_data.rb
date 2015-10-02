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
      dest = dest_path_message(dest_name)
      File.open(dest, "wb") do |dst_io|
        File.open(path(src), "rb") do |src_io|
          FileUtils.copy_stream src_io, dst_io
        end
      end
    end

    def cp_erb(src, dest_name)
      dest = dest_path_message(dest_name)
      File.open(dest, "wb") {|f| f.write erb(src) }
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
      dest = dest_path(dest_name)
      File.chmod(File.stat(dest).mode | 0111, dest)
    end
  end

end
