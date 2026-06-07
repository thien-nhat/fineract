# 10 bài tập backend kiểu “giống đi làm” cho Fineract

Mục tiêu của bộ bài này không phải là “làm CRUD cho có”, mà là luyện các kỹ năng mà backend dev junior+ trở lên phải đụng thật:

- đọc và dùng API có sẵn thay vì code chay
- hiểu lifecycle nghiệp vụ
- biết thiết kế transaction
- biết cache đúng chỗ
- biết làm async/outbox thay vì dual-write
- biết setup data nhanh bằng script để test đi test lại

## 0. Bộ khung local trước khi làm bài

### Chạy DB bằng Docker

```bash
docker rm -f postgres 2>/dev/null || true
docker run --name postgres -p 5432:5432 \
  -e POSTGRES_USER=root \
  -e POSTGRES_PASSWORD=postgres \
  -u nobody:nogroup \
  -d postgres:18.3
```

### Chạy backend

Repo này đã có sẵn script:

```bash
scripts/start-fineract.sh
```

Health check:

```bash
curl --insecure https://localhost:8443/fineract-provider/actuator/health
```

### Header dùng chung

```bash
export FIN_URL="https://localhost:8443/fineract-provider/api/v1"
export FIN_AUTH="Authorization: Basic bWlmb3M6cGFzc3dvcmQ="
export FIN_TENANT="Fineract-Platform-TenantId: default"
export FIN_JSON="Content-Type: application/json"
```

Ví dụ gọi API:

```bash
curl --insecure "$FIN_URL/clients" \
  -H "$FIN_AUTH" \
  -H "$FIN_TENANT" \
  -H "$FIN_JSON"
```

Lưu ý rất quan trọng trước khi copy payload:

- đừng assume mọi `id` đều là `1`; với local DB khác sample data thì `officeId`, `clientId`, `productId`, `staffId`, `loanId`, `savingsId` có thể khác
- nên lấy ID thật từ response của bước trước đó, hoặc từ các API `template`, `list`, `GET by id`
- nếu một field optional như `fieldOfficerId` không có dữ liệu hợp lệ trong DB của anh, hãy bỏ hẳn field đó khỏi payload thay vì nhét đại `1`

## 1. Dựng cây `office` và quản lý phân cấp

### API thật

- `GET /offices`
- `GET /offices/template`
- `POST /offices`
- `GET /offices/{officeId}`
- `PUT /offices/{officeId}`

### Map payload -> endpoint

- `GET /offices`
  Dùng để list cây office hiện có. Không có request body.
- `GET /offices/template`
  Dùng để lấy data hỗ trợ tạo office mới, đặc biệt là `allowedParents`.
- `POST /offices`
  Dùng đúng với payload tạo office ở dưới. `parentId` là field hợp lệ ở request tạo mới.
- `PUT /offices/{officeId}`
  Chỉ update các field như `name`, `openingDate`, `externalId`.
  Không dùng `parentId` ở API update này.

### Data setup nên viết vào `scripts/`

- `scripts/setup-office-tree.sh`
- `scripts/list-offices.sh`

### Payload mẫu

Tạo branch office:

```json
{
  "name": "Hanoi Branch",
  "openingDate": "01 June 2026",
  "dateFormat": "dd MMMM yyyy",
  "locale": "en",
  "parentId": 1
}
```

Payload trên thuộc về:

- `POST /offices`

Lưu ý:

- `parentId` có trong schema request tạo office của code hiện tại, xem [fineract-provider/src/main/java/org/apache/fineract/organisation/office/api/OfficesApiResourceSwagger.java](fineract-provider/src/main/java/org/apache/fineract/organisation/office/api/OfficesApiResourceSwagger.java)
- nếu Swagger UI của anh không hiện `parentId` thì đó là vấn đề render spec/UI, không phải API backend không hỗ trợ field này
- để biết `parentId` nào hợp lệ, gọi trước `GET /offices/template` hoặc `GET /offices/{officeId}?template=true`

Ví dụ update office:

```json
{
  "name": "Hanoi Branch Updated",
  "openingDate": "01 June 2026",
  "dateFormat": "dd MMMM yyyy",
  "locale": "en"
}
```

Payload update trên thuộc về:

- `PUT /offices/{officeId}`

### Logic nghiệp vụ cần hiểu

- khi tạo branch dưới office có sẵn thì phải có `parentId`
- office là cấu trúc phân cấp, không phải flat master data
- update office phải giữ được tính hợp lệ của cây phân cấp

### Kỹ năng luyện

- list, sort, filter
- setup data nền
- hiểu entity có hierarchy

### Dấu hiệu làm xong

- tạo được ít nhất 3 office theo cây
- viết script list office có `orderBy=name&sortOrder=ASC`
- không tạo parent-child sai logic

## 2. Onboard `client` theo lifecycle thật

### API thật

- `GET /clients/template?officeId=1`
- `POST /clients`
- `GET /clients?officeId=1&status=Pending`
- `GET /clients/{clientId}`
- `PUT /clients/{clientId}`
- `POST /clients/{clientId}?command=activate`
- `POST /clients/{clientId}?command=close`

### Map payload -> endpoint

- `GET /clients/template?officeId=1`
  Lấy template/options trước khi tạo client.
- `POST /clients`
  Dùng payload tạo client pending ở dưới.
- `PUT /clients/{clientId}`
  Dùng để sửa thông tin client khi state cho phép.
- `POST /clients/{clientId}?command=activate`
  Dùng payload activation ở dưới.
- `POST /clients/{clientId}?command=close`
  Là command đóng client, không phải `PUT status=closed`.

### Data setup nên viết vào `scripts/`

- `scripts/create-client-pending.sh`
- `scripts/activate-client.sh`
- `scripts/list-clients.sh`

### Payload mẫu

Tạo client pending:

```json
{
  "officeId": 1,
  "legalFormId": 1,
  "firstname": "An",
  "lastname": "Nguyen",
  "mobileNo": "0900000001",
  "active": false,
  "submittedOnDate": "01 June 2026",
  "dateFormat": "dd MMMM yyyy",
  "locale": "en"
}
```

Kích hoạt client:

```json
{
  "activationDate": "02 June 2026",
  "dateFormat": "dd MMMM yyyy",
  "locale": "en"
}
```

Map cụ thể:

- payload `Tạo client pending` -> `POST /clients`
- payload `Kích hoạt client` -> `POST /clients/{clientId}?command=activate`

Điểm hay bị nhầm:

- `active: false` chỉ dùng lúc tạo client pending
- activate là command API riêng, không phải update field `active=true`
- `legalFormId` là field bắt buộc ở build hiện tại; với client cá nhân dùng `1`, với tổ chức dùng `2`
- `officeId` thường nên lấy context từ `GET /clients/template?officeId=...`

### Logic nghiệp vụ cần hiểu

- client có state: pending, active, closed
- không phải cứ tạo xong là active
- hành vi thay đổi theo state, đây là state machine chứ không phải CRUD đơn thuần

### Kỹ năng luyện

- lifecycle API
- partial update
- status-based rule

### Dấu hiệu làm xong

- tạo được 5 client pending
- chỉ activate được client đang pending
- viết script list client theo `status`, `offset`, `limit`, `orderBy`

## 3. Gắn `notes` cho client để luyện CRUD nhỏ nhưng có ngữ cảnh

### API thật

- `GET /clients/{clientId}/notes`
- `POST /clients/{clientId}/notes`
- `GET /clients/{clientId}/notes/{noteId}`
- `PUT /clients/{clientId}/notes/{noteId}`
- `DELETE /clients/{clientId}/notes/{noteId}`

### Map payload -> endpoint

- payload note ở dưới -> `POST /clients/{clientId}/notes`
- update note thường vẫn là JSON cùng shape `{"note":"..."}` -> `PUT /clients/{clientId}/notes/{noteId}`
- delete note không có request body

### Data setup nên viết vào `scripts/`

- `scripts/add-client-note.sh`
- `scripts/update-client-note.sh`
- `scripts/delete-client-note.sh`

### Payload mẫu

```json
{
  "note": "Khách hàng đã được xác minh số điện thoại và địa chỉ."
}
```

Lưu ý:

- field đúng của API là `note`
- không dùng `text`

### Logic nghiệp vụ cần hiểu

- note là child resource của client
- đây là chỗ tốt để luyện audit, soft delete, ownership
- “CRUD nhỏ” nhưng rất giống việc dev thêm tính năng cho màn hình chăm sóc khách hàng

### Kỹ năng luyện

- nested resource
- audit trail
- soft delete nếu muốn nâng cấp bài

### Dấu hiệu làm xong

- note list trả theo thứ tự mới nhất trước
- update/delete chỉ tác động đúng note thuộc đúng client

## 4. Tạo `savings product` như một bài master-data thực chiến

### API thật

- `GET /savingsproducts/template`
- `POST /savingsproducts`
- `GET /savingsproducts`
- `GET /savingsproducts/{productId}`
- `PUT /savingsproducts/{productId}`

### Map payload -> endpoint

- `GET /savingsproducts/template`
  Lấy default/options trước khi dựng payload thật.
- payload product tối giản ở dưới -> `POST /savingsproducts`
- `PUT /savingsproducts/{productId}`
  Dùng khi sửa cấu hình product. Không phải mọi field đều nên sửa bừa sau khi đã có account dùng product đó.

### Data setup nên viết vào `scripts/`

- `scripts/create-savings-product.sh`
- `scripts/list-savings-products.sh`

### Payload mẫu tối giản

```json
{
  "name": "Basic Savings 2026",
  "shortName": "BS26",
  "description": "Sản phẩm tiết kiệm cơ bản cho khách hàng cá nhân",
  "currencyCode": "USD",
  "digitsAfterDecimal": 2,
  "inMultiplesOf": 0,
  "nominalAnnualInterestRate": 5,
  "interestCompoundingPeriodType": 1,
  "interestPostingPeriodType": 4,
  "interestCalculationType": 1,
  "interestCalculationDaysInYearType": 365,
  "accountingRule": 1,
  "locale": "en"
}
```

Lưu ý:

- payload này là payload tối giản để hiểu flow, không đảm bảo đủ cho mọi cấu hình/accounting mode
- `interestPostingPeriodType` cũng là field bắt buộc theo validator hiện tại, dù một số mô tả cũ trong code không ghi đủ
- `interestCalculationDaysInYearType` ở savings product nhận enum ID như `360` hoặc `365`
- `accountingRule: 1` là `No accounting`, nên đây là lựa chọn đơn giản nhất để tránh phải truyền thêm account mapping
- bài này nên gọi `GET /savingsproducts/template` trước rồi fill các enum/options từ template

### Logic nghiệp vụ cần hiểu

- product là cấu hình nghiệp vụ, không phải tài khoản
- dữ liệu product sẽ được kế thừa xuống savings account
- bài này luyện kiểu “admin config feature”

### Kỹ năng luyện

- template endpoint
- master data CRUD
- field inheritance

### Dấu hiệu làm xong

- tạo được 2 savings product khác nhau
- update product không phá account đang dùng

## 5. Mở `savings account` theo flow thật: submit -> approve -> activate

### API thật

- `GET /savingsaccounts/template?clientId={clientId}&productId={productId}`
- `POST /savingsaccounts`
- `GET /savingsaccounts/{accountId}`
- `PUT /savingsaccounts/{accountId}`
- `POST /savingsaccounts/{accountId}?command=approve`
- `POST /savingsaccounts/{accountId}?command=activate`
- `POST /savingsaccounts/{accountId}?command=close`

### Map payload -> endpoint

- payload `Submit application` -> `POST /savingsaccounts`
- payload `Approve` -> `POST /savingsaccounts/{accountId}?command=approve`
- payload `Activate` -> `POST /savingsaccounts/{accountId}?command=activate`
- `GET /savingsaccounts/template?...`
  Dùng để lấy options/defaults trước khi submit application

### Data setup nên viết vào `scripts/`

- `scripts/open-savings-account.sh`
- `scripts/approve-savings-account.sh`
- `scripts/activate-savings-account.sh`

### Payload mẫu

Submit application:

```json
{
  "clientId": 1,
  "productId": 1,
  "submittedOnDate": "02 June 2026",
  "fieldOfficerId": 1,
  "locale": "en",
  "dateFormat": "dd MMMM yyyy"
}
```

Approve:

```json
{
  "approvedOnDate": "03 June 2026",
  "locale": "en",
  "dateFormat": "dd MMMM yyyy"
}
```

Activate:

```json
{
  "activatedOnDate": "03 June 2026",
  "locale": "en",
  "dateFormat": "dd MMMM yyyy"
}
```

Điểm hay bị nhầm:

- `clientId` hoặc `groupId` phải là entity đang `active`; client pending chưa mở savings account được
- `fieldOfficerId` có thể không luôn bắt buộc, tùy data setup
- nếu local của anh không có staff `id=1`, bỏ hẳn `fieldOfficerId` khỏi payload submit application
- `productId` phải là `savings product` hợp lệ, không nên assume luôn là `1` nếu DB local của anh khác sample data
- approve và activate là 2 command khác nhau, không phải update status qua `PUT`
- nên đọc response của `GET /savingsaccounts/{accountId}` sau mỗi bước để thấy state machine

### Logic nghiệp vụ cần hiểu

- savings account có application lifecycle rõ ràng
- chỉ một số field được sửa khi còn pending approval
- đây là chỗ rất tốt để luyện state transition có điều kiện

### Kỹ năng luyện

- command-based API
- state machine
- partial update theo state

### Dấu hiệu làm xong

- account chỉ activate được sau approve
- update bị chặn khi trạng thái không cho phép

## 6. Tìm kiếm `savings transactions` và thêm Redis cache đúng chỗ

### Bối cảnh nghiệp vụ

Đây không chỉ là bài `search + cache`.

Trong business thật, màn hình lịch sử giao dịch savings thường được dùng bởi:

- teller hoặc branch staff tra cứu giao dịch gần đây để hỗ trợ khách hàng
- customer support kiểm tra một giao dịch bị khiếu nại
- operations đối soát xem tiền vào/ra account theo ngày
- supervisor rà soát hành vi bất thường trên một account

Điểm quan trọng là người dùng business không quan tâm `row` trong DB. Họ đang nhìn một `account ledger view`:

- giao dịch nào đã phát sinh
- ngày giá trị là ngày nào
- đó là credit hay debit
- running balance sau giao dịch là bao nhiêu
- có missing event nào không

Vì vậy bài này là bài rất tốt để luyện một câu hỏi system design rất thực tế:

- read-heavy endpoint nào đáng cache
- cache theo query nào
- stale data chấp nhận được bao lâu
- invalidation nên bám account-level hay query-level
- khi nào cache làm sai business expectation

### API thật

- `GET /savingsaccounts/{savingsId}/transactions/template`
- `GET /savingsaccounts/{savingsId}/transactions/{transactionId}`
- `GET /savingsaccounts/{savingsId}/transactions/search?...`

### Map payload -> endpoint

- bài này chủ yếu là `GET`, không có payload JSON chính
- query string ở ví dụ curl phía dưới chính là input chính của API search
- `template` endpoint giúp biết các command/metadata liên quan transaction

### Data setup nên viết vào `scripts/`

- `scripts/search-savings-transactions.sh`
- `scripts/redis-up.sh`
- `scripts/redis-flush.sh`

### Truy vấn mẫu

```bash
curl --insecure \
  "$FIN_URL/savingsaccounts/1/transactions/search?fromDate=2026-06-01&toDate=2026-06-30&limit=20&orderBy=transactionDate,id&sortOrder=DESC&locale=en&dateFormat=yyyy-MM-dd" \
  -H "$FIN_AUTH" \
  -H "$FIN_TENANT" \
  -H "$FIN_JSON"
```

### Bài làm thêm giống đi làm

Thêm Redis cache cho endpoint search nhiều nhưng ghi ít, ví dụ:

- key: `savings:tx-search:{accountId}:{normalizedQuery}`
- TTL: `60s` hoặc `300s`
- invalidate khi có transaction mới phát sinh trên account đó

### Logic nghiệp vụ cần hiểu

- search transaction là màn hình rất hay bị gọi lặp
- cache phải bám query normalized, không phải cache bừa
- bài này luyện trade-off giữa tốc độ và dữ liệu stale
- muốn có data để search thì account phải được activate trước và đã có transaction thật

### Lifecycle business nên hình dung

Flow business đằng sau task này là:

1. client active
2. savings account được submit
3. savings account được approve
4. savings account được activate
5. các transaction thật mới bắt đầu xuất hiện
6. transaction history trở thành read model để tra cứu

Nghĩa là `search` không phải nghiệp vụ gốc. Nó là nghiệp vụ đọc trên một lifecycle đã hoàn thành ở phía trước.

Nếu anh không hiểu phần này, rất dễ cache sai, ví dụ:

- account chưa active mà vẫn cho search ra data lạ
- transaction vừa được post nhưng query cũ vẫn trả kết quả stale quá lâu
- invalidate thiếu nên support nhìn thấy số dư/running balance cũ

### Invariants nên viết ra trước khi code

- một search result luôn gắn với đúng `savingsId`
- kết quả phải phản ánh đúng filter business: date, amount, credit/debit, type
- sort phải stable, nếu không pagination sẽ nhảy loạn
- running balance và transaction order phải nhất quán với ledger view
- transaction mới phát sinh trên account đó phải làm các cache entry liên quan tới account đó hết hiệu lực

### Những câu hỏi business nên tự trả lời

- user thật cần search theo gì nhiều nhất: ngày, số tiền, loại giao dịch, hay booking date
- transaction vừa phát sinh có cần thấy ngay lập tức không, hay stale 60 giây vẫn chấp nhận được
- search này phục vụ màn hình realtime hay báo cáo gần realtime
- support có hay refresh liên tục cùng một query không
- một account có thể có bao nhiêu giao dịch, 100, 10k, hay 1M

Những câu hỏi đó quyết định:

- có cache hay không
- cache ở application hay query layer
- TTL bao lâu
- invalidation coarse hay fine-grained

### Hướng thiết kế nên nghĩ thêm

- normalize query thật kỹ trước khi sinh cache key
- đừng cache các query quá rộng vô hạn nếu tenant có data lớn
- cân nhắc cache page đầu tiên trước, chưa chắc phải cache toàn bộ mọi page
- thêm metric `hit`, `miss`, `eviction`, `stale-served`, `search-latency`
- với query ít lặp lại, cache có thể tốn hơn lợi

### Failure cases đáng mô phỏng

- deposit xong search ngay nhưng vẫn thấy cache cũ
- withdrawal bị reverse/undo nhưng search không phản ánh
- orderBy khác nhau sinh 2 cache key khác nhau nhưng normalize không đúng
- pagination page 1 và page 2 không đồng nhất vì trong lúc đó có transaction mới
- Redis down thì search phải fallback về DB, không được kéo theo outage

### Nếu làm như BA hoặc architect

Hãy mô tả bài này bằng ngôn ngữ nghiệp vụ như sau:

- “Nhân viên quầy cần tra cứu lịch sử giao dịch savings account theo ngày và loại giao dịch với độ trễ thấp.”
- “Kết quả phải đủ tin cậy để hỗ trợ xử lý khiếu nại khách hàng.”
- “Sau khi phát sinh giao dịch mới, dữ liệu cũ không được tồn tại quá lâu trong màn hình tra cứu.”

Khi mô tả được như vậy, anh đang đi từ business requirement sang technical design, không còn là “thêm Redis cho vui”.

### Kỹ năng luyện

- search endpoint
- cache-aside
- cache invalidation

### Dấu hiệu làm xong

- đo được cache hit/miss
- update giao dịch làm cache cũ bị xóa đúng account

## 7. Tạo `loan` theo lifecycle thật: submit -> approve -> disburse

### Bối cảnh nghiệp vụ

Đây là một trong những flow cốt lõi nhất của Fineract.

`loan` trong business không phải là một record CRUD đơn giản. Nó là một credit account đi qua nhiều mốc phê duyệt và phát tiền:

- khách hàng nộp hồ sơ vay
- tổ chức thẩm định
- người có thẩm quyền phê duyệt số tiền
- tiền thực sự được giải ngân
- từ lúc đó loan mới thực sự sống và sinh repayment schedule / transactions

Nói cách khác:

- `submit` = tạo application
- `approve` = chấp thuận về mặt tín dụng / quy trình
- `disburse` = phát tiền thật

Ba bước này bị tách riêng vì trong business thật chúng thường do các actor khác nhau làm.

### Actor nghiệp vụ

- loan officer hoặc sales tạo hồ sơ vay
- credit approver hoặc supervisor phê duyệt
- teller hoặc disbursement operation giải ngân
- customer chỉ là đối tượng được cấp loan, không phải actor hệ thống trực tiếp ở flow này

### Tại sao task này quan trọng cho system design

Bài này buộc anh phải nghĩ về:

- lifecycle thay vì CRUD
- validation phụ thuộc state
- field nào là input lúc submit, field nào chỉ xuất hiện ở approve/disburse
- side effects nào chỉ có sau disbursement
- event nào nên publish ở từng mốc

Nếu anh thiết kế sai ở đây, các bài sau như repayment, write-off, delinquency gần như sẽ lệch toàn bộ.

### API thật

- `GET /loans/template?templateType=individual&clientId={clientId}`
- `POST /loans`
- `GET /loans/{loanId}`
- `PUT /loans/{loanId}`
- `POST /loans/{loanId}?command=approve`
- `POST /loans/{loanId}?command=disburse`
- `POST /loans/{loanId}?command=reject`

### Map payload -> endpoint

- payload `Submit loan` -> `POST /loans`
- payload `Approve` -> `POST /loans/{loanId}?command=approve`
- payload `Disburse` -> `POST /loans/{loanId}?command=disburse`
- `GET /loans/template?...`
  Dùng để lấy product/options/schedule-related defaults trước khi submit

### Data setup nên viết vào `scripts/`

- `scripts/create-loan-application.sh`
- `scripts/approve-loan.sh`
- `scripts/disburse-loan.sh`

### Payload mẫu

Submit loan:

```json
{
  "clientId": 1,
  "loanType": "individual",
  "productId": 1,
  "principal": 1000,
  "loanTermFrequency": 12,
  "loanTermFrequencyType": 2,
  "numberOfRepayments": 12,
  "repaymentEvery": 1,
  "repaymentFrequencyType": 2,
  "interestRatePerPeriod": 12,
  "interestRateFrequencyType": 3,
  "amortizationType": 1,
  "interestType": 0,
  "interestCalculationPeriodType": 1,
  "expectedDisbursementDate": "05 June 2026",
  "submittedOnDate": "04 June 2026",
  "transactionProcessingStrategyCode": "mifos-standard-strategy",
  "locale": "en",
  "dateFormat": "dd MMMM yyyy"
}
```

Approve:

```json
{
  "approvedOnDate": "05 June 2026",
  "approvedLoanAmount": 1000,
  "dateFormat": "dd MMMM yyyy",
  "locale": "en"
}
```

Disburse:

```json
{
  "actualDisbursementDate": "05 June 2026",
  "transactionAmount": 1000,
  "dateFormat": "dd MMMM yyyy",
  "locale": "en"
}
```

Điểm hay bị nhầm:

- `clientId` hoặc `groupId` phải đang `active`; client pending sẽ bị chặn ngay từ lúc submit loan
- `productId` phải là loan product hợp lệ, không phải savings product, và không nên assume luôn là `1`
- `loanType` là field bắt buộc ở build hiện tại, với bài này dùng `individual`
- nhiều field enum là mã nghiệp vụ của Fineract, không nên đoán tay nếu chưa gọi `template`
- `transactionProcessingStrategyCode` phải khớp strategy đang có trong DB

### Logic nghiệp vụ cần hiểu

- loan không phải create xong là xài được
- approval và disbursement là hai bước nghiệp vụ khác nhau
- đây là bài cực điển hình để luyện domain flow

### Lifecycle business nên vẽ ra

Ít nhất nên hình dung state machine tối thiểu:

1. `submitted`
2. `approved`
3. `active` sau khi disburse
4. sau này mới đến repayment, close, write-off, charge-off, reject...

Chính vì vậy:

- chưa approve thì không disburse
- approve sai ngày business date có thể bị chặn
- disburse là mốc cực mạnh vì từ đó repayment schedule và transaction history trở thành dữ liệu nghiệp vụ thật

### Invariants nên khóa từ đầu

- client hoặc group phải ở trạng thái `active`
- product phải là loan product hợp lệ của tenant
- loanType phải khớp business context, ở đây là `individual`
- approved amount không thể vô nghĩa so với submitted principal
- actual disbursement date không được vi phạm business date rules
- disbursement không được xảy ra khi application chưa ở state phù hợp

### Những câu hỏi business cần trả lời

- ai có quyền approve và ai có quyền disburse
- approved amount có được khác submitted amount không, khác bao nhiêu thì hợp lệ
- disbursement có thể partial hay multi-tranche không
- expected disbursement date là planning date hay hard rule
- sau disburse thì những field nào không được sửa nữa

Chỉ khi trả lời được các câu hỏi này, anh mới thiết kế API/service boundary chuẩn.

### Side effects nên nghĩ tới

Sau khi disburse, thường sẽ có:

- loan chuyển sang trạng thái business mới
- transaction disbursement được ghi nhận
- repayment schedule trở thành dữ liệu vận hành
- accounting / journal / event downstream có thể được kích hoạt

Nghĩa là `disburse` không phải update một cột status. Nó là một business action có hệ quả hệ thống.

### Failure modes đáng mô phỏng

- submit hợp lệ schema nhưng sai product semantics
- approve ở ngày tương lai so với business date
- disburse khi chưa approve
- disburse amount không khớp approved amount
- disburse lặp lại do retry hoặc user double-click
- disburse thành công một phần nhưng side effects phía sau fail

Những case này rất tốt để nghĩ về:

- idempotency
- command processing
- transaction boundary
- audit trail

### Góc nhìn BA / domain model

Nếu viết theo ngôn ngữ BA, task này nên được đọc như sau:

- “Tổ chức cần kiểm soát quy trình cấp tín dụng thành các bước submit, approve và disburse thay vì tạo loan active ngay.”
- “Mỗi bước có precondition riêng, actor riêng và dữ liệu đầu vào riêng.”
- “Loan chỉ trở thành account hoạt động sau khi disbursement hoàn tất.”

Đây là ngôn ngữ rất gần dự án thật trong fintech/banking.

### Hướng mở rộng nếu muốn luyện sâu hơn

- tách role-based authorization cho submit/approve/disburse
- thêm idempotency key cho disburse
- thêm event `LoanApproved`, `LoanDisbursed`
- thêm audit timeline đọc được bởi support/compliance
- thêm guard với concurrent approve/disburse

### Kỹ năng luyện

- state transition
- payload nhiều field
- template-driven creation

### Dấu hiệu làm xong

- đọc được loan detail có `repaymentSchedule,transactions`
- disburse sai state bị chặn

## 8. Thực hiện `loan transactions`: repayment, waive interest, write-off

### Bối cảnh nghiệp vụ

Task này là lúc anh chuyển từ “lifecycle mở account” sang “vận hành account”.

Sau khi loan đã active, business không sửa balance bằng tay. Mọi thay đổi quan trọng đi qua `transactions`:

- `repayment`
- `waive interest`
- `write-off`

Đây là một ý rất lớn trong domain tài chính:

- balance là kết quả của ledger actions
- không phải giá trị anh update trực tiếp

Cho nên task này rất gần với hệ thống thật, vì nó buộc anh nghĩ như một event/ledger system.

### Ý nghĩa business của từng action

`repayment`

- khách hàng trả tiền cho khoản vay
- hệ thống phải phân bổ số tiền vào principal / interest / fee / penalty theo strategy
- transaction này ảnh hưởng schedule, outstanding, overdue, accounting

`waiveinterest`

- tổ chức quyết định miễn một phần hoặc toàn bộ lãi cho khách hàng
- đây không phải repayment thật từ khách hàng
- nó là quyết định nghiệp vụ và có hậu quả kế toán riêng

`writeoff`

- tổ chức kết luận khoản nợ không còn kỳ vọng thu hồi theo cách bình thường
- loan bị đưa sang trạng thái business rất mạnh, thường đóng lại dưới dạng written-off
- sau mốc này nhiều thao tác update/undo bình thường sẽ bị chặn

Chỉ riêng việc hiểu khác nhau giữa 3 action này đã là một bài BA/domain rất tốt.

### API thật

- `GET /loans/{loanId}/transactions/template?command=repayment`
- `POST /loans/{loanId}/transactions?command=repayment`
- `POST /loans/{loanId}/transactions?command=waiveinterest`
- `POST /loans/{loanId}/transactions?command=writeoff`
- `GET /loans/{loanId}/transactions`
- `GET /loans/{loanId}/transactions/{transactionId}`

### Map payload -> endpoint

- payload repayment ở dưới -> `POST /loans/{loanId}/transactions?command=repayment`
- waive interest thường dùng payload ngày + locale/dateFormat -> `POST /loans/{loanId}/transactions?command=waiveinterest`
- write-off cũng là command endpoint riêng -> `POST /loans/{loanId}/transactions?command=writeoff`
- `GET /loans/{loanId}/transactions/template?command=repayment`
  Dùng để xem metadata/defaults trước khi trả nợ

### Data setup nên viết vào `scripts/`

- `scripts/loan-repayment.sh`
- `scripts/loan-waive-interest.sh`
- `scripts/loan-writeoff.sh`
- `scripts/list-loan-transactions.sh`

### Payload repayment mẫu

```json
{
  "transactionDate": "10 June 2026",
  "transactionAmount": 100,
  "dateFormat": "dd MMMM yyyy",
  "locale": "en"
}
```

Payload mẫu cho `waiveinterest`:

```json
{
  "transactionDate": "10 June 2026",
  "transactionAmount": 100,
  "dateFormat": "dd MMMM yyyy",
  "locale": "en"
}
```

Payload mẫu cho `writeoff`:

```json
{
  "transactionDate": "10 June 2026",
  "dateFormat": "dd MMMM yyyy",
  "locale": "en"
}
```

Lưu ý:

- các command này chỉ có ý nghĩa sau khi loan đã được approve rồi disburse thành công
- `repayment` bắt buộc `transactionAmount`
- `waiveinterest` ở build hiện tại cũng cần `transactionAmount`
- `writeoff` không cần `transactionAmount`, nhưng có thể truyền thêm `writeoffReasonId` nếu instance của anh có setup code value tương ứng
- với cả 3 command, `transactionDate` không được ở tương lai và không được trước các giao dịch đã có

### Logic nghiệp vụ cần hiểu

- repayment là transaction thật, không phải update loan balance bằng tay
- waive interest và write-off là business action có hậu quả kế toán
- transaction history phải truy vết được

### Vì sao task này tốt cho system design

Nó buộc anh trả lời các câu hỏi khó hơn CRUD:

- transaction nào được phép ở state nào
- transaction order có quan trọng không
- có được backdate không
- undo/reverse có được phép không
- sau write-off còn action nào hợp lệ
- read model của transaction history có cần strong consistency không

Đây là đúng kiểu câu hỏi ở production system cho lending.

### Preconditions nên ghi rõ

- loan phải đã `approved` và `disbursed`
- transactionDate không được đi trước timeline nghiệp vụ đã có
- command phải hợp lệ với status hiện tại của loan
- repayment/waiver/write-off phải obey business date rules

Riêng `writeoff` nên coi là flow riêng:

- tốt nhất đừng test chung trên chính loan đang dùng cho repayment hằng ngày
- vì sau write-off, behavior của loan thay đổi mạnh

### Invariants nên tự bảo vệ

- transaction history phải append đúng business meaning
- running balances / outstanding / schedule phải nhất quán sau mỗi action
- write-off không được làm loan vẫn mở ở state sai
- waived amount không được làm phát sinh số âm vô lý
- repayment không được phá thứ tự ledger
- cùng một request retry không được tạo duplicate transaction nếu hệ thống có idempotency

### Các câu hỏi business đáng suy nghĩ

- repayment được phân bổ theo chiến lược nào
- waive interest có cần approval riêng không
- write-off là quyết định vận hành hay kế toán
- write-off có thể undo không, nếu không thì vì sao
- repayment sau write-off có được phép không, nếu có thì dưới rule nào
- support/audit có cần xem transaction trail theo thứ tự business hay thứ tự ghi DB

### Failure cases nên mô phỏng

- repayment sau write-off
- backdate repayment trước một transaction đã tồn tại
- waive interest nhiều lần làm số liệu lệch
- write-off trên loan chưa active
- retry command tạo duplicate
- history list đúng nhưng summary/outstanding sai

Đây là chỗ rất tốt để luyện consistency giữa:

- write model
- derived balance
- repayment schedule
- history endpoint

### Góc nhìn read model

Sau khi làm transaction, anh không nên chỉ nhìn response command.
Phải đọc lại:

- `GET /loans/{loanId}`
- `GET /loans/{loanId}/transactions`
- `GET /loans/{loanId}/transactions/{transactionId}`

Nếu làm sâu hơn, còn nên so:

- summary
- schedule
- history

để xem toàn bộ model có còn đồng bộ không.

### Cách BA diễn đạt task này

- “Khoản vay đang hoạt động phải được cập nhật qua các giao dịch nghiệp vụ, không sửa trực tiếp số dư.”
- “Mỗi loại giao dịch có ý nghĩa nghiệp vụ riêng và ảnh hưởng khác nhau tới trạng thái khoản vay.”
- “Lịch sử giao dịch phải đủ rõ để support, audit và accounting cùng hiểu một sự kiện đã xảy ra là gì.”

Đó chính là ngôn ngữ của một loan servicing system thật.

### Hướng mở rộng nếu muốn luyện thêm

- thêm `undo` hoặc `adjust` cho repayment để luyện reversal model
- thêm transaction idempotency key
- thêm audit/event publication sau mỗi command
- thêm transaction timeline screen hoặc reporting projection
- so sánh `writeoff` với `charge-off` như hai business concepts khác nhau

### Kỹ năng luyện

- command endpoint
- transaction history
- business action ngoài CRUD

### Dấu hiệu làm xong

- repayment sinh transaction mới
- write-off chỉ cho phép ở trạng thái hợp lệ
- xem lại history thấy đủ các event

## 9. Thiết kế một use case có `transaction` thật sự ở tầng code

### Bài toán đề xuất

Tự thêm endpoint mới:

- `POST /internal/v1/client-savings-bundle`

### Ý nghĩa nghiệp vụ

Một lệnh tạo:

- client mới
- note onboarding đầu tiên
- savings account application đầu tiên

Nếu một bước fail thì rollback toàn bộ.

### Request gợi ý

```json
{
  "client": {
    "officeId": 1,
    "legalFormId": 1,
    "firstname": "Binh",
    "lastname": "Tran",
    "active": true,
    "activationDate": "05 June 2026",
    "submittedOnDate": "05 June 2026",
    "dateFormat": "dd MMMM yyyy",
    "locale": "en"
  },
  "note": {
    "note": "Lead từ chiến dịch tháng 6"
  },
  "savingsApplication": {
    "productId": 1,
    "submittedOnDate": "05 June 2026",
    "dateFormat": "dd MMMM yyyy",
    "locale": "en"
  }
}
```

### Logic nghiệp vụ cần hiểu

- đây là bài rất giống đi làm: một màn hình submit, backend phải ghi nhiều bảng
- không được tạo client xong mà note fail rồi để data nửa vời
- không gọi external service ở giữa transaction DB
- nếu flow của anh giữ client ở trạng thái pending thì không thể mở savings application ngay; khi đó orchestration phải có thêm bước activate client trước khi tạo savings account

### Kỹ năng luyện

- transaction boundary
- orchestration service
- rollback đúng nghĩa

### Dấu hiệu làm xong

- fail ở bước 3 thì client và note cũng không còn
- test integration chứng minh rollback hoạt động

## 10. Thêm `outbox pattern` cho event sau khi giải ngân hoặc mở tài khoản

### Bài toán đề xuất

Khi một trong hai việc xảy ra:

- savings account được `activate`
- loan được `disburse`

thì ghi thêm một bản ghi vào `outbox_events`.

### API/business trigger nên bám

- `POST /savingsaccounts/{accountId}?command=activate`
- `POST /loans/{loanId}?command=disburse`

### Thiết kế tối thiểu

Bảng `outbox_events` nên có:

- `id`
- `aggregate_type`
- `aggregate_id`
- `event_type`
- `payload`
- `status`
- `created_at`
- `processed_at`
- `retry_count`

Status tối thiểu:

- `NEW`
- `SENT`
- `FAILED`

### Script nên viết vào `scripts/`

- `scripts/run-outbox-publisher.sh`
- `scripts/list-outbox-events.sh`
- `scripts/retry-failed-outbox.sh`

### Logic nghiệp vụ cần hiểu

- tuyệt đối tránh kiểu “commit business data xong rồi publish event ngay”
- business write và outbox write phải ở cùng transaction
- publisher đọc outbox sau, retry độc lập sau

### Kỹ năng luyện

- transaction + async boundary
- eventual consistency
- retry/idempotency

### Dấu hiệu làm xong

- disburse loan thành công thì luôn có outbox record
- publisher fail không làm mất event
- retry không publish trùng side effect

## 11. Thiết kế `business date` như một control plane cho toàn hệ thống

### Bài toán đề xuất

Lấy cảm hứng từ `BusinessDate.feature`, làm một bài không chỉ gọi API bật/tắt business date mà còn thiết kế cách backend dùng business date như nguồn sự thật cho workflow.

### API/business trigger nên bám

- `PUT /configurations/name/enable-business-date`
- các API set/check business date hiện có trong system
- mọi flow loan/savings đang phụ thuộc business date thay vì tenant date

### Yêu cầu design

- thiết kế rõ `business date` là global config hay tenant-scoped state
- định nghĩa nơi nào được phép dùng physical date, nơi nào bắt buộc dùng business date
- nếu business date đổi thủ công, phải xác định rõ read model nào cần phản ánh ngay
- nếu có batch job tăng business date, cần nghĩ đến idempotency và re-run safety

### Data setup nên viết vào `scripts/`

- `scripts/enable-business-date.sh`
- `scripts/set-business-date.sh`
- `scripts/show-business-date.sh`
- `scripts/increase-business-date.sh`

### Bài làm thêm giống đi làm

Thiết kế một lớp `BusinessDateProvider` hoặc tương đương để:

- toàn bộ service business chỉ đọc ngày qua abstraction này
- test có thể override dễ dàng
- log/audit luôn ghi được business date tại thời điểm hành động

### Logic nghiệp vụ cần hiểu

- business date không phải chỉ là config UI; nó tác động vào validation và scheduling
- cùng một physical day có thể phải hạch toán sang business day khác
- đây là chỗ rất tốt để luyện “system-wide invariant”

### Kỹ năng luyện

- cross-cutting design
- consistency rule
- configuration-driven behavior
- abstraction for time

### Dấu hiệu làm xong

- chứng minh được một flow loan/savings fail nếu submitted/approved/disbursement date sau business date
- có một abstraction ngày tháng rõ ràng thay vì gọi `LocalDate.now()` bừa
- rerun job tăng business date không tạo side effect sai

## 12. Thiết kế `account transfer` có undo, idempotency, và read-model nhất quán

### Bài toán đề xuất

Lấy cảm hứng từ `AccountTransfer.feature`, thêm một bài orchestration thật sự:

- savings -> savings
- savings -> loan
- undo transfer

### API/business trigger nên bám

- flow transfer giữa savings accounts
- flow transfer từ savings sang linked loan
- undo transfer

### Yêu cầu design

- coi transfer là một business aggregate riêng, không chỉ là 2 transaction rời
- định nghĩa `transferId`, state, audit trail, và rule `cannot undo twice`
- làm rõ write model và read model cho cả 2 phía nguồn/đích
- nếu một bên ghi thành công còn bên kia fail thì rollback thế nào

### Data setup nên viết vào `scripts/`

- `scripts/create-linked-savings-loan.sh`
- `scripts/transfer-savings-to-savings.sh`
- `scripts/transfer-savings-to-loan.sh`
- `scripts/undo-account-transfer.sh`

### Bài làm thêm giống đi làm

Thiết kế schema/bảng hoặc domain record cho transfer:

- `id`
- `source_account_type`
- `source_account_id`
- `destination_account_type`
- `destination_account_id`
- `amount`
- `status`
- `reversed`
- `created_at`
- `reversed_at`

### Logic nghiệp vụ cần hiểu

- transfer không phải chỉ là “withdraw ở A, deposit ở B”
- undo phải giữ accounting/history nhất quán ở cả hai phía
- idempotency là bắt buộc vì request transfer/undo rất dễ bị retry

### Kỹ năng luyện

- orchestration across aggregates
- saga-like thinking trong một transaction boundary
- undo/reversal modeling
- idempotency key design

### Dấu hiệu làm xong

- transfer source/destination đều phản ánh đúng balance và history
- undo làm cả hai phía về trạng thái đúng và không undo được lần hai
- có thể giải thích rõ transaction boundary và unique id cho transfer

## 13. Thêm bài `approved amount modification` như một use case mid-lifecycle

### Bài toán đề xuất

Lấy cảm hứng từ `LoanUpdateApprovedAmount.feature` và docs `approved-amount-modification.adoc`, luyện bài toán thay đổi constraint giữa vòng đời loan.

### API/business trigger nên bám

- `PUT /loans/{loanId}/approved-amount`
- `PUT /loans/{loanId}/available-disbursement-amount`
- `GET /loans/{loanId}/approved-amount`

### Yêu cầu design

- mô hình hóa rõ `proposed amount`, `approved amount`, `disbursed amount`, `available disbursement amount`
- chỉ rõ field nào là persisted state, field nào là derived state
- lịch sử thay đổi approved amount phải query được và explain được
- rule validation phải chịu được multi-disbursement, capitalized income, approved-over-applied

### Data setup nên viết vào `scripts/`

- `scripts/update-approved-amount.sh`
- `scripts/update-available-disbursement.sh`
- `scripts/list-approved-amount-history.sh`

### Bài làm thêm giống đi làm

Viết một note thiết kế ngắn cho endpoint:

- tại sao phải có history table riêng
- tại sao available disbursement nên là giá trị tính toán thay vì cột persisted
- race condition nào có thể xảy ra nếu 2 request update/disburse cùng lúc

### Logic nghiệp vụ cần hiểu

- đây là bài rất sát thực tế vì business rule có thể đổi sau approval
- mutation giữa lifecycle thường nguy hiểm hơn create ban đầu
- khi state có phần derived, design read/write model rất quan trọng

### Kỹ năng luyện

- derived state vs persisted state
- concurrency thinking
- audit/history design
- validation under partial disbursement

### Dấu hiệu làm xong

- giải thích được vì sao một amount hợp lệ trước disbursement nhưng không hợp lệ sau disbursement
- có history query rõ ràng cho approved amount changes
- không nhầm `availableDisbursementAmount` là state persisted

## 14. Thiết kế `write-off` và `charge-off` sao cho accounting không bị nhân đôi

### Bài toán đề xuất

Lấy cảm hứng từ `LoanWriteOff.feature`, làm bài phân biệt thật rõ:

- charge-off
- write-off
- undo/replay restriction sau write-off

### API/business trigger nên bám

- `POST /loans/{loanId}/transactions?command=writeoff`
- các flow charge-off liên quan
- transaction history và journal entries read APIs

### Yêu cầu design

- định nghĩa rõ khác nhau giữa `ACTIVE + charged-off` và `CLOSED_WRITTEN_OFF`
- xác định transaction nào bị cấm sau write-off
- đảm bảo journal entries không bị duplicate khi loan đã charge-off trước đó rồi mới write-off
- đọc history phải truy được cả business action lẫn accounting side effect

### Data setup nên viết vào `scripts/`

- `scripts/chargeoff-loan.sh`
- `scripts/writeoff-loan.sh`
- `scripts/list-loan-journal-entries.sh`
- `scripts/assert-no-duplicate-journal.sh`

### Bài làm thêm giống đi làm

Viết một bảng decision nhỏ:

- state hiện tại
- action người dùng gọi
- có cho phép không
- nếu không cho phép thì error message business nên là gì

### Logic nghiệp vụ cần hiểu

- write-off không chỉ là set status
- accounting consistency và replay safety mới là phần khó
- transaction history phải nói được “điều gì đã xảy ra” chứ không chỉ “balance hiện là bao nhiêu”

### Kỹ năng luyện

- accounting-aware design
- state transition restriction
- side-effect deduplication
- auditability

### Dấu hiệu làm xong

- giải thích được khác nhau giữa charge-off và write-off
- chứng minh được sau write-off một số update/undo phải bị chặn
- journal entries không bị nhân đôi khi đi qua charge-off rồi write-off

## Thứ tự nên làm

1. `office`
2. `client lifecycle`
3. `client notes`
4. `savings product`
5. `savings account lifecycle`
6. `savings transaction search + redis cache`
7. `loan lifecycle`
8. `loan transactions`
9. `custom transaction bundle`
10. `outbox pattern`
11. `business date control plane`
12. `account transfer + undo`
13. `approved amount modification`
14. `write-off vs charge-off consistency`

## Nếu muốn luyện kiểu sát công việc hơn nữa

Mỗi bài đều nên tự ép mình làm đủ 4 phần:

1. viết script setup trong `scripts/`
2. gọi API bằng `curl`
3. viết test integration
4. ghi lại state transition và business rule bằng một file `.md` ngắn

Nếu chỉ gọi API bằng tay rồi thôi thì học rất chậm. Nếu mỗi bài đều có script lặp lại được, bạn sẽ bắt đầu làm việc như một backend dev thật, không còn kiểu sửa mò nữa.
