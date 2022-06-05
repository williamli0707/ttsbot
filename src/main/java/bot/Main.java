package bot;


import com.google.cloud.texttospeech.v1.*;
import com.google.protobuf.ByteString;
import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.ReplaceOptions;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;
import org.bson.Document;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.eq;

public class Main extends ListenerAdapter {
	public static long quota;
	public static JDA jda;
	public static String prefix = ",";

	private final AudioPlayerManager playerManager;
	private final HashMap<Long, GuildMusicManager> musicManagers;
	private final HashMap<Long, GuildSetting> guildSettingHashMap;
	private final HashMap<Long, MemberSetting> memberSettingHashMap;
	private final double memorySize = ((com.sun.management.OperatingSystemMXBean) ManagementFactory
			.getOperatingSystemMXBean()).getTotalPhysicalMemorySize();
	private final double diskSize = new File("/").getTotalSpace();
	private final ReplaceOptions options = new ReplaceOptions().upsert(true);

	private static MongoCollection<Document> guilds, members;

	private final String[] validVoices = {"af-ZA", "ar-XA", "bn-IN", "bg-BG", "ca-ES", "yue-HK", "cs-CZ", "da-DK",
			"nl-BE", "nl-NL", "en-AU", "en-IN", "en-GB", "en-US", "fil-PH", "fi-FI", "fr-CA",
			"fe-FR", "de-DE", "el-GR", "gu-IN", "hi-IN", "hu-HU", "is-IS", "id-ID", "it-IT",
			"ja-JP", "kn-IN", "ko-KR", "lv-LV", "ms-MY", "ml-IN", "cmn-CN",  "nb-NO", "pl-PL",
			"pt-PT", "pa-IN", "ro-RO", "ru-RU", "sr-RS", "sk-SK", "es-ES", "es-US", "sv-SE",
			"ta-IN", "te-IN", "th-TH", "tr-TR", "uk-UA", "vi-VN"};
	
	public static void main(String[] args) {
		if(args.length == 0) {
			System.out.println("No token provided");
			return;
		}
		String token = args[0];
		try {
			jda = JDABuilder.createDefault(token).build();
			System.out.println("Logged in as " + jda.getSelfUser().getName() + "#" + jda.getSelfUser().getDiscriminator());
		} catch (Exception e) {
			System.out.println("Login failed");
			e.printStackTrace();
		}

		ConnectionString connectionString = new ConnectionString(args[1]);


		System.err.println("credentials path: " + System.getenv("GOOGLE_APPLICATION_CREDENTIALS"));
		System.err.println("jvm version: " + System.getProperty("java.version"));

		jda.addEventListener(new Main(connectionString));
		jda.getPresence().setActivity(Activity.playing("1234"));
	}

	private Main(ConnectionString connectionString) {
		musicManagers = new HashMap<>();
		this.playerManager = new DefaultAudioPlayerManager();
		AudioSourceManagers.registerLocalSource(playerManager);
		guildSettingHashMap = new HashMap<>();
		memberSettingHashMap = new HashMap<>();
		MongoClient mongoClient = MongoClients.create(connectionString);
		guilds = mongoClient.getDatabase("main").getCollection("guilds");
		members = mongoClient.getDatabase("main").getCollection("members");
		loadSettings();
	}

	private synchronized GuildMusicManager getGuildAudioPlayer(Guild guild) {
		long guildId = Long.parseLong(guild.getId());
		GuildMusicManager musicManager = musicManagers.get(guildId);

		if (musicManager == null) {
			musicManager = new GuildMusicManager(playerManager);
			musicManagers.put(guildId, musicManager);
		}

		guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());

		return musicManager;
	}

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		if(event.getAuthor().isBot()) return;
		// Gets the raw message content and binds it to a local variable.
		String message = event.getMessage().getContentRaw();
		// So we don't have to access event.getChannel() every time.
		MessageChannel channel = event.getChannel();
		GuildChannel guildChannel = event.getGuildChannel();
		if(event.getAuthor().isBot()) return;

		if(message.startsWith(Main.prefix)) {
			//main commands
			message = message.substring(Main.prefix.length());
			if(message.equals("join")) {
				if(!event.getGuild().getSelfMember().hasPermission(guildChannel, Permission.VOICE_CONNECT)) channel.sendMessage("Permission not granted").queue();
				AudioChannel vc = event.getMember().getVoiceState().getChannel();
				if(vc == null) {
					channel.sendMessage("User not connected to a voice channel").queue();
					return;
				}
				AudioManager am = event.getGuild().getAudioManager();
				am.openAudioConnection(vc);
				channel.sendMessage("Connected").queue();

			}
			else if(message.equals("leave")) {
				AudioChannel vc = event.getGuild().getSelfMember().getVoiceState().getChannel();
				if(vc == null) {
					channel.sendMessage("Not connected to a voice channel").queue();
					return;
				}
				AudioManager am = event.getGuild().getAudioManager();
				am.closeAudioConnection();
				channel.sendMessage("Left voice channel").queue();
			}
			else if(message.equals("help")) {
				//TODO: autojoin cannot be used in conjunction with all channels
				//TODO: uptime channel settings set debug (update info)
				//uptime: RuntimeMXBean, getUptime()
				EmbedBuilder eb = new EmbedBuilder();
				String p = Main.prefix;
				eb.setTitle("Command List");
				eb.addField("Main Commands", "`join`: joins the voice channel you're in\n"
						 + "`leave`: leaves the voice channel TTS Bot is in\n"
						 + "`skip`: clears the message queue"
						 + "Once the bot is in a voice channel, say anything in the channel you set up, and the bot will say whatever you write in the voice channel. ", false);
				eb.addField("Extra Commands", "`tts <message>`: Generates a TTS and sends it into the current text channel\n"
						 + "`uptime`: gets the current uptime of the bot\n"
						 + "`info`: Gets information about the bot\n"
						 + "`channel`: Shows the current set up channel, or none if `all channels` is true\n"
						 + "`ping`: Gets the current ping to Discord", false);
				eb.addField("Settings", "`settings`: Shows the current settings\n"
						 + "`set`: Changes a setting\n"
						 + "`set channel`: Changes the channel setup to the current channel\n"
						 + "`voices <language>`: Changes the current voice setting", false);
				eb.addField("Uncategorized", "`help`: List of available commands\n"
						 + "`debug`: Debug commands for bot", false);
				eb.setFooter(event.getMember().getUser().getName() + "#" + event.getMember().getUser().getDiscriminator(), event.getMember().getUser().getAvatarUrl());
				channel.sendMessageEmbeds(eb.build()).queue();
			}
			else if(message.equals("info")) {
				EmbedBuilder eb = new EmbedBuilder();
				double memory = Math.floor(memorySize/1000) * 1000, disk = Math.floor(diskSize/1000000) * 1000;
				eb.setTitle("Info");
				eb.addField("TTS", jda.getSelfUser().getName() + "#" + jda.getSelfUser().getDiscriminator(), true);
				eb.addField("Disk Size", disk / 1000000 + " GB", true);
				eb.addField("Memory", memory / 1000000 + " MB", true);
				eb.addField("Ping", String.valueOf(jda.getGatewayPing()), true);

				channel.sendMessageEmbeds(eb.build()).queue();
			}
			else if(message.equals("start")){
				guildJoin(event.getGuild());
			}
			else if(message.equals("ping")){
				channel.sendMessage(String.valueOf(jda.getGatewayPing()) + " ms").queue();
			}
			else if(message.equals("skip")){
				musicManagers.get(event.getGuild().getIdLong()).player.stopTrack();
				event.getMessage().addReaction(":white_check_mark:").queue();
			}
			else if(message.equals("set")){

			}
			else if(message.startsWith("set ")){
				message = message.substring(4);
				String[] split = message.split(" ");
				//member specific settings
				switch (split[0]) {
					case "voice":
					case "gender":
						MemberSetting ms = new MemberSetting();
						long memberId = event.getAuthor().getIdLong();
						if (memberSettingHashMap.containsKey(memberId))
							ms = memberSettingHashMap.get(memberId);
						if (split[0].equals("voice")) {
							if (!isValidVoice(split[1])) {
								event.getChannel().sendMessage(split[1] + " is not a valid voice. " +
										"Check https://docs.google.com/spreadsheets/d/1mKLBunYTfeUrIlBLml0IzLrA_F-UvkZHClJO7PeW0Fs/edit?usp=sharing " +
										", or alternatively use the command \"voices\" for a list of valid links, and make sure to use the \"Language code\" value when changing the voice setting. ").queue();
								return;
							}
							ms.setMemberLang(split[1]);
							event.getChannel().sendMessage("Your language was set to " + ms.getMemberLang() + ". ").queue();
						}
						if (split[0].equals("gender")) {
							if (!ms.setMemberGender(split[1])) {
								event.getChannel().sendMessage(split[1] + " is not a valid gender. Please set the gender to \"Male\", \"Female\", or \"Neutral\". ").queue();
								return;
							}
							event.getChannel().sendMessage("Your voice gender was set to " + split[1].toLowerCase() + ". ").queue();
						}
						memberSettingHashMap.put(memberId, ms);
						Document member = ms.toDocument(memberId);
						members.replaceOne(eq("_id", memberId), member, options);
						break;

					//guild specific settings
					case "xsaid":
					case "servervoice":
					case "channel":
						GuildSetting gs = new GuildSetting();
						long guildId = event.getGuild().getIdLong();
						if (guildSettingHashMap.containsKey(event.getAuthor().getIdLong()))
							gs = guildSettingHashMap.get(guildId);
						if (split[0].equals("xsaid")) {
							boolean set = false;
							if (split[1].equals("true")) set = true;
							gs.setXSaid(set);
							if (set) event.getChannel().sendMessage("The bot now says who said the message when playing audio. ").queue();
							else event.getChannel().sendMessage("The bot now does not say who said the message when playing audio. ").queue();
						} else if (split[0].equals("servervoice")) {
							if (!isValidVoice(split[1])) {
								event.getChannel().sendMessage(split[1] + " is not a valid voice. " +
										"Check https://docs.google.com/spreadsheets/d/1mKLBunYTfeUrIlBLml0IzLrA_F-UvkZHClJO7PeW0Fs/edit?usp=sharing " +
										", or alternatively use the command \"voices\" for a list of valid links, and make sure to use the \"Language code\" value when changing the voice setting. ").queue();
								return;
							}
							gs.setServerLang(split[1]);
							event.getChannel().sendMessage("Your guild's default language is now " + gs.getServerLang()).queue();
						} else if (split[0].equals("channel")) {
							gs.setChannel(event.getChannel().getIdLong());
							event.getChannel().sendMessage("Set the channel to " + event.getChannel().getName()).queue();
						}
						guildSettingHashMap.put(guildId, gs);
						Document guild = gs.toDocument(guildId);
						guilds.replaceOne(eq("_id", guildId), guild, options);
						break;
					case "prefix":
						prefix = split[1];
						event.getChannel().sendMessage("Set the prefix to " + prefix).queue();
						break;
				}
			}

		}
		else {
			//tts or return
			AudioChannel vc = event.getGuild().getSelfMember().getVoiceState().getChannel();
			if(!(guildSettingHashMap.get(event.getGuild().getIdLong()).isAutoJoin() &&
					event.getChannel().getIdLong() == guildSettingHashMap.get(event.getGuild().getIdLong()).getChannel()
					&& vc == null)) return;
			if(vc == null) return;
			if(!guildSettingHashMap.containsKey(event.getGuild().getIdLong()) ||
					(guildSettingHashMap.containsKey(event.getGuild().getIdLong())
							&& !guildSettingHashMap.get(event.getGuild().getIdLong()).isAllChannels()
			 				&& guildSettingHashMap.get(event.getGuild().getIdLong()).getChannel() != event.getChannel().getIdLong())) return;

			//requireVoice
			if(guildSettingHashMap.get(event.getGuild().getIdLong()).isRequireVoice() &&
					(event.getMember().getVoiceState() == null) || event.getMember().getVoiceState().equals(vc)) return;

			AudioTrack at = null;
			//remove links
			String removed = message.replaceAll("http.*?\\s", "");
			System.err.println(removed);
			if(!message.equals(removed)){
				message = removed;
				message += ". This message contained a link";
			}

			//xSaid
			if(guildSettingHashMap.get(event.getGuild().getIdLong()).isxSaid()) message = event.getMember().getNickname() + " said " + message;



			System.err.println("Recieved a message to play: " + message);
			if(memberSettingHashMap.containsKey(event.getAuthor().getIdLong())) {
				load(guildChannel, channel, message, memberSettingHashMap.get(event.getAuthor().getIdLong()).getMemberLang(),
						memberSettingHashMap.get(event.getAuthor().getIdLong()).getMemberGender());
			}
			else if (guildSettingHashMap.containsKey(event.getGuild().getIdLong())) {
				load(guildChannel, channel, message, guildSettingHashMap.get(event.getGuild().getIdLong()).getServerLang(),
						SsmlVoiceGender.NEUTRAL);
			}
			else load(guildChannel, channel, message, "en-US", SsmlVoiceGender.NEUTRAL);
		}
		super.onMessageReceived(event);
	}

	public EmbedBuilder settingList(){
		EmbedBuilder eb = new EmbedBuilder();
		//TODO
		return eb;
	}

	public void guildJoin(Guild guild){
		EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle("Welcome to TTS Bot!");
		eb.appendDescription("Hello! Someone invited me to your server! \n"
				+ "TTS Bot is a text to speech bot, as in, it reads messages from a text channel and speaks it into a voice channel\n\n"
				+ "Most commands need to be done on your server, such as `set channel` and `join`. \n"
				+ "I need someone to run the command `set channel` in the channel you want to use \n"
				+ "You can then do `join` in that channel and I will join your voice channel! \n"
				+ "Then, you can just type normal messages and I will say them, like magic! \n\n"
				+ "You can view all the commands with `help`.\n"
				+ "The default prefix is '" + prefix + "' but you can change it at any time.");

		guild.getDefaultChannel().sendMessageEmbeds(eb.build()).queue();
		System.err.println("recieved command");
	}

	@Override
	public void onGuildJoin (GuildJoinEvent event){

		EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle("Welcome to TTS Bot!");
		eb.appendDescription("Hello! Someone invited me to your server! \n"
				+ "TTS Bot is a text to speech bot, as in, it reads messages from a text channel and speaks it into a voice channel\n\n"
				+ "Most commands need to be done on your server, such as `set channel` and `join`. \n"
				+ "I need someone to run the command `set channel` in the channel you want to use \n"
				+ "You can then do `join` in that channel and I will join your voice channel! \n"
				+ "Then, you can just type normal messages and I will say them, like magic! \n\n"
				+ "You can view all the commands with `help`.\n"
				+ "The default prefix is '" + prefix + "' but you can change it at any time.");
		event.getGuild().getDefaultChannel().sendMessageEmbeds(eb.build()).queue();
		super.onGuildJoin(event);
	}

	private void load(final GuildChannel channel, final MessageChannel messageChannel, final String message, String lang, SsmlVoiceGender gender){
		GuildMusicManager gmm = getGuildAudioPlayer(channel.getGuild());

		try {
			getTTS(lang, message, gender);
		} catch (IOException e) {
			e.printStackTrace();
		}

		playerManager.loadItem("output.mp3", new AudioLoadResultHandler() {
			@Override
			public void trackLoaded(AudioTrack track) {
				play(channel.getGuild(), gmm, track);
				messageChannel.sendMessage("Queued").queue();
			}

			@Override
			public void playlistLoaded(AudioPlaylist playlist) {
				for (AudioTrack track : playlist.getTracks()) {
					play(channel.getGuild(), gmm, track);
				}
			}

			@Override
			public void noMatches() {
				// Notify the user that we've got nothing
				messageChannel.sendMessage("No matches found").queue();
			}

			@Override
			public void loadFailed(FriendlyException throwable) {
				// Notify the user that everything exploded
				messageChannel.sendMessage("Load failed").queue();
			}
		});

	}

	private void play(Guild guild, GuildMusicManager musicManager, AudioTrack track) {
		connectToFirstVoiceChannel(guild.getAudioManager());

		musicManager.scheduler.queue(track);
	}

	private void skipTrack(TextChannel channel) {
		GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
		musicManager.scheduler.nextTrack();

		channel.sendMessage("Skipped to next track.").queue();
	}

	private static void connectToFirstVoiceChannel(AudioManager audioManager) {
		if (!audioManager.isConnected()) {
			for (VoiceChannel voiceChannel : audioManager.getGuild().getVoiceChannels()) {
				audioManager.openAudioConnection(voiceChannel);
				break;
			}
		}
	}

	public static void getTTS(String lang, String text, SsmlVoiceGender gender) throws IOException {
		try (TextToSpeechClient textToSpeechClient = TextToSpeechClient.create()) {
		      // Set the text input to be synthesized
		      SynthesisInput input = SynthesisInput.newBuilder().setText(text).build();

		      // Build the voice request, select the language code ("en-US") and the ssml voice gender
		      // ("neutral")
		      VoiceSelectionParams voice =
		          VoiceSelectionParams.newBuilder()
		              .setLanguageCode(lang)
		              .setSsmlGender(gender)
		              .build();



		      // Select the type of audio file you want returned
		      AudioConfig audioConfig =
		          AudioConfig.newBuilder().setAudioEncoding(AudioEncoding.MP3).build();

		      // Perform the text-to-speech request on the text input with the selected voice parameters and
		      // audio file type
		      SynthesizeSpeechResponse response =
		          textToSpeechClient.synthesizeSpeech(input, voice, audioConfig);

		      // Get the audio contents from the response
		      ByteString audioContents = response.getAudioContent();
		      byte[] audio = audioContents.toByteArray();
		      ByteArrayInputStream bais = new ByteArrayInputStream(audio);

		      // Write the response to the output file.
		      try (OutputStream out = Files.newOutputStream(Paths.get("output.mp3"))) {
		        out.write(audioContents.toByteArray());
		        System.out.println("Audio content written to file \"output.mp3\"");
		      }
//		      return playerManager.decodeTrack(new MessageInput(bais)).decodedTrack;
		}
	}

	public boolean isValidVoice(String input) {
		for(String lang: validVoices) if(input.equals(lang)) return true;
		return false;
	}

	public void loadSettings(){
		MongoCursor<Document> guildList = guilds.find().iterator(), memberList = members.find().iterator();
		while(guildList.hasNext()){
			Document doc = guildList.next();
			guildSettingHashMap.put(doc.getLong("_id"), GuildSetting.fromDocument(doc));
		}
		while(memberList.hasNext()){
			Document doc = memberList.next();
			memberSettingHashMap.put(doc.getLong("_id"), MemberSetting.fromDocument(doc));
		}
		guildList.close();
	}

	public boolean hasLink(String text){
		System.err.println(text);
		String urlRegex = "^((https?|ftp)://|(www|ftp)\\\\.)?[a-z0-9-]+(\\\\.[a-z0-9-]+)+([/?].*)?$";
		Pattern p = Pattern.compile(urlRegex);
		Matcher m = p.matcher(text);
		return m.find();
	}
}
