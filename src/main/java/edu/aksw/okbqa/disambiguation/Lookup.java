/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.aksw.okbqa.disambiguation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Implements trigram-based lookup for dictionaries
 * @author ngonga
 */
public class Lookup {

    private final static Logger LOGGER = Logger.getLogger(Main.class.getName());
    private Map<String, Set<String>> dictionary;
    private boolean doneInit = false;
    public static String SPLIT = " rdfs:label ";
    public static double THRESHOLD = 0.2;
    public Lookup(String file)
    {
        init(file);
    }
    //inits in case init as not yet been carried out    
    public void init(String file) {
        if (!doneInit) {
            reInit(file);
            doneInit = true;
        }
    }

    //forces init
    public void reInit(String file) {
        //assume tab-separated file
        try {
            dictionary = new HashMap<>();
            BufferedReader in = new BufferedReader(new FileReader(new File(file)));
            String s = in.readLine();
            while (s != null) {
                String split[] = s.split(SPLIT);
                if (split.length >= 2) {
                    for (int i = 1; i < split.length; i++) {
                        Set<String> trigrams = getTrigrams(clean(split[i]));
                        for (String t : trigrams) {
                            if (!dictionary.containsKey(t)) {
                                dictionary.put(t, new HashSet<String>());
                            }
                            dictionary.get(t).add(split[0]);
                        }
                    }
                }
                s = in.readLine();
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error reading file {0}", file);
            LOGGER.log(Level.WARNING, "Error message: {0}", e.getMessage());
        }
    }

    // returns all candidates with score
    public Map<String, Double> lookup(String s) {
        Set<String> trigrams = getTrigrams(s);
        Map<String, Double> count = new HashMap<>();
        for(String t: trigrams)
        {
            if(dictionary.containsKey(t))
            {
                Set<String> resources = dictionary.get(t);
                for(String r: resources)
                {
                    if(!count.containsKey(r))
                        count.put(r, 1d);
                    else
                        count.put(r, count.get(r)+1d);
                }
            }
        }
        Set<String> toRemove = new HashSet<String>();
        for(String k: count.keySet())
        {
            double score = 2*count.get(k)/(k.length() + s.length());
            if(score >= THRESHOLD)
            count.put(k, score);
            else toRemove.add(k);
        }
        
        for(String k:toRemove)
            count.remove(k);
        
        return count;
    }

    public Map<String, String> lookupVars (Map<String, String> vars)
    {
        Map<String, String> result = new HashMap<>();
        for(String var: vars.keySet())
            result.put(var, lookupBest(vars.get(var)).keySet().iterator().next());
        return result;
    }
    
    public Map<String, Map<String, Double>> lookupVarsWithScore (Map<String, String> vars)
    {
        Map<String, Map<String, Double>> result = new HashMap<>();
        for(String var: vars.keySet())
            result.put(var, lookup(vars.get(var)));
        return result;
    }
    //returns best match with score
    public Map<String, Double> lookupBest(String s)
    {
        Map<String, Double> matches = lookup(s);
        double max = 0d;
        String best = null;
        for(String r: matches.keySet())
        {
            if(matches.get(r) > max)
            {
                max = matches.get(r);
             best = r;   
            }
        }
        Map<String, Double> result = new HashMap<>();
        result.put(best, max);
        return result;
    }
    
    //commodity. Compute all trigrams for a given string
    public static Set<String> getTrigrams(String s) {
        
        Set<String> result = new HashSet<>();
        String t = "  " + clean(s);
        for (int i = 0; i < t.length() - 3; i++) {
            result.add(t.substring(i, i + 3));
        }
        return result;
    }

    public static void main(String args[]) {
        Lookup l = new Lookup("resources/dbpedia_3Eng_class.ttl");
        System.out.println(l.lookup("football"));
    }

    private static String clean(String string) {       
        return string.replaceAll("[^A-Za-z0-9 ]", "").toLowerCase();
    }
}
