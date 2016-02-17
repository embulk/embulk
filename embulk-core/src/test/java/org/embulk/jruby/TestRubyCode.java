package org.embulk;

import org.jruby.Main;
import org.junit.Test;

public class TestRubyCode
{
    @Test
    public void runAllRubyTests()
    {
        Main.main(new String[] {
            "-I", "src/test/ruby",
            "src/test/ruby/test.rb"
        });
    }
}
