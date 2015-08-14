package org.embulk.command;

import java.util.Map;

public interface LiquidTemplate
{
    String render(String source, Map<String, String> params);
}
