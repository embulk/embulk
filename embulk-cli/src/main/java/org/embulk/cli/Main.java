package org.embulk.cli;

import java.net.URISyntaxException;

public class Main
{
    public static void main(String[] args)
    {
        // $ java -jar jruby-complete.jar embulk-core.jar!/embulk/command/embulk_bundle.rb "$@"
        String[] jrubyArgs = new String[args.length + 1];
        String resourcePath;
        try {
            resourcePath = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().toString() + "!/";
        }
        catch (URISyntaxException ex) {
            resourcePath = "classpath:";
        }
        jrubyArgs[0] = resourcePath + "embulk/command/embulk_bundle.rb";
        System.arraycopy(args, 0, jrubyArgs, 1, args.length);
        org.jruby.Main.main(jrubyArgs);
    }
}
