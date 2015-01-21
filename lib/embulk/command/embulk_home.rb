module Embulk
  def self.home(dir)
    home = File.expand_path('../../..', File.dirname(__FILE__))
    File.join(home, dir)
  end
end
