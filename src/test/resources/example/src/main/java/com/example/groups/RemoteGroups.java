package com.example.groups;

import java.io.IOException;
import java.net.URL;

public class RemoteGroups implements GroupSource {
    @Override
    public String members(String group) {
        try {
            return new URL("https://groups.internal/" + group).openStream().toString();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
