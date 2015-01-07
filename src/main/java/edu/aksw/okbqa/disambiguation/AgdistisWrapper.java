package edu.aksw.okbqa.disambiguation;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Root resource (exposed at "agdistis " path)
 */
@Path("run")
public class AgdistisWrapper {

    public static String SLOTS = "slots";
    public static String VERBALIZATION = "verbalization";
    public static String VAR = "var";
    public static String TYPE = "is";
    public static String RESOURCE = "rdf:Resource";
    public static String CLASS = "rdf:Class";
    public static String PROPERTY = "rdf:Property";
    public static final String CLASS_DICTIONARY = "resources/dbpedia_3Eng_class.ttl";
    public static final String PROPERTY_DICTIONARY = "resources/dbpedia_3Eng_property.ttl";
    public static Lookup classLookup = new Lookup(CLASS_DICTIONARY);
    public static Lookup propertyLookup = new Lookup(PROPERTY_DICTIONARY);

    /**
     * Method handling HTTP GET requests. The returned object will be sent to
     * the client as "text/plain" media type. Assume input in the format
     * specified at
     * https://github.com/okbqa/disambiguation/wiki/IO-Specification
     *
     * @return String that will be returned as a text/plain response.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getIt(@QueryParam("data") String data) {
        if(data.length() <= 1) return "{\"message\": \"This is the disambiguation service based on AGDISTIS\" }";
        Map<String, String> resourceVar2StringMap = new HashMap<>();
        Map<String, String> classVar2StringMap = new HashMap<>();
        Map<String, String> propertyVar2StringMap = new HashMap<>();
        System.out.println("Got input: " + data);
        try {
            JSONParser parser = new JSONParser();
            JSONObject input = (JSONObject) parser.parse(data);
            System.out.println(input);
            if (input.containsKey(SLOTS)) {
                String varLabel;
                JSONArray vars = (JSONArray) input.get(SLOTS);

                //store the type of each variable
                Map<String, String> types = new HashMap<>();
                for (Object var : vars.toArray()) {
                    JSONObject next = (JSONObject) var;
                    if (next.containsKey("s") && next.containsKey("p") && next.containsKey("o")) {
                        if (next.get("p").equals(TYPE)) {
                            types.put(next.get("s").toString(), next.get("o").toString());
                        }
                    }
                }
                for (Object var : vars.toArray()) {
                    JSONObject next = (JSONObject) var;
                    if (next.containsKey("s") && next.containsKey("p") && next.containsKey("o")) {
                        if (next.get("p").equals(VERBALIZATION)) {
                            //send all the resources to AGDISTIS                        
                            varLabel = next.get("s") + "";
                            if (varLabel.startsWith(Pattern.quote("?"))) {
                                varLabel = varLabel.substring(1);
                            }
                            if (types.containsKey(next.get("s").toString())) {
                                if (types.get(next.get("s").toString()).equals(PROPERTY)) {
                                    propertyVar2StringMap.put(varLabel, next.get("o") + "");
                                } else if (types.get(next.get("s").toString()).equals(CLASS)) {
                                    classVar2StringMap.put(varLabel, next.get("o") + "");
                                } else if (types.get(next.get("s").toString()).equals(CLASS)) {
                                    resourceVar2StringMap.put(varLabel, next.get("o") + "");
                                }
                            } //untyped things are assumed to be resources
                            else {
                                resourceVar2StringMap.put(varLabel, next.get("o") + "");
                            }
                        }
                    }
                }
            }
            // run agdistis on resources
            resourceVar2StringMap = AgdistisProcessing.getAgdistisResults(resourceVar2StringMap);
            // run dictionary lookup on properties and classes
            classVar2StringMap = classLookup.lookupVars(classVar2StringMap);
            propertyVar2StringMap = propertyLookup.lookupVars(propertyVar2StringMap);

            JSONObject object = new JSONObject();
            if (input.containsKey("question")) {
                object.put("question", input.get("question"));
            }
            JSONArray resources = getJSONArray(resourceVar2StringMap);
            JSONArray classes = getJSONArray(classVar2StringMap);
            JSONArray properties = getJSONArray(propertyVar2StringMap);
            
            JSONArray ned = new JSONArray();
            JSONObject nedBody = new JSONObject();
            nedBody.put("score", 1);
            nedBody.put("entities", resources);
            //if(!classes.isEmpty())
            nedBody.put("classes", classes);
            //if(!properties.isEmpty())
            nedBody.put("properties", properties);
            ned.add(nedBody);
            object.put("ned", ned);
            return object.toJSONString();
            //System.out.println(object);

        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        
        return "null";
    }

    public JSONArray getJSONArray(Map<String, String> var2String) {
        JSONArray resources = new JSONArray();
        for (String var : var2String.keySet()) {
            JSONObject entry = new JSONObject();
            entry.put("var", var);
            entry.put("value", var2String.get(var));
            entry.put("score", 1);
            resources.add(entry);
        }
        return resources;
    }

    public static void main(String args[]) {
        AgdistisWrapper aw = new AgdistisWrapper();
        String data = "{\"question\":\"Who did that and wo?\",\"slots\" : [ {\"s\" : \"?x\", \"p\" : \"verbalization\", \"o\" : \"Berlin\"},"
                + " {\"s\" : \"?y\", \"p\" : \"verbalization\", \"o\" : \"University of Leipzig\"} ] "
                + "}";
        System.out.println(aw.getIt(data));
    }
}
