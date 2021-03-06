/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class AbstractEnumTest {

    @Test
    public void classesOfDifferentTypesAreNotConsideredEqualIfTheyHaveTheSameValue() {
        Region usRegion = Region.region("us");
        Endpoint usEndpoint = Endpoint.endpoint("us");

        assertThat(usEndpoint).isNotEqualTo(usRegion);
    }

    @Test
    public void sameValueSameClassAreSameInstance() {
        Region first = Region.region("first");
        Region alsoFirst = Region.region("first");

        assertThat(first).isSameAs(alsoFirst);
    }

    @Test
    public void canBeUsedAsKeysInMap() {
        Map<Region, String> someMap = new HashMap<>();
        someMap.put(Region.region("key"), "A Value");

        assertThat(someMap.get(Region.region("key"))).isEqualTo("A Value");
    }

    private static class Region extends AbstractEnum {
        private Region(String value) {
            super(value);
        }

        public static Region region(String value) {
            return AbstractEnum.value(value, Region.class, Region::new);
        }
    }

    private static class Endpoint extends AbstractEnum {
        private Endpoint(String value) {
            super(value);
        }

        public static Endpoint endpoint(String value) {
            return AbstractEnum.value(value, Endpoint.class, Endpoint::new);
        }
    }

}