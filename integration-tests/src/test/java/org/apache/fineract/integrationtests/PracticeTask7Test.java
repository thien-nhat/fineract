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
import org.apache.fineract.integrationtests.common.loans.LoanProductHelper;
import org.apache.fineract.integrationtests.common.loans.LoanStatusChecker;
import org.apache.fineract.integrationtests.common.loans.LoanTransactionHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PracticeTask7Test {

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
    public void testLoanLifecycle() {
        final LoanTransactionHelper loanTransactionHelper = new LoanTransactionHelper(this.requestSpec, this.responseSpec);
        final LoanProductHelper loanProductHelper = new LoanProductHelper();

        // 1. Create Active Client
        PostClientsRequest clientRequest = new PostClientsRequest()
                .officeId(1L)
                .legalFormId(1L)
                .firstname("Task7")
                .lastname("LoanClient")
                .active(true)
                .activationDate("01 June 2026")
                .dateFormat("dd MMMM yyyy")
                .locale("en");
        PostClientsResponse clientResponse = ClientHelper.createClient(clientRequest);
        Integer clientId = clientResponse.getClientId().intValue();

        // 2. Create Loan Product
        String loanProductJSON = loanProductHelper.withPrincipal("1000")
                .withNumberOfRepayments("12")
                .withRepaymentAfterEvery("1")
                .withRepaymentTypeAsMonth()
                .withinterestRatePerPeriod("1")
                .withInterestRateFrequencyTypeAsMonths()
                .withAmortizationTypeAsEqualInstallments()
                .withInterestTypeAsFlat()
                .withAccountingRuleAsCashBased()
                .build(null);
        Integer productId = loanProductHelper.createLoanProduct(loanProductJSON);

        // 3. Apply for Loan
        String loanApplicationJSON = loanTransactionHelper.withPrincipal("1000")
                .withLoanTermFrequency("12")
                .withLoanTermFrequencyAsMonths()
                .withNumberOfRepayments("12")
                .withRepaymentEveryAfter("1")
                .withRepaymentFrequencyTypeAsMonths()
                .withAmortizationTypeAsEqualInstallments()
                .withInterestTypeAsFlat()
                .withInterestCalculationPeriodTypeAsDaily()
                .withExpectedDisbursementDate("01 June 2026")
                .withSubmittedOnDate("01 June 2026")
                .build(clientId.toString(), productId.toString(), null);
        Integer loanId = loanTransactionHelper.getLoanId(loanApplicationJSON);

        // 4. Approve
        loanTransactionHelper.approveLoan("02 June 2026", loanId);

        // 5. Disburse
        loanTransactionHelper.disburseLoanWithNetDisversalAmount("02 June 2026", loanId, "1000");

        // 6. Verify Status
        HashMap loanStatus = loanTransactionHelper.getLoanStatus(loanId);
        LoanStatusChecker.verifyLoanIsActive(loanStatus);
    }
}
