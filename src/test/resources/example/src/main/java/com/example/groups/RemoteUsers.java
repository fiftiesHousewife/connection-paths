package com.example.groups;

public class RemoteUsers {
    private final GroupSource groups = new RemoteGroups();

    public String groupsOf(String user) {
        return groups.members(user);
    }
}
