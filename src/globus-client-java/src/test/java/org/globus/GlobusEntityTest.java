/** 
 *
 * Copyright 2014-2016 The University of Chicago
 * 
 * All rights reserved.
 */

package org.globus;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

/**
 * @author pruyne
 *
 */
public class GlobusEntityTest {

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}
	
	private Object valueForPathSpec(String path, Object value) {
		if (value == null) {
			return null;
		}
		System.out.println("Evaluating path " + path + " with value " + value);
		if (path.contains("*")) {
			// We want to store this into an Array regardless of value
			String simplePath = path.replace("*", "");
			Collection<Object> valueAsCollection;
			if (value instanceof Collection) {
				valueAsCollection = (Collection) value;
			} else {
				valueAsCollection = Arrays.asList(value);
			}
			JSONArray jsoa = new JSONArray();
			for (Object singleVal : valueAsCollection) {
				jsoa.put(valueForPathSpec(simplePath, singleVal));
			}
			value = jsoa;
		} else if (path.endsWith("#")) {
			// We want to store a long (integer) value
			value = Long.valueOf(value.toString());
		}  else if (path.endsWith("%")) {
			// We want to store a long (integer) value
			value = Double.valueOf(value.toString());
		} else if (path.endsWith("?")) {
			// We want to store as a boolean
			value = Boolean.valueOf(value.toString());
		}
		return value;
	}

	private void myJSONObjectPut(JSONObject jo, String path, Object value) throws JSONException {
		String[] parts = path.split("\\.", 2);
		String[] specChars = new String[] {"*", "#", "%", "?"};
		// Get rid of the type specifier chars from the path
		String parentName = parts[0];
		boolean parentIsArray = parts[0].contains("*");
		for (String specChar : specChars) {
			parentName = parentName.replace(specChar, "");
		}
		if (parts.length > 1) {
			JSONObject child = null;
			// If we don't have this key in the parent, or we're adding to an array
			// Then we have to create a new child to be filled in
			if (parentIsArray || !jo.has(parentName)) {
				child = new JSONObject();
			} else {
				try {
					child = jo.getJSONObject(parentName);
				} catch (JSONException jse) {
					// If the key is already present, but its not an Object, we have problems
					throw new JSONException("Attempting to put object into scalar key " + parts[0]);
				}
			}
			// Now, fill in the child with the value
			myJSONObjectPut(child, parts[1], value);
			value = child;
		} else {
			// Make sure we don't ask it to handle an array
			value = valueForPathSpec(parts[0].replace("*", ""), value);
		}
		if (parentIsArray) {
			JSONArray childArray;
			try {
				childArray = jo.getJSONArray(parentName);
			} catch (JSONException jse) {
				// The array wasn't present, create it and put it into the parent
				childArray = new JSONArray();
				jo.put(parentName, childArray);
			}
			childArray.put(value);
		} else {
			// Not an array, just put it in the parent object
			jo.put(parentName, value);
		}
	}

	@Test
	public void test() {
		JSONObject jo = new JSONObject();
		myJSONObjectPut(jo, "hello*.world#", "100");
		myJSONObjectPut(jo, "hello*.world#", "1000");
		myJSONObjectPut(jo, "hello*", "Scalar1");
		myJSONObjectPut(jo, "hello*", "Scalar2");
		myJSONObjectPut(jo, "hello*%", "55");
		myJSONObjectPut(jo, "hello*", new String[] {"Bob", "Jane"});
		myJSONObjectPut(jo, "hello*?", true);
		myJSONObjectPut(jo, "root.child1.foo", "foo");
		myJSONObjectPut(jo, "root.child1.bar", "bar");
		myJSONObjectPut(jo, "root.child2.bar", "bar2");
		

		

		System.out.println(jo.toString(2));
	}

}
