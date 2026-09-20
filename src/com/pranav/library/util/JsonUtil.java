package com.pranav.library.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal hand-written JSON reader/writer for this project's flat request
 * and response bodies. Deliberately not a general-purpose parser (no
 * external library like Gson/Jackson) — this project's whole point is to
 * show what a framework/library would otherwise be doing for you.
 * Supports flat objects (string/number/boolean values) and arrays of
 * flat objects, which is all the API here ever needs.
 */
public final class JsonUtil {

    private JsonUtil() { }

    // ---------- Writing ----------

    public static String toJson(Map<String, Object> obj) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : obj.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append(quote(entry.getKey())).append(":").append(valueToJson(entry.getValue()));
        }
        sb.append("}");
        return sb.toString();
    }

    public static String toJsonArray(List<Map<String, Object>> list) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Map<String, Object> obj : list) {
            if (!first) sb.append(",");
            first = false;
            sb.append(toJson(obj));
        }
        sb.append("]");
        return sb.toString();
    }

    private static String valueToJson(Object value) {
        if (value == null) return "null";
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        return quote(value.toString());
    }

    private static String quote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                default: sb.append(c);
            }
        }
        sb.append("\"");
        return sb.toString();
    }

    // ---------- Reading ----------

    /**
     * Parses a flat JSON object into a String-keyed, String-valued map.
     * Good enough for this API's request bodies (signup, login, add-book, etc.)
     * which never nest objects or arrays inside the request.
     */
    public static Map<String, String> parse(String json) {
        Map<String, String> result = new LinkedHashMap<>();
        if (json == null) return result;
        String trimmed = json.trim();
        if (trimmed.isEmpty() || trimmed.equals("{}")) return result;
        if (trimmed.startsWith("{")) trimmed = trimmed.substring(1);
        if (trimmed.endsWith("}")) trimmed = trimmed.substring(0, trimmed.length() - 1);

        int i = 0;
        int len = trimmed.length();
        while (i < len) {
            i = skipWhitespace(trimmed, i);
            if (i >= len) break;
            if (trimmed.charAt(i) == ',') { i++; continue; }

            // key
            int[] keyEnd = new int[1];
            String key = readQuotedString(trimmed, i, keyEnd);
            i = keyEnd[0];
            i = skipWhitespace(trimmed, i);
            if (i < len && trimmed.charAt(i) == ':') i++;
            i = skipWhitespace(trimmed, i);

            // value
            String value;
            if (i < len && trimmed.charAt(i) == '"') {
                int[] valEnd = new int[1];
                value = readQuotedString(trimmed, i, valEnd);
                i = valEnd[0];
            } else {
                int start = i;
                while (i < len && trimmed.charAt(i) != ',' && trimmed.charAt(i) != '}') i++;
                value = trimmed.substring(start, i).trim();
            }
            result.put(key, value);
            i = skipWhitespace(trimmed, i);
        }
        return result;
    }

    private static int skipWhitespace(String s, int i) {
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        return i;
    }

    private static String readQuotedString(String s, int start, int[] endOut) {
        int i = start;
        if (i < s.length() && s.charAt(i) == '"') i++;
        StringBuilder sb = new StringBuilder();
        while (i < s.length() && s.charAt(i) != '"') {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(i + 1);
                switch (next) {
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    default: sb.append(next);
                }
                i += 2;
            } else {
                sb.append(c);
                i++;
            }
        }
        i++; // skip closing quote
        endOut[0] = i;
        return sb.toString();
    }
}
