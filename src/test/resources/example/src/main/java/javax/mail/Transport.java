package javax.mail;

/** Stands in for the JavaMail library's class, which the example repo has no classpath for. */
public abstract class Transport extends Service {
    public abstract void sendMessage(String message, String[] addresses);
}
