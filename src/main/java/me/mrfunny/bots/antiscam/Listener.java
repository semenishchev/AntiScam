package me.mrfunny.bots.antiscam;

import club.minnced.discord.webhook.WebhookClient;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import me.mrfunny.bots.antiscam.ai.CheckService;
import me.mrfunny.bots.antiscam.secret.SuperSecretClass;
import me.mrfunny.bots.antiscam.util.FeedbackType;
import me.mrfunny.bots.antiscam.util.Pair;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.commons.lang3.ArrayUtils;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Listener extends ListenerAdapter {

    private final WebhookClient client;
    public Listener(String webhookUrl){
        client = WebhookClient.withUrl(webhookUrl);
    }
    private final String[] blacklistedWords = {"сначал", "эпик", "стим", "нитро", "ненадеж", "ненадёж", "разда", "нитру", "скин", "успел", "everyone"};
    private MongoCollection<Document> collection;
    private MongoCollection<Document> blockedServers;
    private final String[] mostOfScamLinks = {"discordapp.net", "discordapp.com", "discord.com", "youtu.be", "youtube.com", "discord.gg", "steamcommunity.com", "discordstatus.com", "discord.gift", "store.steampowered.com", "tenor.com", "vk.com", "imgur.com"};
    
    @Override
    public void onReady(@NotNull ReadyEvent event) {
        updatePresence();
        MongoClient client = MongoClients.create(SuperSecretClass.connectionString);
        MongoDatabase database = client.getDatabase("antiscam");
        collection = database.getCollection("servers");
        blockedServers = database.getCollection("blocked_servers");
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
            Category category = guild.createCategory("AntiScam").addMemberPermissionOverride(AntiScam.jda.getSelfUser().getIdLong(), permissions, new ArrayList<>()).addRolePermissionOverride(guild.getPublicRole().getIdLong(), new ArrayList<>(), permissions).complete();
            TextChannel updatesChannel = category.createTextChannel("antiscam-updates").addMemberPermissionOverride(AntiScam.jda.getSelfUser().getIdLong(), permissions, new ArrayList<>()).addRolePermissionOverride(guild.getPublicRole().getIdLong(), new ArrayList<>(), permissions).complete();
            TextChannel logsChannel = (category.createTextChannel("logs").addMemberPermissionOverride(AntiScam.jda.getSelfUser().getIdLong(), permissions, new ArrayList<>()).addRolePermissionOverride(guild.getPublicRole().getIdLong(), new ArrayList<>(), permissions).complete());
            logsChannel.sendMessage("Thanks for adding me! My current prefix is a!").queue(message -> sendHelp(logsChannel, "a!"));
            if(collection.find(new Document("server_id", guild.getId())).first() == null){
                collection.insertOne(new Document("server_id", guild.getId()).append("logs_channel_id", logsChannel.getId()).append("prefix", "a!").append("updates_channel_id", updatesChannel.getId()));
            }
        } catch (InsufficientPermissionException ignored){
        }
    }

    public static Color hexToColor(String colorString) {
        return new Color(Integer.valueOf(colorString.substring(1, 3), 16), Integer.valueOf(colorString.substring(3, 5), 16), Integer.valueOf(colorString.substring(5, 7), 16));
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
                        builder.addField(lineData[0], updateInfo.toString().replaceAll("\\\\n", "\n"), false);
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
                        Objects.requireNonNull(guild.getTextChannelById(serverInfo.getString("updates_channel_id"))).sendMessageEmbeds(embed).queue();
                    }
                }
            } else if(message.startsWith("!updateState")){
                try {
                    String[] messageData = message.replace("!updateState", "").split(" ");
                    Document serverInfo = collection.find(new Document("server_id", messageData[1])).first();
                    if(serverInfo != null){
                        AntiScam.jda.getGuildById(messageData[1]).getTextChannelById(serverInfo.getString("updates_channel_id")).sendMessageEmbeds(
                                new EmbedBuilder().setColor(hexToColor("#2ECC40")).setTitle("Your report has been viewed!")
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
            } else if(message.startsWith("!banServer")){
                try {
                    String[] messageData = message.replace("!updateState", "").split(" ");
                    Document serverInfo = collection.find(new Document("server_id", messageData[1])).first();
                    String reason = String.join(" ", ArrayUtils.removeAll(messageData, 0, 1));
                    if(reason.trim().equals("")){
                        reason = "unspecified";
                    }
                    if(serverInfo != null){
                        AntiScam.jda.getGuildById(messageData[1]).getTextChannelById(serverInfo.getString("updates_channel_id")).sendMessageEmbeds(
                                new EmbedBuilder().setColor(hexToColor("#FF4136")).setTitle("Your server has been banned from posting errors! Now you can't post errors via " + serverInfo.getString("prefix") + "error")
                                        .addField("Reason", reason, false)
                                        .addField("We are wrong?", "Contact developer (contacts in footer)", false)
                                        .setFooter(event.getAuthor().getName() + "#" + event.getAuthor().getDiscriminator(), event.getAuthor().getEffectiveAvatarUrl())
                                        .build()
                        ).queue();
                    }
                    blockedServers.insertOne(new Document("server_id", messageData[1]).append("reason", reason));
                } catch (Exception exception){
                    sendFeedback(exception.toString(), FeedbackType.ERROR, event.getChannel());
                }
            } else if(message.startsWith("!serverInfo")) {
                String server = message.replace("!serverInfo ", "");
                Guild guild = AntiScam.jda.getGuildById(server);
                if(guild == null){
                    sendFeedback("Server not found", FeedbackType.ERROR, event.getChannel());
                    return;
                }
                EmbedBuilder eb = new EmbedBuilder().setTitle("Info about server " + server).setColor(Color.ORANGE)
                        .addField("ID", server, true)
                        .addField("Name", guild.getName(), true)
                        .addField("Members count", guild.getMemberCount() + "", true)
                        .addField("Owner" ,guild.getOwner() == null ? "pepega" : guild.getOwner().getUser().getName() + "#" + guild.getOwner().getUser().getDiscriminator(), false);
                event.getChannel().sendMessageEmbeds(eb.build()).queue();
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
            if(event.getMember() == null) {
                sendFeedback("You are... null?", FeedbackType.ERROR, event.getChannel());
                return;
            }
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
                        Document possibleBan = blockedServers.find(new Document("server_id", event.getGuild().getId())).first();
                        if(possibleBan != null){
                            sendFeedback("Your are banned from posting errors. Reason: " + possibleBan.getString("reason"), FeedbackType.ERROR, event.getChannel());
                        } else {
                            client.send("Guild ID: (" + event.getGuild().getId() + ") " + String.join(" ", ArrayUtils.remove(command, 0)));
                            sendFeedback("Your message has been sent. We will fix your issue as soon as possible", FeedbackType.OK, event.getChannel());
                        }
                        break;
                }
            } else {
                sendFeedback("You don't have enough permissions to execute this command. Note that only user with \"Administrator\" permission can execute this command", FeedbackType.ERROR, event.getChannel());
            }
        }

        if(message.equals("<@!" + AntiScam.jda.getSelfUser().getId() + ">") || message.equals("@" + event.getGuild().getSelfMember().getEffectiveName() + "#" + AntiScam.jda.getSelfUser().getDiscriminator())){
            event.getChannel().sendMessage("My current prefix is " + prefix).queue();
            sendHelp(event.getChannel(), prefix);
        }

        checkMessage(event.getMessage(), event.getAuthor(), event.getGuild(), serverInfo, event.getChannel(), false);
    }

    public void checkMessage(Message messageObject, User author, Guild guild, Document serverInfo, TextChannel genericChannel, boolean edited){
        String message = messageObject.getContentRaw();

        int vl = 0;
        for(String word : blacklistedWords){
            if(message.contains(word)){
                vl++;
            }
        }
        ArrayList<Double> aiScores = new ArrayList<>();
        String addition = "";
        for(String line : message.split("\n")){
            words: for(String word : line.split(" ")){
                if(word.contains(".")){
                    if(word.contains("/")){
                        if(word.startsWith("http:")){
                            vl = -1;
                            break;
                        }
                        if(word.startsWith("https://bit.ly")){
                            try {
                                org.jsoup.nodes.Document document = Jsoup.connect(word.replace("bit.ly", "bitly.com") + "+").get();
                                Pattern pattern = Pattern.compile("(long_url.+?(?=\",))");
                                Matcher matcher = pattern.matcher(document.toString());
                                if(matcher.find()){
                                    word = matcher.group(1).replaceAll("long_url\": \"", "");
                                }
                            } catch (IOException e) {
                                System.out.println("Error: " + e);
                                vl = -1;
                            }
                        }
                        String[] wordData = word.replace("https://", "").split("/");
                        String[] domainData = wordData[0].split("\\.");
                        String domain = (domainData.length > 2 ? joinFromIndex(domainData) : wordData[0]);
                        boolean isFound = false;
                        for(String fullyWhitelisted : mostOfScamLinks) {
                            if(fullyWhitelisted.equals(domain)){
                                isFound = true;
                                break;
                            }
                        }
                        if(!isFound){
                            Pair<Integer, Double> pair = proceedLink(domain);
                            vl = pair.key;
                            aiScores.add(pair.value);
                        } else {
                            continue;
                        }
                    } else {
                        String[] domainData = word.split("\\.");
                        String domain = (domainData.length > 2 ? joinFromIndex(domainData) : word);
                        for(String fullyWhitelisted : mostOfScamLinks) {
                            String[] scamLinkData = fullyWhitelisted.split("\\.");
                            String possibleScamLink = (scamLinkData.length == 0 ? fullyWhitelisted : scamLinkData[0]);
                            if(possibleScamLink.equals(domain)) continue words;
                        }
                        Pair<Integer, Double> pair = proceedLink(domain);
                        vl = pair.key;
                        addition = pair.addition;
                        aiScores.add(pair.value);
                    }
                }
                if(word.startsWith("http:")){
                    vl = -1;
                    break;
                }
                if(!word.startsWith("http")) continue;
                if(word.contains("tradeOffer") && !word.startsWith("https://steamcommunity.com")){
                    vl = 10;
                } else if(word.contains("nitro") && !word.startsWith("https://discord.gift")){
                    vl = 10;
                } else if(word.contains("stea") && !word.startsWith("https://steamcommunity.com/") && !word.startsWith("https://store.steampowered.com/")){
                    vl = 10;
                }
            }
        }

        double avg = average(aiScores);
        if(vl > 3 && (avg != 1.0 || aiScores.isEmpty())){
            messageObject.delete().queue();
            guild.getTextChannelById(serverInfo.getString("logs_channel_id"));
            TextChannel channel = guild.getTextChannelById(serverInfo.getString("logs_channel_id"));
            if(channel == null){
                channel = genericChannel;
            }
            channel.sendMessage("User " + author.getAsMention() + " sent scam message!").queue();
            channel.sendMessageEmbeds(new EmbedBuilder()
                    .setTitle("User "
                            + nullSafe(author.getName())
                            + "#"
                            + author.getDiscriminator()
                            + " (ID: "
                            + author.getId() + ")"
                    ).addField("Message" + (edited ? " (edited message)" : ""), message, false)
                    .addField("AI", "Score: " + average(aiScores) + ", addition: " + addition, false)
                    .setFooter("From 0.45 to 0.99 is possibly a scam. If not, report this via " + serverInfo.getString("prefix") + "error falsePositive <message> <score>")
                    .build()).queue();
        } else if(vl == -1){
            messageObject.delete().queue();
        }
    }

    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {
        setupServer(event.getGuild());
        updatePresence();
        client.send("#Guild ID: (" + event.getGuild().getName() + ") joined");
    }


    @Override
    public void onGuildLeave(@NotNull GuildLeaveEvent event) {
        collection.deleteOne(new Document("server_id", event.getGuild().getId()));
        updatePresence();
        client.send("#Guild ID: (" + event.getGuild().getName() + ") left");
    }

    @Override
    public void onGuildBan(@NotNull GuildBanEvent event) {
        updatePresence();
    }

    @Override
    public void onGuildMessageUpdate(@NotNull GuildMessageUpdateEvent event) {
        String guildId = event.getGuild().getId();
        Document serverInfo = collection.find(new Document("server_id", guildId)).first();
        checkMessage(event.getMessage(), event.getAuthor(), event.getGuild(), serverInfo, event.getChannel(), true);
    }

    public String nullSafe(@Nullable String string){
        if(string == null){
            return "null";
        } else {
            return string;
        }
    }

    public void updatePresence(){
        AntiScam.jda.getPresence().setPresence(Activity.watching("a!help | " + AntiScam.jda.getGuilds().size() + " servers"), true);
    }

    private double average(ArrayList<Double> doubles){
        double preFinal = 0;
        for(double double_ : doubles){
            preFinal += double_;
        }
        if(doubles.size() == 0) return 0;
        return preFinal / (double) doubles.size();
    }

    private Pair<Integer, Double> proceedLink(String domain) {
        double biggestScore = 0;
        int vl = 0;
        String lastScamLink = "";
        for (String possibleScamLink : mostOfScamLinks) {
            String[] linkData = domain.split("\\.");
            String[] scamLinkData = possibleScamLink.split("\\.");
            String link = (scamLinkData.length == 0 ? possibleScamLink : scamLinkData[0]);
            if(domain.startsWith(link)){
                if(!domain.equals(possibleScamLink)){
                    vl = 10;
                }
            } else {
                double score = CheckService.score(link, (linkData.length == 0 ? domain : linkData[0]));
                if(score > biggestScore){
                    biggestScore = score;
                    lastScamLink = possibleScamLink;
                }
                if(score > 0.46D) vl = 10;
            }

        }
        Pair<Integer, Double> pair =  new Pair<>(vl, biggestScore);
        pair.addition = lastScamLink;
        return pair;
    }

    private String joinFromIndex(String[] array){
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < array.length; i++){
            if(i >= 1){
                sb.append(array[i]).append((i == array.length - 1 ? "" :"."));
            }
        }
        return sb.toString();
    }

    public void sendHelp(TextChannel channel, String prefix){
        channel.sendMessageEmbeds(new EmbedBuilder().setTitle("List of commands")
                .addField(prefix + "prefix <new_prefix>", "Sets up new prefix for me", false)
                .addField(prefix + "setUpdatesChannel #channel", "Sets channel for my updates", false)
                .addField(prefix + "setLogsChannel #channel", "Sets channel where logs will appear (possible scam message etc.)", false)
                .addField(prefix + "error <your message>", "If you get some error, or bot does not delete scam messages or vice versa deletes normal messages", false)
                .build()).queue();
    }
}
