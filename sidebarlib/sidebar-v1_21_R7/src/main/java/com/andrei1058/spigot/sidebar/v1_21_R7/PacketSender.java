package com.andrei1058.spigot.sidebar.v1_21_R7;

import net.minecraft.network.protocol.Packet;
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Reflection-based packet sender for MC 1.21.11.
 * The obfuscated PlayerConnection.b(Packet) method was renamed in this version,
 * so we discover the connection field and send method at runtime.
 */
class PacketSender {

    private static volatile Field connectionField;
    private static volatile Method sendMethod;

    static void send(Player player, Packet<?> packet) {
        try {
            Object handle = ((CraftPlayer) player).getHandle();
            ensureReflection(handle);
            if (connectionField != null && sendMethod != null) {
                Object conn = connectionField.get(handle);
                sendMethod.invoke(conn, packet);
            }
        } catch (Exception ignored) {
        }
    }

    private static synchronized void ensureReflection(Object playerHandle) throws Exception {
        if (connectionField != null) return;

        Class<?> cls = playerHandle.getClass();
        outer:
        while (cls != null && cls != Object.class) {
            for (Field f : cls.getDeclaredFields()) {
                String typeName = f.getType().getSimpleName();
                if (typeName.contains("Connection") || typeName.contains("PacketListener")
                        || typeName.contains("NetworkHandler")) {
                    f.setAccessible(true);
                    Object conn = f.get(playerHandle);
                    if (conn == null) continue;
                    for (Method m : conn.getClass().getMethods()) {
                        if (m.getParameterCount() != 1) continue;
                        Class<?> paramType = m.getParameterTypes()[0];
                        if (paramType.isAssignableFrom(Packet.class) || Packet.class.isAssignableFrom(paramType)) {
                            connectionField = f;
                            sendMethod = m;
                            break outer;
                        }
                    }
                }
            }
            cls = cls.getSuperclass();
        }
    }
}
