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
import org.apache.fineract.integrationtests.common.loans.LoanProductHelper;
import org.apache.fineract.integrationtests.common.loans.LoanStatusChecker;
import org.apache.fineract.integrationtests.common.loans.LoanTransactionHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PracticeTask8Test {

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
    public void testLoanTransactions() {
        final LoanTransactionHelper loanTransactionHelper = new LoanTransactionHelper(this.requestSpec, this.responseSpec);
        final LoanProductHelper loanProductHelper = new LoanProductHelper();

        // 1. Set Business Date
        BusinessDateHelper.updateBusinessDate(this.requestSpec, this.responseSpec, "BUSINESS_DATE", "05 June 2026");

        // 2. Create Active Client
        PostClientsRequest clientRequest = new PostClientsRequest()
                .officeId(1L)
                .legalFormId(1L)
                .firstname("Task8")
                .lastname("TransactionsClient")
                .active(true)
                .activationDate("05 June 2026")
                .dateFormat("dd MMMM yyyy")
                .locale("en");
        PostClientsResponse clientResponse = ClientHelper.createClient(clientRequest);
        Integer clientId = clientResponse.getClientId().intValue();

        // 3. Create Loan Product
        String loanProductJSON = loanProductHelper.withPrincipal("1000")
                .withInterestTypeAsFlat()
                .withAccountingRuleAsCashBased()
                .build(null);
        Integer productId = loanProductHelper.createLoanProduct(loanProductJSON);

        // 4. Create Loan A and Disburse
        Integer loanAId = createAndDisburseLoan(clientId, productId, "1000", "05 June 2026");

        // 5. Repayment on Loan A
        loanTransactionHelper.makeLoanRepayment("05 June 2026", "200", loanAId);

        // 6. Verify Repayment
        HashMap loanSummary = loanTransactionHelper.getLoanSummary(loanAId);
        Assertions.assertEquals(200.0f, loanSummary.get("totalRepayment"));

        // 7. Create Loan B and Disburse
        Integer loanBId = createAndDisburseLoan(clientId, productId, "800", "05 June 2026");

        // 8. Write-off on Loan B
        loanTransactionHelper.writeOffLoan("05 June 2026", loanBId);

        // 9. Verify Status
        HashMap loanStatus = loanTransactionHelper.getLoanStatus(loanBId);
        LoanStatusChecker.verifyLoanAccountIsClosed(loanStatus);
        LoanStatusChecker.verifyLoanAccountIsWrittenOff(loanStatus);
    }

    private Integer createAndDisburseLoan(Integer clientId, Integer productId, String principal, String date) {
        final LoanTransactionHelper loanTransactionHelper = new LoanTransactionHelper(this.requestSpec, this.responseSpec);
        String loanApplicationJSON = loanTransactionHelper.withPrincipal(principal)
                .withExpectedDisbursementDate(date)
                .withSubmittedOnDate(date)
                .build(clientId.toString(), productId.toString(), null);
        Integer loanId = loanTransactionHelper.getLoanId(loanApplicationJSON);
        loanTransactionHelper.approveLoan(date, loanId);
        loanTransactionHelper.disburseLoanWithNetDisversalAmount(date, loanId, principal);
        return loanId;
    }
}
