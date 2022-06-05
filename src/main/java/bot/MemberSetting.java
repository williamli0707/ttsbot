package bot;

import com.google.cloud.texttospeech.v1.SsmlVoiceGender;
import org.bson.Document;

public class MemberSetting {
	String memberLang;
	SsmlVoiceGender memberGender;
	public MemberSetting(){
		memberLang = "en-US";
		memberGender = SsmlVoiceGender.NEUTRAL;
	}
	public String getMemberLang() {
		return memberLang;
	}
	public void setMemberLang(String memberLang) {
		this.memberLang = memberLang;
	}

	public SsmlVoiceGender getMemberGender() {
		return memberGender;
	}
	public boolean setMemberGender(String memberGender) {
		if(memberGender.equalsIgnoreCase("NEUTRAL")) this.memberGender = SsmlVoiceGender.NEUTRAL;
		else if(memberGender.equalsIgnoreCase("MALE")) this.memberGender = SsmlVoiceGender.MALE;
		else if(memberGender.equalsIgnoreCase("FEMALE")) this.memberGender = SsmlVoiceGender.FEMALE;
		else return false;
		return true;
	}

	public Document toDocument(long id) {
		Document document = new Document();
		document.append("_id", id);
		document.append("lang", memberLang);
		document.append("gender", memberGender.name());
		return document;
	}
	public static MemberSetting fromDocument(Document document) {
		MemberSetting memberSetting = new MemberSetting();
		memberSetting.setMemberLang(document.getString("lang"));
		memberSetting.setMemberGender(document.getString("gender"));
		return memberSetting;
	}
}
