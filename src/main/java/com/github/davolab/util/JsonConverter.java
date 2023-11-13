package com.github.davolab.util;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonConverter {

    public static Map convert(Map map) {
        JSONObject json = new JSONObject(map);
        JSONObject nestedJson = new JSONObject();

        for (String key : json.keySet()) {
            String[] parts = key.split("\\.");

            JSONObject current = nestedJson;
            for (int i = 0; i < parts.length - 1; i++) {
                if (!current.has(parts[i])) {
                    current.put(parts[i], new JSONObject());
                }
                current = current.getJSONObject(parts[i]);
            }
            current.put(parts[parts.length - 1], json.get(key));
        }

        return convertJsonToMap(nestedJson);
    }

    private static Map<String, Object> convertJsonToMap(JSONObject json) {
        Map<String, Object> map = new HashMap<>();

        for (String key : json.keySet()) {
            Object value = json.get(key);

            if (value instanceof JSONObject) {
                value = convertJsonToMap((JSONObject) value);
            } else if (value instanceof JSONArray) {
                value = convertJsonArrayToList((JSONArray) value);
            }

            map.put(key, value);
        }

        return map;
    }

    private static List<Object> convertJsonArrayToList(JSONArray jsonArray) {
        List<Object> list = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            Object item = jsonArray.get(i);
            if (item instanceof JSONObject) {
                list.add(convertJsonToMap((JSONObject) item));
            } else if (item instanceof JSONArray) {
                list.add(convertJsonArrayToList((JSONArray) item));
            } else {
                list.add(item);
            }
        }
        return list;
    }


    public static Map removeHibernateLazyInitializer(Map map) {
        JSONObject json = new JSONObject(map);
        for (String key : json.keySet()) {
            Object value = json.get(key);
            if (value instanceof JSONObject) {
                value = removeHibernateLazyInitializer(convertJsonToMap((JSONObject) value));
                json.put(key, value);
            }
        }
        json.remove("hibernateLazyInitializer");
        return convertJsonToMap(json);
    }
}
