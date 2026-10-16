package net.kdt.pojavlaunch.minethor;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import net.kdt.pojavlaunch.JMinecraftVersionList;
import net.kdt.pojavlaunch.Tools;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * The only entry points the launcher needs for MineThor at game launch time.
 * Keeping this logic here (instead of in upstream's AsyncAssetManager / JREUtils / Tools)
 * keeps the number of edited upstream lines minimal, see docs/GIT_WORKFLOW.md.
 */
public final class MineThorLaunch {
    private static final String TAG = "MineThorLaunch";
    private static final String AGENT_COMPONENT = "minethor_agent";
    private static final String AGENT_JAR = "minethor_agent.jar";

    private MineThorLaunch() {
    }

    /**
     * Copies the agent jar from the APK assets into the private data directory when it is missing
     * or when its version file differs. Cheap enough to call on every launcher start.
     */
    public static void installAgent(Context context) {
        try {
            AssetManager assets = context.getAssets();
            String assetDir = "components/" + AGENT_COMPONENT;
            File targetDir = new File(Tools.DIR_DATA, AGENT_COMPONENT);
            File installedVersion = new File(targetDir, "version");

            String packagedVersion;
            try (InputStream versionStream = assets.open(assetDir + "/version")) {
                packagedVersion = Tools.read(versionStream);
            }
            if (installedVersion.isFile() && packagedVersion.equals(Tools.read(installedVersion.getAbsolutePath()))
                    && new File(targetDir, AGENT_JAR).isFile()) {
                return;
            }

            if (targetDir.isDirectory()) FileUtils.deleteDirectory(targetDir);
            if (!targetDir.mkdirs()) throw new IOException("Cannot create " + targetDir);
            String[] files = assets.list(assetDir);
            if (files == null) return;
            for (String file : files) {
                Tools.copyAssetFile(context, assetDir + "/" + file, targetDir.getAbsolutePath(), true);
            }
            Log.i(TAG, "Installed " + AGENT_COMPONENT + " " + packagedVersion);
        } catch (IOException | RuntimeException e) {
            // The game must still start without the companion.
            Log.w(TAG, "Cannot install " + AGENT_COMPONENT, e);
        }
    }

    /**
     * Adds the JVM arguments that attach the companion agent to the game: the agent itself, the
     * mapping descriptor for the launched version and the version binding for texture lookups.
     */
    public static void appendJvmArgs(List<String> jvmArgs, String versionId, JMinecraftVersionList.Version versionInfo) {
        File agentJar = new File(Tools.DIR_DATA, AGENT_COMPONENT + "/" + AGENT_JAR);
        if (agentJar.isFile()) {
            jvmArgs.add("-javaagent:" + agentJar.getAbsolutePath());
        } else {
            Log.w(TAG, "Agent jar is missing, companion will not receive game state");
        }

        String descriptor = MineThorMappingResolver.resolveDescriptorForLaunch(versionId, versionInfo);
        CompanionEndpoint.setMappingsAvailable(descriptor != null);
        if (descriptor != null) {
            jvmArgs.add("-Dminethor.mappingDescriptor=" + descriptor);
        }
        // One-time token and a free loopback port for this launch; the agent refuses to start without them.
        if (CompanionEndpoint.newSession()) {
            jvmArgs.add("-Dminethor.port=" + CompanionEndpoint.port());
            jvmArgs.add("-Dminethor.token=" + CompanionEndpoint.token());
        } else {
            Log.w(TAG, "No free loopback port, companion bridge disabled");
        }
        // LWJGL hash checks are not needed (and break) on the Android runtime.
        jvmArgs.add("-Dorg.lwjgl.util.NoChecks=true");

        MineThorMinecraftAssets.setActiveVersion(versionId);
    }
}
