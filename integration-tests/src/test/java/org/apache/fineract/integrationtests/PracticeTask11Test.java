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
import org.apache.fineract.integrationtests.common.HookHelper;
import org.apache.fineract.integrationtests.common.Utils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PracticeTask11Test {

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
    public void testHookCRUD() {
        final HookHelper hookHelper = new HookHelper(this.requestSpec, this.responseSpec);

        // 1. Create Hook
        String payloadURL = "https://webhook.site/dummy-url";
        Integer hookId = hookHelper.createHook(payloadURL);
        Assertions.assertNotNull(hookId);

        // 2. Verify
        hookHelper.verifyHookCreatedOnServer(Long.valueOf(hookId));

        // 3. Update Hook
        String updatedURL = "https://webhook.site/updated-url";
        hookHelper.updateHook(updatedURL, Long.valueOf(hookId));

        // 4. Delete Hook
        hookHelper.deleteHook(Long.valueOf(hookId));

        // 5. Verify Deletion
        hookHelper.verifyDeleteHook(Long.valueOf(hookId));
    }
}
