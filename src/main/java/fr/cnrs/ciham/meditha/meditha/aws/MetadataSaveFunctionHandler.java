package fr.cnrs.ciham.meditha.meditha.aws;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpStatus;
import org.bson.Document;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;

public class MetadataSaveFunctionHandler implements RequestStreamHandler, Constants {

	JSONParser parser = new JSONParser();
	@Override
	public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException
	{
		boolean result = false;
		MongoClientURI uri = new MongoClientURI(URL);
		String json = "";
		JSONObject responseJson = new JSONObject();
		String responseCode = ""+HttpStatus.SC_OK;
		JSONObject responseBody = new JSONObject();
		String message="";
		LambdaLogger logger = context.getLogger();
		logger.log("Sauvegarde d'une fiche");

		String rawBody="";

		try (MongoClient mongoClient = new MongoClient(uri)) {

			String uuid = "";
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
			JSONObject event = (JSONObject) parser.parse(reader);
			if (event.get("pathParameters") != null) {
				JSONObject pathParameters = (JSONObject)event.get("pathParameters");
				if ( pathParameters.get("uuid") != null) {
					uuid = (String) pathParameters.get("uuid");
					logger.log("uuid "+uuid);
				}
				else {
					responseCode = ""+HttpStatus.SC_BAD_REQUEST;
					message ="Missing parameter uuid";
				}
				JSONObject body = (JSONObject)parser.parse((String)event.get("body"));
				if ( body.get("detail") != null) {
					rawBody = (String) body.get("detail").toString();
					logger.log("detail "+rawBody);
				}
				else {
					responseCode = ""+HttpStatus.SC_BAD_REQUEST;
					message ="Missing parameter detail in post content";
				}


			}	

			MongoDatabase database = mongoClient.getDatabase(MONGO_DATABASE);
			MongoCollection<Document> collection = database.getCollection(MONGO_METADATA_COLLECTION);



			BasicDBObject filter = new BasicDBObject();
			filter.put("_id", uuid);
			DeleteResult deleteOne = collection.deleteOne(filter);
			collection.insertOne(Document.parse(rawBody));
			responseJson.put("statusCode", HttpStatus.SC_OK);
			message = "success";
			result = true;


		}
		catch (Exception e) {
			responseJson.put("statusCode", HttpStatus.SC_INTERNAL_SERVER_ERROR);
			responseJson.put("exception", e);
			message =e.getMessage()+" xxx:"+rawBody;
			logger.log(ExceptionUtils.getStackTrace(e));
			
		}



		JSONObject headerJson = new JSONObject();
		headerJson.put("Access-Control-Allow-Origin", "*");
		responseJson.put("isBase64Encoded", false);
		responseJson.put("statusCode", responseCode);
		responseJson.put("headers", headerJson);
		if (result) {
			responseBody.put("result", "0K");
		}
		else {
			responseBody.put("result", "KO: "+message);
		}
		responseJson.put("body", responseBody.toString());  
		OutputStreamWriter writer = new OutputStreamWriter(outputStream, Charset.defaultCharset());
		writer.write(responseJson.toJSONString());  
		writer.close();
	}
}

