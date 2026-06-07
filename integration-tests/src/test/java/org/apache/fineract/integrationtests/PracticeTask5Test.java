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
import java.util.HashMap;
import org.apache.fineract.client.models.PostClientsRequest;
import org.apache.fineract.client.models.PostClientsResponse;
import org.apache.fineract.integrationtests.common.ClientHelper;
import org.apache.fineract.integrationtests.common.Utils;
import org.apache.fineract.integrationtests.common.savings.SavingsAccountHelper;
import org.apache.fineract.integrationtests.common.savings.SavingsProductHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PracticeTask5Test {

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
    public void testSavingsAccountLifecycle() {
        final SavingsAccountHelper savingsAccountHelper = new SavingsAccountHelper(this.requestSpec, this.responseSpec);
        final SavingsProductHelper savingsProductHelper = new SavingsProductHelper();

        // 1. Create Active Client
        PostClientsRequest clientRequest = new PostClientsRequest()
                .officeId(1L)
                .legalFormId(1L)
                .firstname("Task5")
                .lastname("SavingsClient")
                .active(true)
                .activationDate("01 June 2026")
                .dateFormat("dd MMMM yyyy")
                .locale("en");
        PostClientsResponse clientResponse = ClientHelper.createClient(clientRequest);
        Integer clientId = clientResponse.getClientId().intValue();

        // 2. Create Savings Product
        String productName = Utils.uniqueRandomStringGenerator("Savings_Product_", 4);
        Integer productId = savingsProductHelper.createSavingsProduct(productName, Utils.uniqueRandomStringGenerator("S", 3), this.requestSpec, this.responseSpec);

        // 3. Submit Savings Account Application
        Integer savingsId = savingsAccountHelper.applyForSavingsApplication(clientId, productId, "02 June 2026");
        Assertions.assertNotNull(savingsId);

        // 4. Approve
        savingsAccountHelper.approveSavings(savingsId, "03 June 2026");

        // 5. Activate
        savingsAccountHelper.activateSavings(savingsId, "03 June 2026");

        // 6. Verify Status
        HashMap<String, Object> savingsStatus = savingsAccountHelper.getSavingsAccountStatus(savingsId);
        Assertions.assertEquals(300, savingsStatus.get("id"));
    }
}
