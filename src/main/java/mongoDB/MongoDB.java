package mongoDB;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class MongoDB {

	public static void main(String[] args) {
		ConnectionString connectionString = new ConnectionString("mongodb+srv://<ttsbot>:<R9hyjSyp8RVC5ya3>@cluster0.y0fxf.mongodb.net/Cluster0?retryWrites=true&w=majority");
//		MongoClientSettings settings = MongoClientSettings.builder()
//		        .applyConnectionString(connectionString)
//		        .serverApi(ServerApi.builder()
//		            .version(ServerApiVersion.V1)
//		            .build())
//		        .build();
		MongoClient mongoClient = MongoClients.create();
		MongoDatabase database = mongoClient.getDatabase("test");
	}

}
