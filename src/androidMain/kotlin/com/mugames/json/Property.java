/*
 *    Copyright (c) 2023 Udhayarajan M
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.mugames.json;

/*
Public Domain.
*/

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Enumeration;
import java.util.Properties;

/**
 * Converts a Property file data into JSONObject and back.
 * @author JSON.org
 * @version 2015-05-05
 */
public class Property {
    /**
     * Converts a property file object into a JSONObject. The property file object is a table of name value pairs.
     * @param properties java.util.Properties
     * @return JSONObject
     * @throws JSONException if a called function has an error
     */
    public static JSONObject toJSONObject(Properties properties) throws JSONException {
        // can't use the new constructor for Android support
        // JSONObject jo = new JSONObject(properties == null ? 0 : properties.size());
        JSONObject jo = new JSONObject();
        if (properties != null && !properties.isEmpty()) {
            Enumeration<?> enumProperties = properties.propertyNames();
            while(enumProperties.hasMoreElements()) {
                String name = (String)enumProperties.nextElement();
                jo.put(name, properties.getProperty(name));
            }
        }
        return jo;
    }

    /**
     * Converts the JSONObject into a property file object.
     * @param jo JSONObject
     * @return java.util.Properties
     * @throws JSONException if a called function has an error
     */
//    public static Properties toProperties(JSONObject jo)  throws JSONException {
//        Properties  properties = new Properties();
//        if (jo != null) {
//        	// Don't use the new entrySet API to maintain Android support
//            for (final String key : jo.keySet()) {
//                Object value = jo.opt(key);
//                if (!JSONObject.NULL.equals(value)) {
//                    properties.put(key, value.toString());
//                }
//            }
//        }
//        return properties;
//    }
}
