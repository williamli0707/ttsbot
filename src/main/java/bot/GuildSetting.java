package bot;

public class GuildSetting {
    String serverLang;
    boolean xSaid, allChannels;
    long channel;
	public GuildSetting(){

    }
    public String getServerLang() {
        return serverLang;
    }
    public boolean isxSaid() {
        return xSaid;
    }
    public boolean isAllChannels() {
        return allChannels;
    }
    public long getChannel() {
        return channel;
    }

}
