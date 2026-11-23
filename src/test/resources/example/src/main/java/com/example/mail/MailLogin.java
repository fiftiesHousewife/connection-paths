package com.example.mail;

public class MailLogin {
    public boolean check(String user, String password) {
        new Pop3Store().connect("pop.internal", user, password);
        return true;
    }
}
