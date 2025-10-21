package me.devnatan.inventoryframework.runtime.thirdparty;

import org.bukkit.event.inventory.InventoryType;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

/**
 * Minimal stub of InventoryUpdate used exclusively for unit tests. The real
 * implementation ships with Inventory Framework, however the test environment
 * does not provide Minecraft's NMS classes. This stub exposes enough reflective
 * utilities so {@link com.raindropcentral.rplatform.view.anvil.CustomAnvilInputNMS}
 * can be exercised with fake containers.
 */
public final class InventoryUpdate {

    public static final Class<?> CONTAINER = InventoryContainer.class;

    public static final Containers Containers = new Containers();

    private static MethodHandle defaultPacketHandle() {
        try {
            return MethodHandles.lookup()
                                 .findStatic(PacketStub.class,
                                     "openWindow",
                                     MethodType.methodType(Object.class, int.class, Object.class, Object.class));
        } catch (NoSuchMethodException | IllegalAccessException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static MethodHandle defaultBukkitViewHandle() {
        try {
            return MethodHandles.lookup()
                                 .findVirtual(InventoryContainer.class,
                                     "getBukkitView",
                                     MethodType.methodType(Object.class));
        } catch (NoSuchMethodException | IllegalAccessException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public static MethodHandle packetPlayOutOpenWindow = defaultPacketHandle();

    public static MethodHandle getBukkitView = defaultBukkitViewHandle();

    private static boolean containersAvailable = true;

    private InventoryUpdate() {}

    public static void reset() {
        packetPlayOutOpenWindow = defaultPacketHandle();
        getBukkitView = defaultBukkitViewHandle();
        containersAvailable = true;
    }

    public static void setPacketHandle(MethodHandle handle) {
        packetPlayOutOpenWindow = Objects.requireNonNull(handle, "handle");
    }

    public static void setBukkitViewHandle(MethodHandle handle) {
        getBukkitView = Objects.requireNonNull(handle, "handle");
    }

    public static void setUseContainers(boolean useContainers) {
        containersAvailable = useContainers;
    }

    public static boolean useContainers() {
        return containersAvailable;
    }

    public static Object getContainerOrName(Object containerKey, InventoryType type) {
        return containersAvailable ? containerKey : type;
    }

    public static Object createTitleComponent(Object title) {
        return title;
    }

    public static MethodHandle getConstructor(Class<?> owner, Class<?>... parameterTypes) {
        try {
            return lookup(owner).findConstructor(owner, MethodType.methodType(void.class, parameterTypes));
        } catch (NoSuchMethodException | IllegalAccessException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public static MethodHandle setFieldHandle(Class<?> owner, Class<?> fieldType, String... candidates) {
        return setField(owner, fieldType, candidates);
    }

    public static MethodHandle setField(Class<?> owner, Class<?> fieldType, String... candidates) {
        final Field field = findField(owner, candidates);
        try {
            return lookup(owner).unreflectSetter(field);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public static MethodHandle getField(Class<?> owner, Class<?> fieldType, String... candidates) {
        final Field field = findField(owner, candidates);
        try {
            return lookup(owner).unreflectGetter(field);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public static MethodHandle getMethod(Class<?> owner, String name, MethodType type) {
        return findMethod(owner, type, new String[] {name});
    }

    public static MethodHandle getMethod(Class<?> owner, String name, MethodType type, boolean ignored, String... fallbacks) {
        final String[] candidates = new String[1 + fallbacks.length];
        candidates[0] = name;
        System.arraycopy(fallbacks, 0, candidates, 1, fallbacks.length);
        return findMethod(owner, type, candidates);
    }

    private static MethodHandle findMethod(Class<?> owner, MethodType type, String[] candidates) {
        for (final String candidate : candidates) {
            if (candidate == null || candidate.isEmpty()) {
                continue;
            }
            try {
                final Method method = owner.getDeclaredMethod(candidate, type.parameterArray());
                method.setAccessible(true);
                return lookup(owner).unreflect(method);
            } catch (NoSuchMethodException ignored) {
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException(exception);
            }
        }
        throw new IllegalArgumentException(
            "No matching method found on " + owner.getName() + " for names " + Arrays.toString(candidates));
    }

    private static Field findField(Class<?> owner, String... candidates) {
        for (final String candidate : candidates) {
            if (candidate == null || candidate.isEmpty()) {
                continue;
            }
            try {
                final Field field = owner.getDeclaredField(candidate);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new IllegalArgumentException(
            "No matching field found on " + owner.getName() + " for names " + Arrays.toString(candidates));
    }

    private static MethodHandles.Lookup lookup(Class<?> owner) {
        try {
            return MethodHandles.privateLookupIn(owner, MethodHandles.lookup());
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public static final class Containers {

        public final Object ANVIL = new Object();
    }

    public static class InventoryContainer {

        private boolean checkReachable = true;
        private int windowId = -1;
        private Object bukkitView;
        private Object lastListener;

        public boolean isCheckReachable() {
            return checkReachable;
        }

        public void setCheckReachable(boolean checkReachable) {
            this.checkReachable = checkReachable;
        }

        public int getWindowId() {
            return windowId;
        }

        public void setWindowId(int windowId) {
            this.windowId = windowId;
        }

        public Object getBukkitView() {
            return bukkitView;
        }

        public void setBukkitView(Object bukkitView) {
            this.bukkitView = bukkitView;
        }

        public Object getLastListener() {
            return lastListener;
        }

        public void a(Object listener) {
            lastListener = listener;
        }
    }

    private static final class PacketStub {

        private static Object openWindow(int windowId, Object container, Object title) {
            return new Object();
        }
    }
}
