package dev.cbyrne.pufferfishmodloader;

import com.mojang.authlib.Agent;
import com.mojang.authlib.UserAuthentication;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Start {
    // Stuff stored by UUID/username in worlds resetting on each reboot is annoying - let's fix that.
    private static final UUID NOT_AUTHENTICATED_UUID = UUID.fromString("abcdef01-2345-6789-abcd-ef0123456789");
    private static final String NOT_AUTHENTICATED_USERNAME = "Player002";

    public static void main(String... args) throws Throwable {
        String mainClassName = System.getenv("MAIN_CLASS");
        File assetDirectory = new File(System.getenv("ASSET_DIRECTORY")).getAbsoluteFile();
        String assetIndex = System.getenv("ASSET_INDEX");
        File runDirectory = new File(System.getenv("RUN_DIRECTORY")).getAbsoluteFile();
        NextArgumentType nextArg = NextArgumentType.ARGUMENT_NAME;
        String passedUsername = null;
        String passedPassword = null;
        List<String> targetArgs = new ArrayList<>();
        boolean hasGameDir = false;
        boolean hasAssetsDir = false;
        boolean hasAssetIndex = false;
        boolean hasUuid = false;
        boolean hasAccessToken = false;
        boolean hasVersion = false;
        for (String arg : args) {
            switch (nextArg) {
                case ARGUMENT_NAME:
                    if (arg.equalsIgnoreCase("--username")) {
                        nextArg = NextArgumentType.USERNAME;
                    } else if (arg.equalsIgnoreCase("--password")) {
                        nextArg = NextArgumentType.PASSWORD;
                    } else {
                        if (arg.equalsIgnoreCase("--gameDir")) {
                            hasGameDir = true;
                        } else if (arg.equalsIgnoreCase("--assetsDir")) {
                            hasAssetsDir = true;
                        } else if (arg.equalsIgnoreCase("--assetIndex")) {
                            hasAssetIndex = true;
                        } else if (arg.equalsIgnoreCase("-uuid")) {
                            hasUuid = true;
                        } else if (arg.equalsIgnoreCase("--accessToken")) {
                            hasAccessToken = true;
                        } else if (arg.equalsIgnoreCase("--version")) {
                            hasVersion = true;
                        }
                        targetArgs.add(arg);
                    }
                    break;
                case USERNAME:
                    passedUsername = arg;
                    nextArg = NextArgumentType.ARGUMENT_NAME;
                    break;
                case PASSWORD:
                    passedPassword = arg;
                    nextArg = NextArgumentType.ARGUMENT_NAME;
                    break;
            }
        }
        String targetUsername = NOT_AUTHENTICATED_USERNAME;
        UUID targetUuid = NOT_AUTHENTICATED_UUID;
        String accessToken = "PML";
        if (passedUsername != null && passedPassword != null) {

            YggdrasilAuthenticationService authService = new YggdrasilAuthenticationService(Proxy.NO_PROXY, UUID.randomUUID().toString());
            UserAuthentication auth = authService.createUserAuthentication(Agent.MINECRAFT);
            auth.setUsername(passedUsername);
            auth.setPassword(passedPassword);
            auth.logIn();
            targetUsername = auth.getSelectedProfile().getName();
            targetUuid = auth.getSelectedProfile().getId();
            accessToken = auth.getAuthenticatedToken();

        } else if (passedUsername != null) {
            targetUsername = passedUsername;
        } else if (passedPassword != null) {
            targetArgs.add("--password");
            targetArgs.add(passedPassword);
        }

        if (!hasUuid) {
            targetArgs.add("--uuid");
            targetArgs.add(targetUuid.toString());
        }
        if (!hasAccessToken) {
            targetArgs.add("--accessToken");
            targetArgs.add(accessToken);
        }
        targetArgs.add("--username");
        targetArgs.add(targetUsername);

        if (!hasGameDir) {
            targetArgs.add("--gameDir");
            targetArgs.add(runDirectory.toString());
        }
        if (!hasAssetsDir) {
            targetArgs.add("--assetsDir");
            targetArgs.add(assetDirectory.toString());
        }
        if (!hasAssetIndex) {
            targetArgs.add("--assetIndex");
            targetArgs.add(assetIndex);
        }
        if (!hasVersion) {
            targetArgs.add("--version");
            targetArgs.add("PufferfishGradle");
        }

        Class<?> mainClass = Class.forName(mainClassName);

        Method method = mainClass.getMethod("main", String[].class);
        try {
            method.invoke(null, (Object) targetArgs.toArray(new String[0]));
        } catch (InvocationTargetException ex) {
            throw ex.getTargetException();
        }
    }

    public enum NextArgumentType {
        ARGUMENT_NAME,
        USERNAME,
        PASSWORD
    }

}
