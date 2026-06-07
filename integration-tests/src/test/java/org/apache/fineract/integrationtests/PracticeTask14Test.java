/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.integrationtests;

import java.util.List;
import org.apache.fineract.client.models.GetSearchResponse;
import org.apache.fineract.client.models.PostClientsRequest;
import org.apache.fineract.client.models.PostClientsResponse;
import org.apache.fineract.integrationtests.common.ClientHelper;
import org.apache.fineract.integrationtests.common.SearchHelper;
import org.apache.fineract.integrationtests.common.Utils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PracticeTask14Test {

    @Test
    public void testGlobalSearch() {
        // 1. Create a client with unique name
        String uniqueName = Utils.uniqueRandomStringGenerator("Searchable_", 5);
        PostClientsRequest clientRequest = new PostClientsRequest()
                .officeId(1L)
                .legalFormId(1L)
                .firstname(uniqueName)
                .lastname("Task14")
                .active(true)
                .activationDate("01 June 2026")
                .dateFormat("dd MMMM yyyy")
                .locale("en");
        PostClientsResponse clientResponse = ClientHelper.createClient(clientRequest);
        Assertions.assertNotNull(clientResponse.getClientId());

        // 2. Search for the client
        List<GetSearchResponse> searchResults = SearchHelper.getSearch(uniqueName, true, "clients");
        Assertions.assertFalse(searchResults.isEmpty());
        Assertions.assertEquals(uniqueName + " Task14", searchResults.get(0).getEntityName());
    }
}
