package com.example.util;

import java.util.Map;

public class Names {
    public static String join(Map<String, String> names) {
        names.put("joined", "yes");
        return String.join(",", names.values());
    }
}
