package bot;


import com.google.cloud.texttospeech.v1.*;
import com.google.protobuf.ByteString;
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
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;

public class Main extends ListenerAdapter {
	public static long quota;
	public static JDA jda;
	public static String prefix = ",";

	private final AudioPlayerManager playerManager;
	private final HashMap<Long, GuildMusicManager> musicManagers;

	
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
		jda.addEventListener(new Main());
		jda.getPresence().setActivity(Activity.playing("qwopldfjksf"));
		
		
	}

	private Main() {
		musicManagers = new HashMap<>();
		this.playerManager = new DefaultAudioPlayerManager();
		AudioSourceManagers.registerLocalSource(playerManager);
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
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		if(event.getAuthor().isBot()) return;
		// Gets the raw message content and binds it to a local variable.
		String message = event.getMessage().getContentRaw().toLowerCase();
		// So we don't have to access event.getChannel() every time.
		TextChannel channel = event.getChannel();
		if(event.getAuthor().isBot()) return;
		if(message.startsWith(Main.prefix)) {
			//main commands
			message = message.substring(Main.prefix.length());
			if(message.equals("join")) {
				if(!event.getGuild().getSelfMember().hasPermission(channel, Permission.VOICE_CONNECT)) channel.sendMessage("Permission not granted").queue();
				VoiceChannel vc = event.getMember().getVoiceState().getChannel();
				if(vc == null) {
					channel.sendMessage("User not connected to a voice channel").queue();
					return;
				}
				AudioManager am = event.getGuild().getAudioManager();
				am.openAudioConnection(vc);
				channel.sendMessage("Connected").queue();

			}
			else if(message.equals("leave")) {
				VoiceChannel vc = event.getGuild().getSelfMember().getVoiceState().getChannel();
				if(vc == null) {
					channel.sendMessage("Not connected to a voice channel").queue();
					return;
				}
				AudioManager am = event.getGuild().getAudioManager();
				am.closeAudioConnection();
				channel.sendMessage("Left voice channel").queue();
			}
			else if(message.equals("help")) {
				EmbedBuilder eb = new EmbedBuilder();
				String p = Main.prefix;
				eb.setTitle("Command List");
				eb.addField("TTS", p + "join - joins the voice channel\n"
						+ p + "leave - leaves the voice channel\n", false);
				eb.setFooter(event.getMember().getUser().getName() + "#" + event.getMember().getUser().getDiscriminator(), event.getMember().getUser().getAvatarUrl());
				channel.sendMessageEmbeds(eb.build()).queue();
			}

		}
		else {
			//tts or return
			VoiceChannel vc = event.getGuild().getSelfMember().getVoiceState().getChannel();
			if(vc == null) return;
			AudioTrack at = null;
			load(channel, message);
		}
		super.onGuildMessageReceived(event);
	}

	private void load(final TextChannel channel, final String message){
		GuildMusicManager gmm = getGuildAudioPlayer(channel.getGuild());

		try {
			getTTS("en-US", message);
		} catch (IOException e) {
			e.printStackTrace();
		}

		playerManager.loadItem("output.mp3", new AudioLoadResultHandler() {
			@Override
			public void trackLoaded(AudioTrack track) {
				play(channel.getGuild(), gmm, track);
				channel.sendMessage("Queued").queue();
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
				channel.sendMessage("No matches found").queue();
			}

			@Override
			public void loadFailed(FriendlyException throwable) {
				// Notify the user that everything exploded
				channel.sendMessage("Load failed").queue();
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
		if (!audioManager.isConnected() && !audioManager.isAttemptingToConnect()) {
			for (VoiceChannel voiceChannel : audioManager.getGuild().getVoiceChannels()) {
				audioManager.openAudioConnection(voiceChannel);
				break;
			}
		}
	}

	public static void getTTS(String lang, String text) throws IOException {
		try (TextToSpeechClient textToSpeechClient = TextToSpeechClient.create()) {
		      // Set the text input to be synthesized
		      SynthesisInput input = SynthesisInput.newBuilder().setText(text).build();

		      // Build the voice request, select the language code ("en-US") and the ssml voice gender
		      // ("neutral")
		      VoiceSelectionParams voice =
		          VoiceSelectionParams.newBuilder()
		              .setLanguageCode("en-US")
		              .setSsmlGender(SsmlVoiceGender.MALE)
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
		      try (OutputStream out = new FileOutputStream("output.mp3")) {
		        out.write(audioContents.toByteArray());
		        System.out.println("Audio content written to file \"output.mp3\"");
		      }
//		      return playerManager.decodeTrack(new MessageInput(bais)).decodedTrack;
		}
	}

}
