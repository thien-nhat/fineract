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

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.apache.fineract.client.models.GetClientsClientIdResponse;
import org.apache.fineract.client.models.PostClientsRequest;
import org.apache.fineract.client.models.PostClientsResponse;
import org.apache.fineract.integrationtests.common.ClientHelper;
import org.apache.fineract.integrationtests.common.Utils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PracticeTask2Test {

    private ResponseSpecification responseSpec;
    private RequestSpecification requestSpec;

    @BeforeEach
    public void setup() {
        Utils.initializeRESTAssured();
        this.requestSpec = new RequestSpecBuilder().setContentType(ContentType.JSON).build();
        this.requestSpec.header("Authorization", "Basic " + Utils.loginIntoServerAndGetBase64EncodedAuthenticationKey());
        this.requestSpec.header("Fineract-Platform-TenantId", "default");
        this.responseSpec = new ResponseSpecBuilder().expectStatusCode(200).build();
    }

    @Test
    public void testClientLifecycle() {
        final ClientHelper clientHelper = new ClientHelper(this.requestSpec, this.responseSpec);

        // 1. Create Pending Client
        PostClientsRequest request = new PostClientsRequest()
                .officeId(1L)
                .legalFormId(1L)
                .firstname("Task2")
                .lastname("PendingClient")
                .active(false)
                .submittedOnDate("01 June 2026")
                .dateFormat("dd MMMM yyyy")
                .locale("en");
        
        PostClientsResponse response = ClientHelper.createClient(request);
        Long clientId = response.getClientId();
        Assertions.assertNotNull(clientId);

        // 2. Verify status is Pending (100)
        GetClientsClientIdResponse client = ClientHelper.getClient(this.requestSpec, this.responseSpec, clientId.intValue());
        Assertions.assertEquals(100, client.getStatus().getId());

        // 3. Activate Client
        ClientHelper.activateClient(client.getExternalId(), "02 June 2026");

        // 4. Verify status is Active (300)
        client = ClientHelper.getClient(this.requestSpec, this.responseSpec, clientId.intValue());
        Assertions.assertEquals(300, client.getStatus().getId());
    }
}
