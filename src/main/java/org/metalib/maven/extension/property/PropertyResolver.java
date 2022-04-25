package org.metalib.maven.extension.property;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class PropertyResolver {

    public Map<String,String> resolve(final Map<String,String> properties) {
        final var stack = new LinkedHashSet<String>();
        final var result = new HashMap<String,String>();
        properties.forEach((k,v) -> resolve(result, properties, k, v, stack));
        return result;
    }

    protected void resolve(final Map<String,String> target, final Map<String,String> properties, final String key, final String value, Set<String> stack) {
        if (target.containsKey(key)) {
            return;
        }
        if (stack.contains(key)) {
            throw new PropertyCircularReferenceException(stack);
        }
        final var resolved = new StringBuilder();
        final var len = value.length();
        var i = 0;
        do {
            final var j = value.indexOf("${", i);
            if (-1==j) {
                resolved.append(value.substring(i));
                break;
            }
            final var k = value.indexOf("}", j+1);
            if (-1==k) {
                resolved.append(value.substring(i));
                break;
            }
            final var name = value.substring(j+2, k);
            if (name.isBlank()) {
                resolved.append(value.substring(j, k));
            } else {
                final var val0 = target.get(name);
                if (null == val0) {
                    final var propertyValue = properties.get(name);
                    if (null == propertyValue) {
                        resolved.append(value.substring(j, k+1));
                    } else {
                        stack.add(key);
                        resolve(target, properties, name, propertyValue, stack);
                        stack.remove(key);
                        resolved.append(target.get(name));
                    }
                } else {
                    resolved.append(value.substring(i,j)).append(val0);
                }
            }
            i = k+1;
            if (i >= len) {
                break;
            }
        } while (i < len);
        target.put(key, resolved.toString());
    }

    public static class PropertyCircularReferenceException extends RuntimeException {
        PropertyCircularReferenceException(Set<String> stack) {
            super(message(stack));
        }
    }
    static String message(final Set<String> stack) {
        final var prefix = "Properties formed circular reference:";
        final var result = new StringBuilder(prefix);
        for (final var s : stack) {
            result.append(" -> <").append(s).append(">");
        }
        final var size = prefix.length();
        return result.replace(size, size+4, " ").toString();
    }
}
