package com.example.plugins;

import com.example.config.Settings;

public final class Exporters {
    private Exporters() {
    }

    public static Exporter configured() throws ReflectiveOperationException {
        final String name = Settings.get("exporter.class");
        return name == null ? new FileExporter()
            : (Exporter) Class.forName(name).getDeclaredConstructor().newInstance();
    }
}
