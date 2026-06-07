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

import java.time.LocalDate;
import java.util.List;
import org.apache.fineract.client.models.GetOfficesResponse;
import org.apache.fineract.integrationtests.common.OfficeHelper;
import org.apache.fineract.integrationtests.common.Utils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PracticeTask1Test {

    private final OfficeHelper officeHelper = new OfficeHelper();

    @Test
    public void testOfficeHierarchy() {
        // Create Branch A under Head Office (ID 1)
        String branchAName = Utils.uniqueRandomStringGenerator("Branch_A_", 4);
        Long branchAId = officeHelper.createOffice(branchAName, LocalDate.of(2026, 6, 1), 1L).getResourceId();
        Assertions.assertNotNull(branchAId);

        // Create Sub-branch A.1 under Branch A
        String subBranchA1Name = Utils.uniqueRandomStringGenerator("Sub_Branch_A1_", 4);
        Long subBranchA1Id = officeHelper.createOffice(subBranchA1Name, LocalDate.of(2026, 6, 1), branchAId).getResourceId();
        Assertions.assertNotNull(subBranchA1Id);

        // Create Sub-branch A.2 under Branch A
        String subBranchA2Name = Utils.uniqueRandomStringGenerator("Sub_Branch_A2_", 4);
        Long subBranchA2Id = officeHelper.createOffice(subBranchA2Name, LocalDate.of(2026, 6, 1), branchAId).getResourceId();
        Assertions.assertNotNull(subBranchA2Id);

        // Verify hierarchy
        GetOfficesResponse subBranchA1 = officeHelper.retrieveOffice(subBranchA1Id);
        Assertions.assertEquals(branchAId, subBranchA1.getParentId());

        GetOfficesResponse subBranchA2 = officeHelper.retrieveOffice(subBranchA2Id);
        Assertions.assertEquals(branchAId, subBranchA2.getParentId());

        // List offices and check if they exist
        List<GetOfficesResponse> offices = officeHelper.getAllOffices();
        boolean foundA = offices.stream().anyMatch(o -> o.getId().equals(branchAId));
        boolean foundA1 = offices.stream().anyMatch(o -> o.getId().equals(subBranchA1Id));
        boolean foundA2 = offices.stream().anyMatch(o -> o.getId().equals(subBranchA2Id));

        Assertions.assertTrue(foundA);
        Assertions.assertTrue(foundA1);
        Assertions.assertTrue(foundA2);
    }
}
