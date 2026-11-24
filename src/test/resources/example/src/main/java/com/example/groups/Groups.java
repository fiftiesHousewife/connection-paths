package com.example.groups;

public final class Groups {
    private static GroupSource source = new LocalGroups();

    private Groups() {
    }

    public static void use(GroupSource groups) {
        source = groups;
    }

    public static String members(String group) {
        return source.members(group);
    }
}
