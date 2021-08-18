package me.mrfunny.bots.antiscam;

import me.mrfunny.bots.antiscam.secret.SuperSecretClass;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

import javax.security.auth.login.LoginException;

public class AntiScam {
    public static JDA jda;
    public static String version = "3.0";

    public static void main(String[] args) throws LoginException {
        jda = JDABuilder.createDefault(SuperSecretClass.token).addEventListeners(new Listener(SuperSecretClass.webhookUrl)).build();
    }
}
