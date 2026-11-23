package com.example.util;

import com.example.db.Database;
import java.util.HashMap;

public class SavingMap extends HashMap<String, String> {
    @Override
    public String put(String key, String value) {
        new Database().openQuietly();
        return super.put(key, value);
    }
}
