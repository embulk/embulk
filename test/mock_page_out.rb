class MockPageOut
  include org.embulk.spi.PageOutput

  def initialize
    @pages = []
  end

  attr_reader :pages

  def add(page)
    @pages << page
  end

  def finish
  end

  def close
  end
end

