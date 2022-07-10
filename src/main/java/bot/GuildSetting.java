package bot;

import org.bson.Document;

public class GuildSetting {
    String serverLang, prefix;
    boolean xSaid, allChannels, requireVoice, autoJoin;

    int volume;
    long channel;
	public GuildSetting(){
        serverLang = "en-US";
        requireVoice = true;
        autoJoin = false;
        xSaid = false;
        prefix = ",";
        volume = 100;
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
    public boolean isAutoJoin() { return autoJoin; }
    public boolean isRequireVoice() { return requireVoice; }
    public String getPrefix() {
        return prefix;
    }

    public int getVolume() {
        return volume;
    }


    public void setServerLang(String lang) {
        serverLang = lang;
    }
    public void setXSaid(boolean x){
        xSaid = x;
    }
    public void setAllChannels(boolean all) {
        allChannels = all;
        if(all) {
            channel = 0;
            autoJoin = false;
        }
    }
    public void setChannel(long channel) {
        allChannels = false;
        this.channel = channel;
    }
    public void setAutoJoin(boolean auto) {
        this.autoJoin = auto;
        if(auto) allChannels = false;
    }
    public void setRequireVoice(boolean v) {requireVoice = v;}
    public void setPrefix(String prefix) {this.prefix = prefix; }
    public void setVolume(int v) {
        this.volume = v;
    }

    public Document toDocument(long id) {
        Document document = new Document();
        document.append("_id", id);
        document.append("serverLang", serverLang);
        document.append("xSaid", xSaid);
        document.append("allChannels", allChannels);
        document.append("channel", channel);
        document.append("autoJoin", autoJoin);
        document.append("requireVoice", requireVoice);
        document.append("prefix", prefix);
        document.append("volume", volume);
        return document;
    }
    public static GuildSetting fromDocument(Document document){
        GuildSetting setting = new GuildSetting();
        setting.setServerLang(document.getString("serverLang"));
        setting.setXSaid(document.getBoolean("xSaid"));
        setting.setAllChannels(document.getBoolean("allChannels"));
        setting.setChannel(document.getLong("channel"));
        setting.setAutoJoin(document.getBoolean("autoJoin"));
        setting.setRequireVoice(document.getBoolean("requireVoice"));
        setting.setPrefix(document.getString("prefix"));
        setting.setVolume(document.getInteger("volume"));
        return setting;
    }
}
