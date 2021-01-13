package me.dreamhopping.pml.runtime.start;

import me.dreamhopping.pml.runtime.start.args.StartArgs;
import me.dreamhopping.pml.runtime.start.auth.AuthData;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class Start {
    public static void main(String[] args) throws Throwable {
        StartArgs arguments = StartArgs.parse(args);

        boolean isServer = Boolean.parseBoolean(System.getenv().getOrDefault("PG_IS_SERVER", "false"));

        if (!isServer) {
            addArgument(arguments.getLiteralArguments(), "--version", () -> "PufferfishGradle");
            addArgument(arguments.getLiteralArguments(), "--assetIndex", () -> System.getenv("PG_ASSET_INDEX"));
            addArgument(arguments.getLiteralArguments(), "--assetsDir", () -> System.getenv("PG_ASSETS_DIR"));

            boolean shouldAuthenticate = arguments.getAuthUsername() != null && arguments.getAuthPassword() != null;
            String accessToken = "PufferfishGradle";
            String username = "Player";
            String uuid = new UUID(0, 0).toString();

            if (shouldAuthenticate) {
                AuthData data = AuthData.authenticate(arguments.getAuthUsername(), arguments.getAuthPassword());

                accessToken = data.getAccessToken();
                username = data.getUsername();
                uuid = data.getUuid();
            }

            addArgument(arguments.getLiteralArguments(), "--username", shouldAuthenticate, username);
            addArgument(arguments.getLiteralArguments(), "--uuid", shouldAuthenticate, uuid);
            addArgument(arguments.getLiteralArguments(), "--accessToken", shouldAuthenticate, accessToken);

            if (!shouldAuthenticate && !arguments.getLiteralArguments().contains("--demo")) {
                // If user doesn't want to authenticate or doesn't own the game, go into demo.
                arguments.getLiteralArguments().add("--demo");
            }
        }

        String mainClass = System.getenv("PG_MAIN_CLASS");
        if (mainClass == null) mainClass = isServer ? "net.minecraft.server.Main" : "net.minecraft.client.main.Main";

        Class<?> cl = Class.forName(mainClass);
        Method m = cl.getMethod("main", String[].class);
        try {
            m.invoke(null, (Object) arguments.getLiteralArguments().toArray(new String[0]));
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    private static void addArgument(List<String> list, String arg, Supplier<String> value) {
        addArgument(list, arg, false, value);
    }

    private static void addArgument(List<String> list, String arg, boolean override, String value) {
        addArgument(list, arg, override, () -> value);
    }

    private static void addArgument(List<String> list, String arg, boolean override, Supplier<String> value) {
        if (override || !list.contains(arg)) {
            if (override) {
                int index = list.indexOf(arg);
                while (index != -1) {
                    list.remove(index);
                    list.remove(index);
                    index = list.indexOf(arg);
                }
            }
            list.add(arg);
            list.add(value.get());
        }
    }
}
