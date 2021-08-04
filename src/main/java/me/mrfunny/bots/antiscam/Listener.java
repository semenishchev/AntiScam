package me.mrfunny.bots.antiscam;

import club.minnced.discord.webhook.WebhookClient;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.commons.lang3.ArrayUtils;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Listener extends ListenerAdapter {
    private final WebhookClient client;
    public Listener(String webhookUrl){
        client = WebhookClient.withUrl(webhookUrl);
    }

    private final String[] blacklistedWords = {"сначал", "эпик", "стим", "нитро", "ненадеж", "ненадёж", "разда", "нитру", "скин", "успел", "everyone"};
    private MongoCollection<Document> collection;

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        updatePresence();
        MongoClient client = MongoClients.create(SuperSecretClass.connectionString);
        collection = client.getDatabase("antiscam").getCollection("servers");
        for(Guild guild : AntiScam.jda.getGuilds()){
            if(collection.find(new Document("server_id", guild.getId())).first() == null){
                setupServer(guild);
            }
        }
    }

    private void setupServer(Guild guild){
        ArrayList<Permission> permissions = new ArrayList<>();
        permissions.add(Permission.VIEW_CHANNEL);
        try {
            Category category = guild.createCategory("AntiScam").addRolePermissionOverride(guild.getPublicRole().getIdLong(), new ArrayList<>(), permissions).complete();
            List<TextChannel> possibleUpdatesChannels = guild.getTextChannelsByName("antiscam-updates", true);
            List<TextChannel> possibleLogsChannels = guild.getTextChannelsByName("logs", true);
            TextChannel updatesChannel = (possibleUpdatesChannels.isEmpty() ? category.createTextChannel("antiscam-updates").addMemberPermissionOverride(AntiScam.jda.getSelfUser().getIdLong(), permissions, new ArrayList<>()).addRolePermissionOverride(guild.getPublicRole().getIdLong(), new ArrayList<>(), permissions).complete() : possibleUpdatesChannels.get(0));
            TextChannel logsChannel = (possibleLogsChannels.isEmpty() ? category.createTextChannel("logs").addMemberPermissionOverride(AntiScam.jda.getSelfUser().getIdLong(), permissions, new ArrayList<>()).addRolePermissionOverride(guild.getPublicRole().getIdLong(), new ArrayList<>(), permissions).complete() : possibleLogsChannels.get(0));
            logsChannel.sendMessage("Thanks for adding me! My current prefix is a!").queue(message -> sendHelp(logsChannel, "a!"));
            if(collection.find(new Document("server_id", guild.getId())).first() == null){
                collection.insertOne(new Document("server_id", guild.getId()).append("logs_channel_id", logsChannel.getId()).append("prefix", "a!").append("updates_channel_id", updatesChannel.getId()));
            }
        } catch (Exception exception){
            try {
                for(TextChannel channel: guild.getTextChannels()){
                    if(channel.canTalk()){
                        channel.sendMessage("Please give me " + "Manage Channels, Read Messages, Send Messages, Manage Messages and" +
                                " Include links permissions").queue();
                    }
                }
            } catch (InsufficientPermissionException exception1){
                guild.leave().queue();
            }

        }

    }

    public static Color hexToColor(String colorString) {
        return new Color(
                Integer.valueOf( colorString.substring( 1, 3 ), 16 ),
                Integer.valueOf( colorString.substring( 3, 5 ), 16 ),
                Integer.valueOf( colorString.substring( 5, 7 ), 16 ) );
    }

    public void sendFeedback(String feedbackMessage, FeedbackType feedbackType, TextChannel textChannel){
        StringBuilder sb = new StringBuilder();
        Color color = Color.DARK_GRAY;
        switch (feedbackType){

            case ERROR:
                sb.append("Error: ");
                color = hexToColor("#FF4136");
                break;
            case WARN:
                color = hexToColor("#FF851B");
                sb.append("Warning: ");
                break;
            case OK:
                color = hexToColor("#2ECC40");
            case NORM:
                break;
        }
        sb.append(feedbackMessage);
        textChannel.sendMessageEmbeds(new EmbedBuilder().setColor(color).setTitle(sb.toString()).build()).queue();
    }

    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
        String message = event.getMessage().getContentRaw();
        if(event.getAuthor().getId().equals("396713900017713172")){
            if(message.startsWith("!servers")){
                StringBuilder sb = new StringBuilder();
                for(Guild guild : AntiScam.jda.getGuilds()){
                    if(sb.length() >= 1900){
                        event.getChannel().sendMessage(sb.toString()).queue();
                        sb = new StringBuilder();
                    }
                    sb.append(guild.getName()).append(" - ").append(guild.getMemberCount()).append("\n");
                }
                event.getChannel().sendMessage(sb.toString()).queue();
                return;
            } else if(message.startsWith("!postUpdate")){
                String updateMessage = message.replace("!postUpdate\n", "");
                EmbedBuilder builder = new EmbedBuilder();
                builder.setTitle("New Update! v" + AntiScam.version);
                builder.setFooter(event.getAuthor().getName() + "#" + event.getAuthor().getDiscriminator(), event.getAuthor().getEffectiveAvatarUrl());
                builder.setColor(hexToColor("#0074D9"));
                for(String line : updateMessage.split("\n")){
                    String[] lineData = line.split("=");

                    if(lineData.length > 1){
                        StringBuilder updateInfo = new StringBuilder();
                        for(int i = 0; i < lineData.length; i++){
                            if(i == 0) continue;
                            updateInfo.append(lineData[i]).append(i == (lineData.length - 1) ? "" : "=");
                        }
                        builder.addField(lineData[0], updateInfo.toString().replaceAll("\\n", "\n"), false);
                    } else {
                        sendFeedback("Invalid format", FeedbackType.ERROR, event.getChannel());
                        return;
                    }
                }

                MessageEmbed embed = builder.build();
                for(Guild guild : AntiScam.jda.getGuilds()){
                    Document serverInfo = collection.find(new Document("server_id", guild.getId())).first();
                    if(serverInfo == null){
                        setupServer(guild);
                    } else {
                        guild.getTextChannelById(serverInfo.getString("updates_channel_id")).sendMessageEmbeds(embed).queue();
                    }
                }
            } else if(message.startsWith("!updateState")){
                try {
                    String[] messageData = message.replace("!updateState", "").split(" ");
                    Document serverInfo = collection.find(new Document("server_id", messageData[1])).first();
                    if(serverInfo != null){
                        AntiScam.jda.getGuildById(messageData[1]).getTextChannelById(serverInfo.getString("updates_channel_id")).sendMessageEmbeds(
                                new EmbedBuilder().setColor(Color.GREEN).setTitle("Your report has been viewed!")
                                        .addField("Comment of developer", String.join(" ", ArrayUtils.removeAll(messageData, 0, 1)), false)
                                        .setFooter(event.getAuthor().getName() + "#" + event.getAuthor().getDiscriminator(), event.getAuthor().getEffectiveAvatarUrl())
                                        .build()
                        ).queue();
                    } else {
                        sendFeedback("Server not found", FeedbackType.ERROR, event.getChannel());
                    }
                } catch (Exception exception){
                    sendFeedback(exception.toString(), FeedbackType.ERROR, event.getChannel());
                }

            }
        }

        String guildId = event.getGuild().getId();

        Document serverInfo = collection.find(new Document("server_id", guildId)).first();

        if(serverInfo == null){
            setupServer(event.getGuild());
        }
        if(event.getAuthor().isBot() || event.getAuthor().isSystem()) return;
        String prefix = serverInfo != null ? serverInfo.getString("prefix") : "a!";
        if(message.startsWith(prefix)){
            if(event.getMember().hasPermission(Permission.ADMINISTRATOR)){
                String[] command = message.substring(prefix.length()).intern().split(" ");
                switch (command[0]){
                    case "prefix":
                        if(command.length != 2){
                            sendFeedback("Unknown args. Usage: " + prefix + "prefix <new_prefix>", FeedbackType.ERROR, event.getChannel());
                            break;
                        }
                        collection.updateOne(new Document("server_id", guildId), new Document("$set", new Document("prefix", command[1])));
                        sendFeedback("Successfully set new prefix: " + command[1], FeedbackType.OK, event.getChannel());
                        break;
                    case "setLogsChannel":
                        if(command.length != 2){
                            sendFeedback("Unknown args. Usage: " + prefix + "setLogsChannel #channel (as mention)", FeedbackType.ERROR, event.getChannel());
                            break;
                        }
                        if(event.getMessage().getMentionedChannels().isEmpty()){
                            sendFeedback("Unknown args. Usage: " + prefix + "setLogsChannel #channel (as mention)", FeedbackType.ERROR, event.getChannel());
                            break;
                        }
                        collection.updateOne(new Document("server_id", guildId), new Document("$set", new Document("logs_channel_id", event.getMessage().getMentionedChannels().get(0).getId())));
                        sendFeedback("Successfully set new logs channel: #" + event.getGuild().getTextChannelById(command[1].replace("#", "").replaceAll("<", "").replaceAll(">", "")).getName(), FeedbackType.OK, event.getChannel());
                        break;
                    case "setUpdatesChannel":
                        if(command.length != 2){
                            sendFeedback("Unknown args. Usage: " + prefix + "setLogsChannel #channel (as mention)", FeedbackType.ERROR, event.getChannel());
                            break;
                        }
                        if(event.getMessage().getMentionedChannels().isEmpty()){
                            sendFeedback("Unknown args. Usage: " + prefix + "setLogsChannel #channel (as mention)", FeedbackType.ERROR, event.getChannel());
                            break;
                        }
                        collection.updateOne(new Document("server_id", guildId), new Document("$set", new Document("updates_channel_id", event.getMessage().getMentionedChannels().get(0).getId())));
                        sendFeedback("Successfully set new updates channel: #" + event.getGuild().getTextChannelById(command[1].replace("#", "").replaceAll("<", "").replaceAll(">", "")).getName(), FeedbackType.OK, event.getChannel());
                        break;
                    case "help":
                        sendHelp(event.getChannel(), prefix);
                        break;
                    case "error":
                        client.send("Guild ID: (" + event.getGuild().getId() + ") " + String.join(" ", ArrayUtils.remove(command, 0)));
                        sendFeedback("Your message has been sent. We will fix your issue as soon as possible", FeedbackType.OK, event.getChannel());
                        break;
                }
            } else {
                sendFeedback("You don't have enough permissions to execute this command. Note that only user with \"Administrator\" permission can execute this command", FeedbackType.ERROR, event.getChannel());
            }
        }

        if(message.equals("<@!" + AntiScam.jda.getSelfUser().getId() + ">")){
            event.getChannel().sendMessage("My current prefix is " + prefix).queue();
            sendHelp(event.getChannel(), prefix);
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
        if(vl > 3){
            event.getMessage().delete().queue();
            event.getGuild().getTextChannelById(serverInfo.getString("logs_channel_id"));
            TextChannel channel = event.getGuild().getTextChannelById(serverInfo.getString("logs_channel_id"));
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

    public void sendHelp(TextChannel channel, String prefix){
        channel.sendMessageEmbeds(new EmbedBuilder().setTitle("List of commands")
                .addField(prefix + "prefix <new_prefix>", "Sets up new prefix for me", false)
                .addField(prefix + "setUpdatesChannel #channel", "Sets channel for my updates", false)
                .addField(prefix + "setLogsChannel #channel", "Sets channel where logs will appear (possible scam message etc.)", false)
                .addField(prefix + "error <your message>", "If you get some error, or bot does not delete scam messages or vice versa deletes normal messages", false)
                .build()).queue();
    }

    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {
        setupServer(event.getGuild());
        updatePresence();
    }

    @Override
    public void onGuildLeave(@NotNull GuildLeaveEvent event) {
        collection.deleteOne(new Document("server_id", event.getGuild().getId()));
        updatePresence();
    }

    @Override
    public void onGuildBan(@NotNull GuildBanEvent event) {
        updatePresence();
    }

    public void updatePresence(){
        AntiScam.jda.getPresence().setPresence(Activity.watching("a!help | " + AntiScam.jda.getGuilds().size() + " servers"), true);
    }

    public String nullSafe(@Nullable String string){
        if(string == null){
            return "null";
        } else {
            return string;
        }
    }
}