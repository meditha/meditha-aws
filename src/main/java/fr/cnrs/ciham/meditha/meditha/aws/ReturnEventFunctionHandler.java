package fr.cnrs.ciham.meditha.meditha.aws;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.bson.Document;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.w3c.dom.stylesheets.DocumentStyle;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Projections;

public class ReturnEventFunctionHandler implements RequestStreamHandler {

	JSONParser parser = new JSONParser();
	@Override
	public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException
	{
		LambdaLogger logger = context.getLogger();
		logger.log("Loading Java Lambda handler of ProxyWithStream");
		
String url = "mongodb+srv://meditaAdmin:medithaAdmin@meditha-ly5tt.mongodb.net/test?retryWrites=true;";
		

		MongoClientURI uri = new MongoClientURI(url);
		String json = "";
		try (MongoClient mongoClient = new MongoClient(uri)) {
		MongoDatabase database = mongoClient.getDatabase("meditha");
		MongoCollection<Document> collection = database.getCollection("metadata");
		FindIterable<Document> documents = collection.find().projection(Projections.include("source.bhlIdentifier"));
		json = StreamSupport.stream(collection.find().spliterator(), false)
		        .map(Document::toJson)
		        .collect(Collectors.joining(", ", "[", "]"));
				
		}
		catch (Exception e) {
			e.printStackTrace();
			
		}
		
		
		
		
		String proxy = null;
		String param1 = null;
		String param2 = null;
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		JSONObject responseJson = new JSONObject();
		String responseCode = "200";
		JSONObject event = null;
		try {
			event = (JSONObject)parser.parse(reader);
			if (event.get("pathParameters") != null) {
				JSONObject pps = (JSONObject)event.get("pathParameters");
				if ( pps.get("proxy") != null) {
					proxy = (String)pps.get("proxy");
				}
			}
			
		}
		catch(Exception pex)
		{
			responseJson.put("statusCode", "400");
			responseJson.put("exception", pex);
		}
		// Implement your logic here
		int output = 0;
		
		JSONObject responseBody = new JSONObject();
		responseBody.put("input", event.toJSONString());
		responseBody.put("detail", json);
		JSONObject headerJson = new JSONObject();
		headerJson.put("x-custom-header", "my custom header value");
		headerJson.put("Access-Control-Allow-Origin", "*");
		responseJson.put("isBase64Encoded", false);
		responseJson.put("statusCode", responseCode);
		responseJson.put("headers", headerJson);
		responseJson.put("body", responseBody.toString());  
		OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
		writer.write(responseJson.toJSONString());  
		writer.close();
	}
	public int sum(int a, int b)
	{
		return a+b;
	}
	public int subtract(int a, int b)
	{
		return a-b;
	}
}

