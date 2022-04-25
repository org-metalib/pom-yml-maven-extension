package org.metalib.maven.extension.property;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class BeanPropertyMap {

    static final String DOT = ".";

    static Set<Class<?>> primitiveTypes = Set.of(Number.class, Character.class, CharSequence.class, Date.class,
            TemporalAccessor.class, UUID.class, File.class, Class.class);

    public static Map<String,String> resolve(String prefix, Object instance) {
        final var result = new LinkedHashMap<String,String>();
        final var beans = new HashSet<>();
        beans.add(instance);
        resolve(result, prefix, instance, beans);
        return result;
    }

    static void resolve(Map<String,String> target, String prefix, Object instance, Set beans) {
        if (null == instance || 0 == (instance.getClass().getModifiers() & Modifier.PUBLIC)) {
            return;
        }
        for (final var method : instance.getClass().getMethods()) {
            if (0 < method.getParameterCount() || 0 == (Modifier.PUBLIC & method.getModifiers())) {
                continue;
            }
            final var methodName = method.getName();
            final var methodNameLen = methodName.length();
            if (methodNameLen <= 3 || !method.getName().startsWith("get")) {
                continue;
            }
            char propertyFirstChar = methodName.charAt(3);
            if (Character.isLowerCase(propertyFirstChar)) {
                continue;
            }
            final var type = method.getReturnType();
            if (Iterable.class.isAssignableFrom(type) ||
                    type == Class.class ||
                    Map.class.isAssignableFrom(type)) {
                continue;
            }
            final var propertyName = prefix + DOT + Character.toLowerCase(propertyFirstChar) + methodName.substring(4);
            try {
                if (isBeanType(type)) {
                    final var value = method.invoke(instance);
                    if (null == value || beans.contains(value)) {
                        continue;
                    }
                    beans.add(value);
                    resolve(target, propertyName, value, beans);
                } else {
                        final var value = method.invoke(instance);
                        if (null == value) {
                            continue;
                        }
                        target.put(propertyName, value.toString());
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    static boolean isBeanType(Class type) {
        if (type.isPrimitive()) {
            return false;
        }
        for (final var primitive : primitiveTypes) {
            if (primitive.isAssignableFrom(type)) {
                return false;
            }
        }
        return true;
    }

    private BeanPropertyMap() {
        // Empty
    }
}
