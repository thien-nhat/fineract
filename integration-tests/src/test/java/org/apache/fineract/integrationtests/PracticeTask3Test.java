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
import org.apache.fineract.client.models.PostClientsRequest;
import org.apache.fineract.client.models.PostClientsResponse;
import org.apache.fineract.integrationtests.common.ClientHelper;
import org.apache.fineract.integrationtests.common.NotesHelper;
import org.apache.fineract.integrationtests.common.Utils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PracticeTask3Test {

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
    public void testClientNotesCRUD() {
        // 1. Create Client
        PostClientsRequest clientRequest = new PostClientsRequest()
                .officeId(1L)
                .legalFormId(1L)
                .firstname("Task3")
                .lastname("NoteClient")
                .active(true)
                .activationDate("01 June 2026")
                .dateFormat("dd MMMM yyyy")
                .locale("en");
        
        PostClientsResponse clientResponse = ClientHelper.createClient(clientRequest);
        Integer clientId = clientResponse.getClientId().intValue();

        // 2. Create Note
        String noteText = "Initial Note Content";
        String notePayload = "{\"note\":\"" + noteText + "\"}";
        Integer noteId = NotesHelper.createClientNote(this.requestSpec, this.responseSpec, clientId, notePayload);
        Assertions.assertNotNull(noteId);

        // 3. Retrieve Note
        String retrievedNote = NotesHelper.getClientNote(this.requestSpec, this.responseSpec, clientId, noteId);
        Assertions.assertEquals(noteText, retrievedNote);

        // 4. Update Note
        String updatedNoteText = "Updated Note Content";
        String updatePayload = "{\"note\":\"" + updatedNoteText + "\"}";
        NotesHelper.updateClientNote(this.requestSpec, this.responseSpec, clientId, noteId, updatePayload);

        // 5. Verify Update
        retrievedNote = NotesHelper.getClientNote(this.requestSpec, this.responseSpec, clientId, noteId);
        Assertions.assertEquals(updatedNoteText, retrievedNote);

        // 6. Delete Note
        NotesHelper.deleteClientNote(this.requestSpec, this.responseSpec, clientId, noteId);

        // 7. Verify Deletion (expecting 404 or empty note, Fineract usually returns 404 if retrieved directly)
        // Note: NotesHelper.getClientNote might fail if the status code is not 200.
        ResponseSpecification responseSpec404 = new ResponseSpecBuilder().expectStatusCode(404).build();
        Utils.performServerGet(this.requestSpec, responseSpec404, "/fineract-provider/api/v1/clients/" + clientId + "/notes/" + noteId + "?" + Utils.TENANT_IDENTIFIER);
    }
}
