package web;

import java.util.*;

/**
 * Lightweight, zero-dependency JSON serializer and parser in pure Java.
 */
public final class JsonUtils {

    private JsonUtils() {}

    public static String toJson(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof String) {
            return "\"" + escapeJson((String) obj) + "\"";
        }
        if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        }
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(escapeJson(String.valueOf(entry.getKey()))).append("\":");
                sb.append(toJson(entry.getValue()));
                first = false;
            }
            sb.append("}");
            return sb.toString();
        }
        if (obj instanceof Collection) {
            Collection<?> col = (Collection<?>) obj;
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : col) {
                if (!first) sb.append(",");
                sb.append(toJson(item));
                first = false;
            }
            sb.append("]");
            return sb.toString();
        }
        if (obj.getClass().isArray()) {
            int len = java.lang.reflect.Array.getLength(obj);
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < len; i++) {
                if (i > 0) sb.append(",");
                sb.append(toJson(java.lang.reflect.Array.get(obj, i)));
            }
            sb.append("]");
            return sb.toString();
        }
        if (obj instanceof Enum) {
            return "\"" + ((Enum<?>) obj).name() + "\"";
        }
        Class<?> clazz = obj.getClass();
        if (!clazz.getName().startsWith("java.") && !clazz.getName().startsWith("javax.")) {
            Map<String, Object> pojoMap = new LinkedHashMap<>();
            for (java.lang.reflect.Field f : clazz.getFields()) {
                if (!java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                    try {
                        pojoMap.put(f.getName(), f.get(obj));
                    } catch (Exception ignored) {}
                }
            }
            for (java.lang.reflect.Method m : clazz.getMethods()) {
                if (m.getParameterCount() == 0 && !m.getName().equals("getClass") &&
                    !m.getName().equals("hashCode") && !m.getName().equals("toString") &&
                    !java.lang.reflect.Modifier.isStatic(m.getModifiers())) {
                    String name = m.getName();
                    String propName = null;
                    if (name.startsWith("get") && name.length() > 3) {
                        propName = Character.toLowerCase(name.charAt(3)) + name.substring(4);
                    } else if (name.startsWith("is") && name.length() > 2) {
                        propName = Character.toLowerCase(name.charAt(2)) + name.substring(3);
                    }
                    if (propName != null) {
                        try {
                            pojoMap.put(propName, m.invoke(obj));
                        } catch (Exception ignored) {}
                    }
                }
            }
            return toJson(pojoMap);
        }
        return "\"" + escapeJson(obj.toString()) + "\"";
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    /**
     * Parses a simple JSON object string into a Map of String -> Object.
     */
    public static Map<String, Object> parseJsonObject(String json) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (json == null) return map;
        String trimmed = json.trim();
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return map;

        String inner = trimmed.substring(1, trimmed.length() - 1).trim();
        if (inner.isEmpty()) return map;

        int idx = 0;
        int len = inner.length();

        while (idx < len) {
            // Find key
            while (idx < len && (Character.isWhitespace(inner.charAt(idx)) || inner.charAt(idx) == ',')) idx++;
            if (idx >= len) break;

            if (inner.charAt(idx) != '"') break;
            idx++; // skip opening quote
            int keyStart = idx;
            while (idx < len && inner.charAt(idx) != '"') {
                if (inner.charAt(idx) == '\\') idx++;
                idx++;
            }
            String key = inner.substring(keyStart, idx);
            idx++; // skip closing quote

            // Skip to colon
            while (idx < len && (Character.isWhitespace(inner.charAt(idx)) || inner.charAt(idx) == ':')) idx++;

            // Extract value
            while (idx < len && Character.isWhitespace(inner.charAt(idx))) idx++;
            if (idx >= len) break;

            char firstValChar = inner.charAt(idx);
            if (firstValChar == '"') {
                idx++;
                StringBuilder val = new StringBuilder();
                while (idx < len && inner.charAt(idx) != '"') {
                    if (inner.charAt(idx) == '\\' && idx + 1 < len) {
                        idx++;
                        char esc = inner.charAt(idx);
                        if (esc == 'n') val.append('\n');
                        else if (esc == 'r') val.append('\r');
                        else if (esc == 't') val.append('\t');
                        else val.append(esc);
                    } else {
                        val.append(inner.charAt(idx));
                    }
                    idx++;
                }
                idx++; // skip closing quote
                map.put(key, val.toString());
            } else if (firstValChar == '{') {
                int braceCount = 1;
                int startObj = idx;
                idx++;
                while (idx < len && braceCount > 0) {
                    if (inner.charAt(idx) == '{') braceCount++;
                    else if (inner.charAt(idx) == '}') braceCount--;
                    idx++;
                }
                String sub = inner.substring(startObj, idx);
                map.put(key, parseJsonObject(sub));
            } else if (firstValChar == '[') {
                int bracketCount = 1;
                int startArr = idx;
                idx++;
                while (idx < len && bracketCount > 0) {
                    if (inner.charAt(idx) == '[') bracketCount++;
                    else if (inner.charAt(idx) == ']') bracketCount--;
                    idx++;
                }
                String sub = inner.substring(startArr, idx);
                map.put(key, parseJsonArray(sub));
            } else {
                // Number or boolean
                int startLit = idx;
                while (idx < len && inner.charAt(idx) != ',' && inner.charAt(idx) != '}') idx++;
                String lit = inner.substring(startLit, idx).trim();
                if ("true".equalsIgnoreCase(lit)) map.put(key, Boolean.TRUE);
                else if ("false".equalsIgnoreCase(lit)) map.put(key, Boolean.FALSE);
                else if ("null".equalsIgnoreCase(lit)) map.put(key, null);
                else {
                    try {
                        if (lit.contains(".")) {
                            map.put(key, Double.parseDouble(lit));
                        } else {
                            map.put(key, Long.parseLong(lit));
                        }
                    } catch (NumberFormatException e) {
                        map.put(key, lit);
                    }
                }
            }

            // Skip to comma
            while (idx < len && (Character.isWhitespace(inner.charAt(idx)) || inner.charAt(idx) == ',')) idx++;
        }

        return map;
    }

    public static List<Object> parseJsonArray(String json) {
        List<Object> list = new ArrayList<>();
        if (json == null) return list;
        String trimmed = json.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) return list;

        String inner = trimmed.substring(1, trimmed.length() - 1).trim();
        if (inner.isEmpty()) return list;

        int idx = 0;
        int len = inner.length();

        while (idx < len) {
            while (idx < len && (Character.isWhitespace(inner.charAt(idx)) || inner.charAt(idx) == ',')) idx++;
            if (idx >= len) break;

            char c = inner.charAt(idx);
            if (c == '"') {
                idx++;
                StringBuilder val = new StringBuilder();
                while (idx < len && inner.charAt(idx) != '"') {
                    if (inner.charAt(idx) == '\\' && idx + 1 < len) {
                        idx++;
                        val.append(inner.charAt(idx));
                    } else {
                        val.append(inner.charAt(idx));
                    }
                    idx++;
                }
                idx++;
                list.add(val.toString());
            } else if (c == '{') {
                int braceCount = 1;
                int start = idx;
                idx++;
                while (idx < len && braceCount > 0) {
                    if (inner.charAt(idx) == '{') braceCount++;
                    else if (inner.charAt(idx) == '}') braceCount--;
                    idx++;
                }
                list.add(parseJsonObject(inner.substring(start, idx)));
            } else {
                int start = idx;
                while (idx < len && inner.charAt(idx) != ',' && inner.charAt(idx) != ']') idx++;
                String lit = inner.substring(start, idx).trim();
                list.add(lit);
            }
        }
        return list;
    }
}
