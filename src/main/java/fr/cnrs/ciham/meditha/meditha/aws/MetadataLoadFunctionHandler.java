package fr.cnrs.ciham.meditha.meditha.aws;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.http.HttpStatus;
import org.bson.Document;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.w3c.dom.stylesheets.DocumentStyle;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Projections;

public class MetadataLoadFunctionHandler implements RequestStreamHandler, Constants {

	JSONParser parser = new JSONParser();
	@Override
	public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException
	{

		MongoClientURI uri = new MongoClientURI(URL);
		String json = "";
		JSONObject responseJson = new JSONObject();
		String responseCode = ""+HttpStatus.SC_OK;
		JSONObject responseBody = new JSONObject();


		try (MongoClient mongoClient = new MongoClient(uri)) {

			String uuid = "";
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
			JSONObject event = (JSONObject) parser.parse(reader);
			if (event.get("pathParameters") != null) {
				JSONObject pathParameters = (JSONObject)event.get("pathParameters");
				if ( pathParameters.get("uuid") != null) {
					  uuid = (String) pathParameters.get("uuid");
				}
			}	

			MongoDatabase database = mongoClient.getDatabase(MONGO_DATABASE);
			MongoCollection<Document> collection = database.getCollection(MONGO_METADATA_COLLECTION);
			BasicDBObject query = new BasicDBObject();
		    query.put("_id", uuid);

		    FindIterable<Document> find = collection.find(query);
		    Document first = find.first();
		    
		    if (first == null) {
		    	responseJson.put("statusCode", HttpStatus.SC_NOT_FOUND);
				responseJson.put("exception", "No record corresponding to this uuid");
		    }
		    else {
		    	json = first.toJson();
		    	JSONParser parser = new JSONParser();
				JSONObject jsonObject = (JSONObject) parser.parse(json);
		    	responseBody.put("detail", jsonObject);
		    }

		}
		catch (Exception e) {
			responseJson.put("statusCode", HttpStatus.SC_INTERNAL_SERVER_ERROR);
			responseJson.put("exception", e);
		}

		
		
		JSONObject headerJson = new JSONObject();
		headerJson.put("Access-Control-Allow-Origin", "*");
		responseJson.put("isBase64Encoded", false);
		responseJson.put("statusCode", responseCode);
		responseJson.put("headers", headerJson);
		responseJson.put("body", responseBody.toString());  
		OutputStreamWriter writer = new OutputStreamWriter(outputStream, Charset.defaultCharset());
		writer.write(responseJson.toJSONString());  
		writer.close();
	}
}

