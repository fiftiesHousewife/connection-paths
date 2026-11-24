package com.example.groups;

public class LocalGroups implements GroupSource {
    @Override
    public String members(String group) {
        return group;
    }
}
