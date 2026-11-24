package com.example.groups;

import org.junit.jupiter.api.Test;

class GroupsTest {
    @Test
    void listsMembers() {
        Groups.members("admins");
    }
}
