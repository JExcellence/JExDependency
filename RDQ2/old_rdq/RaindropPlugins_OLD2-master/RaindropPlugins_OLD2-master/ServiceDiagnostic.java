import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Diagnostic utility to debug service registration issues.
 * 
 * Add this to your plugin temporarily to diagnose ClassLoader issues.
 * 
 * Usage:
 * ServiceDiagnostic.diagnose(this, "com.raindropcentral.rcore.api.RCoreAdapter");
 */
public class ServiceDiagnostic {
    
    public static void diagnose(JavaPlugin plugin, String serviceClassName) {
        plugin.getLogger().info("╔════════════════════════════════════════════════════════════════╗");
        plugin.getLogger().info("║           SERVICE REGISTRATION DIAGNOSTIC                      ║");
        plugin.getLogger().info("╠════════════════════════════════════════════════════════════════╣");
        plugin.getLogger().info("║ Service: " + serviceClassName);
        plugin.getLogger().info("╚════════════════════════════════════════════════════════════════╝");
        
        // Step 1: Check if class exists
        plugin.getLogger().info("");
        plugin.getLogger().info("Step 1: Checking if class exists...");
        Class<?> serviceClass;
        try {
            serviceClass = Class.forName(serviceClassName);
            plugin.getLogger().info("  ✓ Class found: " + serviceClass.getName());
            plugin.getLogger().info("  ✓ ClassLoader: " + serviceClass.getClassLoader());
        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe("  ✗ Class NOT found!");
            plugin.getLogger().severe("  → Make sure the plugin providing this service is loaded");
            plugin.getLogger().severe("  → Check your plugin.yml for 'depend' or 'softdepend'");
            return;
        }
        
        // Step 2: Check plugin ClassLoader
        plugin.getLogger().info("");
        plugin.getLogger().info("Step 2: Checking ClassLoaders...");
        plugin.getLogger().info("  Your plugin ClassLoader: " + plugin.getClass().getClassLoader());
        plugin.getLogger().info("  Service class ClassLoader: " + serviceClass.getClassLoader());
        
        if (plugin.getClass().getClassLoader().equals(serviceClass.getClassLoader())) {
            plugin.getLogger().warning("  ⚠ WARNING: Same ClassLoader detected!");
            plugin.getLogger().warning("  → This means the service class is bundled in YOUR plugin");
            plugin.getLogger().warning("  → Change 'implementation' to 'compileOnly' in build.gradle.kts");
            plugin.getLogger().warning("  → Rebuild your plugin");
        } else {
            plugin.getLogger().info("  ✓ Different ClassLoaders (correct)");
        }
        
        // Step 3: Check if service is registered
        plugin.getLogger().info("");
        plugin.getLogger().info("Step 3: Checking service registration...");
        RegisteredServiceProvider<?> registration = 
            Bukkit.getServicesManager().getRegistration(serviceClass);
        
        if (registration == null) {
            plugin.getLogger().severe("  ✗ Service NOT registered!");
            plugin.getLogger().severe("  → The providing plugin may not have registered the service yet");
            plugin.getLogger().severe("  → Check load order in plugin.yml");
            plugin.getLogger().severe("  → Try accessing the service later (e.g., with a delay)");
        } else {
            plugin.getLogger().info("  ✓ Service is registered!");
            plugin.getLogger().info("  ✓ Provider class: " + registration.getProvider().getClass().getName());
            plugin.getLogger().info("  ✓ Provider plugin: " + registration.getPlugin().getName());
            plugin.getLogger().info("  ✓ Priority: " + registration.getPriority());
            
            // Check ClassLoader of provider
            Class<?> providerClass = registration.getProvider().getClass();
            plugin.getLogger().info("  ✓ Provider ClassLoader: " + providerClass.getClassLoader());
            
            if (!serviceClass.getClassLoader().equals(providerClass.getClassLoader())) {
                plugin.getLogger().severe("  ✗ CLASSLOADER MISMATCH!");
                plugin.getLogger().severe("  → Service class and provider have different ClassLoaders");
                plugin.getLogger().severe("  → This is why getRegistration() returns null");
                plugin.getLogger().severe("  → Fix: Use 'compileOnly' instead of 'implementation'");
            }
        }
        
        // Step 4: List all registered services
        plugin.getLogger().info("");
        plugin.getLogger().info("Step 4: All registered services:");
        int count = 0;
        for (Class<?> service : Bukkit.getServicesManager().getKnownServices()) {
            count++;
            plugin.getLogger().info("  " + count + ". " + service.getName());
            RegisteredServiceProvider<?> reg = Bukkit.getServicesManager().getRegistration(service);
            if (reg != null) {
                plugin.getLogger().info("     → Provider: " + reg.getPlugin().getName());
            }
        }
        
        // Step 5: Check providing plugin
        plugin.getLogger().info("");
        plugin.getLogger().info("Step 5: Checking providing plugin...");
        String providingPluginName = extractPluginName(serviceClassName);
        Plugin providingPlugin = Bukkit.getPluginManager().getPlugin(providingPluginName);
        
        if (providingPlugin == null) {
            plugin.getLogger().severe("  ✗ Plugin '" + providingPluginName + "' not found!");
            plugin.getLogger().severe("  → Make sure the plugin is in the plugins folder");
        } else {
            plugin.getLogger().info("  ✓ Plugin found: " + providingPlugin.getName());
            plugin.getLogger().info("  ✓ Version: " + providingPlugin.getDescription().getVersion());
            plugin.getLogger().info("  ✓ Enabled: " + providingPlugin.isEnabled());
            plugin.getLogger().info("  ✓ Main class: " + providingPlugin.getDescription().getMain());
        }
        
        // Summary
        plugin.getLogger().info("");
        plugin.getLogger().info("╔════════════════════════════════════════════════════════════════╗");
        plugin.getLogger().info("║                         SUMMARY                                ║");
        plugin.getLogger().info("╚════════════════════════════════════════════════════════════════╝");
        
        if (registration != null && 
            serviceClass.getClassLoader().equals(registration.getProvider().getClass().getClassLoader())) {
            plugin.getLogger().info("  ✓ Everything looks good!");
            plugin.getLogger().info("  → Service should be accessible");
        } else {
            plugin.getLogger().severe("  ✗ Issues detected!");
            plugin.getLogger().severe("  → See messages above for details");
            plugin.getLogger().severe("");
            plugin.getLogger().severe("  Common fixes:");
            plugin.getLogger().severe("  1. Change 'implementation' to 'compileOnly' in build.gradle.kts");
            plugin.getLogger().severe("  2. Add 'depend: [" + providingPluginName + "]' to plugin.yml");
            plugin.getLogger().severe("  3. Rebuild your plugin with './gradlew clean build'");
            plugin.getLogger().severe("  4. Verify the service class is NOT in your JAR");
        }
        
        plugin.getLogger().info("╚════════════════════════════════════════════════════════════════╝");
    }
    
    private static String extractPluginName(String className) {
        // Try to extract plugin name from package
        if (className.contains("rcore")) return "RCore";
        if (className.contains("currency")) return "JECurrency";
        if (className.contains("raindropquests") || className.contains(".rdq.")) return "RaindropQuests";
        
        // Default: try to extract from package
        String[] parts = className.split("\\.");
        if (parts.length > 2) {
            return capitalize(parts[2]);
        }
        
        return "Unknown";
    }
    
    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
