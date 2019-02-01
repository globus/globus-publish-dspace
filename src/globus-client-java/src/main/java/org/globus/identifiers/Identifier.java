/*
 * Copyright 2019 University of Chicago
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
package org.globus.identifiers;

import java.util.Map;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.globus.GlobusEntity;

/**
 * @author pruyne
 *
 */
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class Identifier extends GlobusEntity
{
    public class Checksum {
        @JsonProperty("function")
        public String function;

        @JsonProperty("value")
        public String value;
    }
    
    @JsonProperty("identifier")
    public String identifier;
    
    @JsonProperty("location")
    public String[] location;

    @JsonProperty("landing_page")
    public String landingPage;

    @JsonProperty("admins")
    public String[] admins;

    @JsonProperty("checksums")
    public Checksum[] checksums;

    @JsonProperty("visible_to")
    public String[] visibleTo;

    @JsonProperty("metadata")
    public Map<String, Object> metadata;
}
