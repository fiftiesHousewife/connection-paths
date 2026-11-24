package com.example.groups;

import org.junit.jupiter.api.Test;

class GroupsFromTheDirectoryTest {
    @Test
    void listsRemoteMembers() {
        Groups.use(new RemoteGroups());
        Groups.members("admins");
    }
}
