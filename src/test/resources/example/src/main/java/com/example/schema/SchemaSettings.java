package com.example.schema;

final class SchemaSettings {
    private SchemaSettings() {
    }

    static String get(String key) {
        return System.getenv("SCHEMA_SETTINGS_" + key.hashCode());
    }
}
