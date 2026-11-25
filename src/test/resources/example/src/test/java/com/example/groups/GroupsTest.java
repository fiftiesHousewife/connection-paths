package com.example.groups;

import com.example.db.Database;
import org.junit.jupiter.api.Test;

class GroupsTest {
    @Test
    void listsMembers() {
        Groups.members("admins");
    }

    // Reaches the database, and calls into groups whose remote source only another test class creates.
    @Test
    void savesTheMembers() {
        new Database().openQuietly();
        Groups.members("admins");
    }
}
