package fr.cnrs.ciham.meditha.meditha.aws;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;

public class TestJson {
	
	@Test
	public void testBasique() throws Exception {
		String json ="{ \"_id\" : \"ee75ac62-3220-47c8-8ee3-e7e02e4f96bb\", \"source\" : { \"bhlIdentifier\" : \"BHL 1485\", \"latinTitle\" : \"Wingardium Leviosa\" } }";
		JSONObject responseBody = new JSONObject();
		
		JSONParser parser = new JSONParser();
		JSONObject jsonObject = (JSONObject) parser.parse(json);
		
		responseBody.put("detail", jsonObject);
		System.out.println(responseBody.toString());
	}

}
