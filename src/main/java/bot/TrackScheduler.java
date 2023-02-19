package bot;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import java.util.ArrayDeque;
import java.util.Queue;

public class TrackScheduler extends AudioEventAdapter {
	private final Queue<AudioTrack> q;
	private final AudioPlayer player;

	public TrackScheduler(AudioPlayer ap) {
		q = new ArrayDeque<>();
		player = ap;
	}
	
	public void queue(AudioTrack track) {
		if(!player.startTrack(track, true)) q.offer(track);
	}


	@Override
	public void onTrackStart(AudioPlayer player, AudioTrack track) {
		System.err.println("Track Started");
	}

	public void nextTrack(){
		player.startTrack(q.poll(), false);
	}

	@Override
	public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
		if(endReason.mayStartNext) nextTrack();

	    // endReason == FINISHED: A track finished or died by an exception (mayStartNext = true).
	    // endReason == LOAD_FAILED: Loading of a track failed (mayStartNext = true).
	    // endReason == STOPPED: The player was stopped.
	    // endReason == REPLACED: Another track started playing while this had not finished
	    // endReason == CLEANUP: Player hasn't been queried for a while, if you want you can put a
	    //                       clone of this back to your queue
	}
}
