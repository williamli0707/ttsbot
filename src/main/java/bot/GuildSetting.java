package bot;

import org.bson.Document;

public class GuildSetting {
    String serverLang;
    boolean xSaid, allChannels, requireVoice, autoJoin;
    long channel;
	public GuildSetting(){
        serverLang = "en-US";
        requireVoice = true;
        autoJoin = false;
        xSaid = false;
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

    public void setServerLang(String lang) {
        serverLang = lang;
    }
    public void setXSaid(boolean x){
        xSaid = x;
    }
    public void setAllChannels(boolean all) {
        allChannels = all;
        if(all) channel = 0;
    }
    public void setChannel(long channel) {
        this.channel = channel;
    }
    public void setAutoJoin(boolean auto) {this.autoJoin = auto;}
    public void setRequireVoice(boolean v) {requireVoice = v;}

    public Document toDocument(long id) {
        Document document = new Document();
        document.append("_id", id);
        document.append("serverLang", serverLang);
        document.append("xSaid", xSaid);
        document.append("allChannels", allChannels);
        document.append("channel", channel);
        document.append("autoJoin", autoJoin);
        document.append("requireVoice", requireVoice);
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
        return setting;
    }
}
