module Embulk
  def self.selfupdate(options={})
    require 'uri'

    jar, resource = __FILE__.split("!", 2)
    jar_path = URI.parse(jar).path rescue jar
    unless resource && File.file?(jar_path)
      STDERR.puts ""
      STDERR.puts "Embulk is not installed by a single jar. Selfupdate is not supported."
      STDERR.puts "If you used gem to install embulk, please run: "
      STDERR.puts ""
      STDERR.puts "  $ gem install embulk"
      STDERR.puts ""
      raise SystemExit.new(1)
    end

    puts "Checking the latest version..."
    latest_version = check_latest_version

    if Gem::Version.new(latest_version) <= Gem::Version.new(Embulk::VERSION)
      puts "Already up-to-date. #{latest_version} is the latest version."
      return
    end

    puts "Found new version #{latest_version}."

    unless File.writable?(jar_path)
      STDERR.puts ""
      STDERR.puts "Installation path #{jar_path} is not writable."
      STDERR.puts "Dou you need to run with sudo?"
      STDERR.puts ""
      raise SystemExit.new(1)
    end

    url = "https://dl.bintray.com/embulk/maven/embulk-#{latest_version}.jar"
    puts "Downloading #{url} ..."

    require 'open-uri'
    require 'tempfile'
    Tempfile.open(["embulk-selfupdate", ".jar"]) do |tmp|
      tmp.chmod(File.stat(jar_path).mode)
      OpenURI.open_uri(url) do |f|
        IO.copy_stream(f, tmp)
      end
      tmp.close(false)

      # check corruption
      unless options[:force]
        begin
          data = File.read("jar:#{java.io.File.new(tmp.path).toURI.toURL}!/embulk/version.rb")
          m = Module.new
          m.module_eval(data)
          unless m::Embulk::VERSION == latest_version
            raise "Embulk::VERSION does not match with #{latest_version}"
          end
        rescue => e
          STDERR.puts "Corruption checking failed (#{e})."
          STDERR.puts "This version might include incompatible changes."
          STDERR.puts "Please add '-f' argument to selfupdate command to skip checking."
          raise SystemExit.new(1, e.to_s)
        end
      end

      File.rename(tmp.path, jar_path)
    end

    puts "Updated to #{latest_version}."
  end

  def self.check_latest_version
    require 'net/https'
    bintray = Net::HTTP.new('bintray.com', 443)
    bintray.use_ssl = true
    bintray.verify_mode = OpenSSL::SSL::VERIFY_NONE
    bintray.start do
      response = bintray.get('/embulk/maven/embulk/_latestVersion')
      raise "Expected response code 302 Found but got #{response.code}" if response.code != "302"
      location = response["Location"].to_s
      m = /(\d+\.\d+[^\/]+)/.match(location)
      raise "Cound not find version number in Location header '#{location}'" unless m
      return m[1]
    end
  end
end
