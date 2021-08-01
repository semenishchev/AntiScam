package me.mrfunny.bots.antiscam;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class Listener extends ListenerAdapter {

    private final String[] blacklistedWords = {"сначал", "эпик", "стим", "нитро", "ненадеж", "ненадёж", "разда", "нитру", "скин", "успел", "everyone"};

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        AntiScam.jda.getPresence().setPresence(Activity.watching(AntiScam.jda.getGuilds().size() + " servers"), true);
    }

    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
        String message = event.getMessage().getContentRaw();
        if(message.startsWith("!servers") && event.getAuthor().getId().equals("396713900017713172")){
            for(Guild guild : AntiScam.jda.getGuilds()){
                event.getChannel().sendMessage(guild.getName()).queue();
            }
            return;
        }
        int vl = 0;
        for(String word : blacklistedWords){
            if(message.contains(word)){
                vl++;
            }
        }
        for(String word : message.split(" ")){
            if(!word.startsWith("http")) continue;
            if(word.contains("tradeOffer") && !word.startsWith("https://steamcommunity.com")){
                vl = 10;
            } else if(word.contains("nitro") && !word.startsWith("https://discord.gift")){
                vl = 10;
            }
        }
        if(vl > 2){
            event.getMessage().delete().queue();
            for(TextChannel channel : event.getGuild().getCategoriesByName("scammers", true).get(0).getTextChannels()){
                channel.sendMessage("@everyone").queue();
                channel.sendMessageEmbeds(new EmbedBuilder()
                    .setTitle("User "
                        + nullSafe(event.getMember().getEffectiveName())
                        + "#"
                        + event.getAuthor().getDiscriminator()
                        + " (ID: "
                        + event.getAuthor().getId() + ")"
                        ).addField("Message", message, false)
                        .build()).queue();
            }

        }
    }

    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {
        ArrayList<Permission> permissions = new ArrayList<>();
        permissions.add(Permission.VIEW_CHANNEL);
        event.getGuild().createCategory("scammers").queue(category -> {
            category.createTextChannel("logs")
                .addPermissionOverride(event.getGuild().getPublicRole(), new ArrayList<>(), permissions)
                .addMemberPermissionOverride(AntiScam.jda.getSelfUser().getIdLong(), permissions, new ArrayList<>())
                .queue(channel -> channel.sendMessage("**Do not rename these channels and categories!**").queue());
        });
        AntiScam.jda.getPresence().setPresence(Activity.watching(AntiScam.jda.getGuilds().size() + " servers"), true);
    }

    @Override
    public void onGuildLeave(@NotNull GuildLeaveEvent event) {
        AntiScam.jda.getPresence().setPresence(Activity.watching(AntiScam.jda.getGuilds().size() + " servers"), true);
    }

    @Override
    public void onGuildBan(@NotNull GuildBanEvent event) {
        AntiScam.jda.getPresence().setPresence(Activity.watching(AntiScam.jda.getGuilds().size() + " servers"), true);
    }

    @SuppressWarnings("all")
    public String nullSafe(@Nullable String string){
        if(string == null){
            return "null";
        } else {
            return string;
        }
    }
}
