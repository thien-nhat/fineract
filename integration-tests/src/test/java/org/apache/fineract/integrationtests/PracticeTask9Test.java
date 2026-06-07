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
import java.util.Map;
import org.apache.fineract.client.models.PostClientsRequest;
import org.apache.fineract.client.models.PostClientsResponse;
import org.apache.fineract.integrationtests.common.ClientHelper;
import org.apache.fineract.integrationtests.common.Utils;
import org.apache.fineract.integrationtests.common.system.DatatableHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PracticeTask9Test {

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
    public void testDatatableCRUD() {
        final DatatableHelper datatableHelper = new DatatableHelper(this.requestSpec, this.responseSpec);

        // 1. Create Datatable
        String datatableName = Utils.uniqueRandomStringGenerator("dt_", 5).toLowerCase();
        String json = DatatableHelper.getTestDatatableAsJSON("m_client", datatableName, null, false);
        datatableHelper.createDatatable(json);

        // 2. Create Client
        PostClientsRequest clientRequest = new PostClientsRequest()
                .officeId(1L)
                .legalFormId(1L)
                .firstname("Task9")
                .lastname("DTClient")
                .active(true)
                .activationDate("01 June 2026")
                .dateFormat("dd MMMM yyyy")
                .locale("en");
        PostClientsResponse clientResponse = ClientHelper.createClient(clientRequest);
        Integer clientId = clientResponse.getClientId().intValue();

        // 3. Add Entry
        Map<String, Object> data = new HashMap<>();
        data.put("Spouse Name", "Jane Doe");
        data.put("Number of Dependents", 2);
        data.put("locale", "en");
        data.put("dateFormat", "dd MMMM yyyy");
        data.put("Date of Approval", "01 June 2026");
        
        datatableHelper.createDatatableEntry(datatableName, clientId, false, Utils.convertHashMapToJSON(data));

        // 4. Read Entry
        String entry = datatableHelper.readDatatableEntry(datatableName, clientId, false);
        Assertions.assertTrue(entry.contains("Jane Doe"));

        // 5. Update Entry
        data.put("Spouse Name", "Janet Doe");
        datatableHelper.updateDatatableEntry(datatableName, clientId, false, Utils.convertHashMapToJSON(data));

        // 6. Verify Update
        entry = datatableHelper.readDatatableEntry(datatableName, clientId, false);
        Assertions.assertTrue(entry.contains("Janet Doe"));

        // 7. Delete Entry
        datatableHelper.deleteDatatableEntries(datatableName, clientId, "resourceIdentifier");

        // 8. Delete Datatable
        datatableHelper.deleteDatatable(datatableName);
    }
}
