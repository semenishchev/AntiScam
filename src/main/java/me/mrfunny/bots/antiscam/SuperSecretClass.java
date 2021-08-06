package me.mrfunny.bots.antiscam;

import com.mongodb.ConnectionString;

public class SuperSecretClass {
    public static final String token = System.getenv().get("token");
    public static final String debugToken = System.getenv().get("debugToken");
    public static final ConnectionString connectionString = new ConnectionString(System.getenv().get("connectionString"));
    public static final String webhookUrl = System.getenv().get("webhookUrl");
}
