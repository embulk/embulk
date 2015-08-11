module Embulk
  def self.add_embedded_gem_path
    begin
      resource_class = org.embulk.command.Runner.java_class
    rescue NameError
      begin
        Embulk.require_classpath
        resource_class = org.embulk.command.Runner.java_class
      rescue NameError
        return nil
      end
    end

    gem_path = resource_class.resource("/embulk/gems").to_s.sub(/^jar:/, '')
    unless gem_path.empty?
      # GEM_PATH can't include ':'
      gem_path = gem_path.gsub(/\w{0,5}:(?:\/(?=\/))*/, "")
      orig = ENV['GEM_PATH'].to_s
      if orig.empty?
        ENV['GEM_PATH'] = gem_path
      elsif !orig.split(Gem.path_separator).include?(gem_path)
        ENV['GEM_PATH'] = "#{gem_path}:#{orig}"
      end
      Gem.clear_paths
      return ENV['GEM_PATH']
    end
    return nil
  end
end
