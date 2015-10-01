module Embulk
  def self.selfupdate(options={})
    require 'uri'

    jar, resource = __FILE__.split("!", 2)
    jar_uri = URI.parse(jar).path rescue jar
    unless resource && File.file?(jar_uri)
      STDERR.puts ""
      STDERR.puts "Embulk is not installed by a single jar. Selfupdate is not supported."
      STDERR.puts "If you used gem to install embulk, please run: "
      STDERR.puts ""
      STDERR.puts "  $ gem install embulk"
      STDERR.puts ""
      raise SystemExit.new(1)
    end
    jar_path = jar_uri

    if version = options[:version]
      puts "Checking version #{version}..."
      target_version = check_target_version(version)

      unless target_version
        puts "Specified version does not exist: #{version}"
        raise SystemExit.new(1)
      end
      puts "Found version #{target_version}."

    else
      puts "Checking the latest version..."
      target_version = check_latest_version

      current_version = Gem::Version.new(Embulk::VERSION)
      if Gem::Version.new(target_version) <= current_version
        puts "Already up-to-date. #{current_version} is the latest version."
        return
      end

      puts "Found new version #{target_version}."
    end

    unless File.writable?(jar_path)
      STDERR.puts ""
      STDERR.puts "Installation path #{jar_path} is not writable."
      STDERR.puts "Dou you need to run with sudo?"
      STDERR.puts ""
      raise SystemExit.new(1)
    end

    url = "https://dl.bintray.com/embulk/maven/embulk-#{target_version}.jar"
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
          unless m::Embulk::VERSION == target_version
            raise "Embulk::VERSION does not match with #{target_version}"
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

    puts "Updated to #{target_version}."
  end

  private

  def self.check_latest_version
    start_bintray_http do |http|
      response = http.get('/embulk/maven/embulk/_latestVersion')
      if response.code != "302"
        raise "Expected response code 302 Found but got #{response.code}"
      end
      location = response["Location"].to_s
      m = /(\d+\.\d+[^\/]+)/.match(location)
      unless m
        raise "Cound not find version number in Location header '#{location}'"
      end
      return m[1]
    end
  end

  def self.check_target_version(version=nil)
    start_bintray_http do |http|
      response = http.get("/embulk/maven/embulk/#{version}")
      if response.code == "404"
        return nil
      elsif response.code != "200"
        raise "Unexpected response code: #{response.code}"
      else
        return version
      end
    end
  end

  def self.start_bintray_http(&block)
    require 'net/https'
    bintray = Net::HTTP.new('bintray.com', 443)
    bintray.use_ssl = true
    bintray.verify_mode = OpenSSL::SSL::VERIFY_NONE
    bintray.start(&block)
  end
end
