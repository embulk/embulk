package org.embulk.cli;

import java.net.URISyntaxException;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.ProtectionDomain;

public class Main
{
    public static void main(String[] args)
    {
        java.security.Policy.setPolicy(new java.security.Policy() {
                // NOTE: Policy#getPermissions(ProtectionDomain) calls Policy#getPermissions(CodeSource)
                // internally. Overriding just Policy#getPermissions(CodeSource) is usually fine.
                @Override
                public PermissionCollection getPermissions(CodeSource codeSource)
                {
                    Permissions permissions = new Permissions();
                    permissions.add(new java.security.AllPermission());
                    return permissions;
                }

                @Override
                public PermissionCollection getPermissions(ProtectionDomain protectionDomain)
                {
                    if (protectionDomain.getClassLoader() instanceof org.embulk.plugin.PluginClassLoader) {
                        return new Permissions();
                    }
                    return super.getPermissions(protectionDomain);
                }
            });
        System.setSecurityManager(new SecurityManager());

        // $ java -jar jruby-complete.jar embulk-core.jar!/embulk/command/embulk_bundle.rb "$@"
        String[] jrubyArgs = new String[args.length + 1];
        int i;
        for (i = 0; i < args.length; i++) {
            if (args[i].startsWith("-R")) {
                jrubyArgs[i] = args[i].substring(2);
            } else {
                break;
            }
        }
        jrubyArgs[i] = getScriptPath();
        for (; i < args.length; i++) {
            jrubyArgs[i+1] = args[i];
        }
        org.jruby.Main.main(jrubyArgs);
    }

    private static String getScriptPath()
    {
        String resourcePath;
        try {
            resourcePath = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().toString() + "!/";
        }
        catch (URISyntaxException ex) {
            resourcePath = "uri:classloader:/";
        }
        return resourcePath + "embulk/command/embulk_bundle.rb";
    }
}
