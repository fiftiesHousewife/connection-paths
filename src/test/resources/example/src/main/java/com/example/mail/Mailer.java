package com.example.mail;

import org.unknown.mail.Post;

public class Mailer {
    public void send(String to) {
        Post.deliver(to);
    }
}
