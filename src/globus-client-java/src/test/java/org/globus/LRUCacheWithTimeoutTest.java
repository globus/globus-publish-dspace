/*
 * Copyright 2014 University of Chicago
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.globus;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author pruyne
 *
 */
public class LRUCacheWithTimeoutTest
{

    private String keyValForInt(int val)
    {
        return "Key_" + val;
    }


    @Test
    public void testMaxSize()
    {
        LRUCacheWithTimeout<String, Integer> testMap = new LRUCacheWithTimeout<>();
        testMap.setMaxSize(3);
        for (int i = 0; i < 10; i++) {
            testMap.put(keyValForInt(i), i);
            int size = testMap.size();
            assertTrue(i < 3 && size == i + 1 || i >= 3 && size == 3);
            if (i > 5) {
                Integer pulledVal = testMap.get(keyValForInt(i - 5));
                assertNull(pulledVal);
                Integer intVal = testMap.get(keyValForInt(i - 2));
                if (intVal != null) {
                    assertEquals(i - 2, intVal.intValue());
                }
            }
            System.out.println(i + ": " + testMap);
        }
    }


    @Test
    public void testTimeout() throws InterruptedException
    {
        LRUCacheWithTimeout<String, Integer> testMap = new LRUCacheWithTimeout<>();
        testMap.setMaxLifetime(100); // 0.1 seconds
        testMap.put("TestVal", 1);
        Integer mapVal = testMap.get("TestVal");
        assertNotNull(mapVal);
        Thread.sleep(200);
        mapVal = testMap.get("TestVal");
        assertNull(mapVal);
    }

}
