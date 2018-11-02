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
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Sorts;

public class ReferenceManuscriptListFunctionHandler implements RequestStreamHandler, Constants {

	JSONParser parser = new JSONParser();
	@Override
	public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException
	{
		MongoClientURI uri = new MongoClientURI(URL);
		String json = "";
		JSONObject responseJson = new JSONObject();
		String responseCode = "200";
		responseJson.put("statusCode", responseCode);
		JSONObject responseBody = new JSONObject();
		try (MongoClient mongoClient = new MongoClient(uri)) {
			int pageNumber = 1;
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
			JSONObject event = (JSONObject) parser.parse(reader);
			
			if (event.get("queryStringParameters") != null) {
				JSONObject parameters = (JSONObject)event.get("queryStringParameters");
				if ( parameters.get("pageNumber") != null) {
					pageNumber = Integer.parseInt((String) parameters.get("pageNumber"));
				}
			}		

			MongoDatabase database = mongoClient.getDatabase(MONGO_DATABASE);
			MongoCollection<Document> collection = database.getCollection(MONGO_REFERENCE_MANUSCRIPT_COLLECTION);

			int pageSize = 10;
			long hits = collection.countDocuments();
			FindIterable<Document> documents = collection.find().skip(pageSize*(pageNumber-1)).limit(pageSize).sort(Sorts.orderBy(Sorts.ascending("cote")));
			json = StreamSupport.stream(documents.spliterator(), false)
					.map(Document::toJson)
					.collect(Collectors.joining(", ", "[", "]"));

			JSONParser parser = new JSONParser();
			JSONArray jsonObject = (JSONArray) parser.parse(json);
			responseBody.put("detail", jsonObject);
			responseBody.put("hits", hits);
			responseBody.put("pageNumber", pageNumber);
			responseBody.put("pageSize", pageSize);

		}
		catch (Exception e) {
			responseJson.put("statusCode", "500");
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

