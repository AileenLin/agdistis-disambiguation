/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.aksw.okbqa.disambiguation;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author ngonga
 */
public class AgdistisProcessing {
    public static String SERVICEURL = "http://139.18.2.164:8080/AGDISTIS";
    public static Map<String, String> getAgdistisResults(Map<String, String> varStringMap) {
        String t = "";
        Map<String, String> keyVarMap = new HashMap<>();
        Map<String, String> result = new HashMap<>();            
        for (String v : varStringMap.keySet()) {
            t = t + " <entity>" + varStringMap.get(v) + "</entity>";
            keyVarMap.put(varStringMap.get(v), v);
        }
        try {
            HashMap<String, String> disambiguation = runDisambiguation(t);
            for (String label : disambiguation.keySet()) {
                result.put(keyVarMap.get(label), disambiguation.get(label));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static HashMap<String, String> runDisambiguation(String inputText) throws ParseException, IOException, org.json.simple.parser.ParseException {
        String urlParameters = "text=" + URLEncoder.encode(inputText, "UTF-8");
        urlParameters += "&type=agdistis";

        //change this URL to http://139.18.2.164:8080/AGDISTIS_ZH to use chinese endpoint
        URL url = new URL(SERVICEURL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setUseCaches(false);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setRequestProperty("Content-Length", String.valueOf(urlParameters.length()));

        DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
        wr.writeBytes(urlParameters);
        wr.flush();

        InputStream inputStream = connection.getInputStream();
        InputStreamReader in = new InputStreamReader(inputStream);
        BufferedReader reader = new BufferedReader(in);

        StringBuilder sb = new StringBuilder();
        while (reader.ready()) {
            sb.append(reader.readLine());
        }

        wr.close();
        reader.close();
        connection.disconnect();

        String agdistis = sb.toString();

        JSONParser parser = new JSONParser();
        JSONArray resources = (JSONArray) parser.parse(agdistis);

        HashMap<String, String> tmp = new HashMap<String, String>();
        for (Object res : resources.toArray()) {
            JSONObject next = (JSONObject) res;
            String namedEntity = (String) next.get("namedEntity");
            String disambiguatedURL = (String) next.get("disambiguatedURL");
            tmp.put(namedEntity, disambiguatedURL);
        }
        return tmp;
    }
    
    
    public static void main(String args[])
    {
        Map<String, String> map = new HashMap<>();
        map.put("x","Jack Nicholson");
        map.put("y","Metallica");
        System.out.println(AgdistisProcessing.getAgdistisResults(map));
    }
}
