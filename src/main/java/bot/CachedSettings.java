package bot;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

public class CachedSettings {

	public static void main(String[] args) {
		MongoClient client = MongoClients.create("<<MongoDB URI>>");
	}

}
