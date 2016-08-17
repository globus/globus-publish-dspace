/**
 * 
 * Copyright 2016 University of Chicago. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.dspace.globus.configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.emory.mathcs.backport.java.util.Arrays;

/**
 *
 */
public class BasicConfigurablePropertyValueOptions extends
        Configurable.ConfigurablePropertyValueOptions
{

    private Object introspectObject;

    public BasicConfigurablePropertyValueOptions(Object[] values)
    {
        this.introspectObject = values;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.dspace.globus.configuration.Configurable.ConfigurablePropertyValueOptions
     * #getValueOptions()
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<Object> getValueOptions()
    {
        List<Object> valueList = null;
        if (introspectObject instanceof List)
        {
            valueList = (List) introspectObject;
        }
        else if (introspectObject instanceof Collection)
        {
            @SuppressWarnings("rawtypes")
            Collection valueCollection = (Collection) introspectObject;
            valueList = new ArrayList<Object>();
            valueList.addAll(valueCollection);
        }
        else if (introspectObject.getClass().isArray())
        {
            Object[] array = (Object[]) introspectObject;
            valueList = Arrays.asList(array);
        }
        return valueList;
    }

}
