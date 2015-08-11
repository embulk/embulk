module Embulk
  module Java
    require 'liquid'

    class LiquidTemplateHelper
      include org.embulk.command.LiquidTemplate

      def render(source, params)
        template = Liquid::Template.parse(source)
        data = {
          "env" => ENV.to_h,
        }.merge(params)
        template.render(data)
      end
    end
  end
end
