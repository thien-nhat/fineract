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
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.fineract.integrationtests.common.Utils;
import org.apache.fineract.integrationtests.common.system.CodeHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PracticeTask10Test {

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
    public void testCodeAndCodeValuesCRUD() {
        // 1. Create Code
        String codeName = Utils.uniqueRandomStringGenerator("CustomerType_", 5);
        Integer codeId = (Integer) CodeHelper.createCode(this.requestSpec, this.responseSpec, codeName, "resourceId");
        Assertions.assertNotNull(codeId);

        // 2. Add Code Values
        CodeHelper.createCodeValue(this.requestSpec, this.responseSpec, codeId, "VIP", 1);
        CodeHelper.createCodeValue(this.requestSpec, this.responseSpec, codeId, "Standard", 2);

        // 3. Verify Code Values
        ArrayList<HashMap<String, Object>> codeValues = CodeHelper.getAllCodeValuesByCodeId(this.requestSpec, this.responseSpec, codeId);
        Assertions.assertEquals(2, codeValues.size());

        boolean foundVIP = codeValues.stream().anyMatch(cv -> "VIP".equals(cv.get("name")));
        boolean foundStandard = codeValues.stream().anyMatch(cv -> "Standard".equals(cv.get("name")));

        Assertions.assertTrue(foundVIP);
        Assertions.assertTrue(foundStandard);

        // 4. Update Code Value
        Integer vipId = (Integer) codeValues.stream().filter(cv -> "VIP".equals(cv.get("name"))).findFirst().get().get("id");
        CodeHelper.updateCodeValue(this.requestSpec, this.responseSpec, codeId, vipId, "Ultra VIP", 1, "subResourceId");

        // 5. Verify Update
        HashMap<String, Object> updatedVip = (HashMap<String, Object>) CodeHelper.getCodeValueById(this.requestSpec, this.responseSpec, codeId, vipId, "");
        Assertions.assertEquals("Ultra VIP", updatedVip.get("name"));

        // 6. Delete Code Value
        CodeHelper.deleteCodeValueById(this.requestSpec, this.responseSpec, codeId, vipId, "resourceId");

        // 7. Verify Deletion
        codeValues = CodeHelper.getAllCodeValuesByCodeId(this.requestSpec, this.responseSpec, codeId);
        Assertions.assertEquals(1, codeValues.size());
    }
}
