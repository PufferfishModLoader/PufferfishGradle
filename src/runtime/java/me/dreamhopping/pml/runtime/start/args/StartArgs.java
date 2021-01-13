package me.dreamhopping.pml.runtime.start.args;

import java.util.ArrayList;
import java.util.List;

public class StartArgs {
    private final List<String> literalArguments;
    private final String authUsername;
    private final String authPassword;

    public StartArgs(List<String> literalArguments, String authUsername, String authPassword) {
        this.literalArguments = literalArguments;
        this.authUsername = authUsername;
        this.authPassword = authPassword;
    }

    public static StartArgs parse(String[] args) {
        int type = 0;
        List<String> literalArgs = new ArrayList<>();
        String username = null;
        String password = null;

        for (String arg : args) {
            if (type == 0) {
                switch (arg) {
                    case "--username":
                        type = 1;
                        break;
                    case "--password":
                        type = 2;
                        break;
                    default:
                        literalArgs.add(arg);
                        break;
                }
            } else if (type == 1) {
                username = arg;
                type = 0;
            } else {
                password = arg;
                type = 0;
            }
        }

        if (username != null && password == null) {
            literalArgs.add("--username");
            literalArgs.add(username);
            username = null;
        } else if (password != null && username == null) {
            literalArgs.add("--password");
            literalArgs.add(password);
            password = null;
        }

        return new StartArgs(literalArgs, username, password);
    }

    public List<String> getLiteralArguments() {
        return literalArguments;
    }

    public String getAuthUsername() {
        return authUsername;
    }

    public String getAuthPassword() {
        return authPassword;
    }
}
