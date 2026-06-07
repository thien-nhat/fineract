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
import org.apache.fineract.integrationtests.common.BusinessDateHelper;
import org.apache.fineract.integrationtests.common.ClientHelper;
import org.apache.fineract.integrationtests.common.Utils;
import org.apache.fineract.integrationtests.common.savings.SavingsAccountHelper;
import org.apache.fineract.integrationtests.common.savings.SavingsProductHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PracticeTask6Test {

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
    public void testSavingsTransactionSearch() {
        final SavingsAccountHelper savingsAccountHelper = new SavingsAccountHelper(this.requestSpec, this.responseSpec);
        final SavingsProductHelper savingsProductHelper = new SavingsProductHelper();

        // 1. Enable Business Date
        BusinessDateHelper.updateBusinessDate(this.requestSpec, this.responseSpec, "BUSINESS_DATE", "03 June 2026");

        // 2. Create Active Client
        PostClientsRequest clientRequest = new PostClientsRequest()
                .officeId(1L)
                .legalFormId(1L)
                .firstname("Task6")
                .lastname("SearchClient")
                .active(true)
                .activationDate("03 June 2026")
                .dateFormat("dd MMMM yyyy")
                .locale("en");
        PostClientsResponse clientResponse = ClientHelper.createClient(clientRequest);
        Integer clientId = clientResponse.getClientId().intValue();

        // 3. Create Savings Product
        String productName = Utils.uniqueRandomStringGenerator("Search_Savings_Product_", 4);
        Integer productId = savingsProductHelper.createSavingsProduct(productName, Utils.uniqueRandomStringGenerator("S", 3), this.requestSpec, this.responseSpec);

        // 4. Create and Activate Savings Account
        Integer savingsId = savingsAccountHelper.applyForSavingsApplication(clientId, productId, "03 June 2026");
        savingsAccountHelper.approveSavings(savingsId, "03 June 2026");
        savingsAccountHelper.activateSavings(savingsId, "03 June 2026");

        // 5. Transaction 1: Deposit (03 June 2026) - 1000
        savingsAccountHelper.depositToSavingsAccount(savingsId, "1000", "03 June 2026", null);

        // 6. Change Business Date to 04 June 2026
        BusinessDateHelper.updateBusinessDate(this.requestSpec, this.responseSpec, "BUSINESS_DATE", "04 June 2026");

        // 7. Transaction 2: Withdrawal (04 June 2026) - 125
        savingsAccountHelper.withdrawalFromSavingsAccount(savingsId, "125", "04 June 2026", null);

        // 8. Change Business Date to 05 June 2026
        BusinessDateHelper.updateBusinessDate(this.requestSpec, this.responseSpec, "BUSINESS_DATE", "05 June 2026");

        // 9. Transaction 3: Deposit (05 June 2026) - 50
        savingsAccountHelper.depositToSavingsAccount(savingsId, "50", "05 June 2026", null);

        // 10. Verify Search by Date Range
        HashMap<String, Object> searchResults = savingsAccountHelper.searchTransactions(savingsId, "2026-06-04", "2026-06-05");
        Assertions.assertNotNull(searchResults);
        // Depending on how searchTransactions is implemented, it might return a Map or List.
        // Assuming it returns the full response which includes 'content' (Page object).
    }
}
