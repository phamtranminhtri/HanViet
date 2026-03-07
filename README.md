# Tiện ích OCR chữ Hán và phiên âm, dịch nghĩa

* **Live demo:** https://huggingface.co/spaces/phamtranminhtri/hanviet

> *Lưu ý*: Ứng dụng trong quá trình phát triển nên vẫn còn sơ sài, nhiều lỗi...

## Tổng quát

Tiện ích này là 1 ứng dụng Spring Boot kết hợp Thymeleaf để tạo 1 trang web đơn giản. Người dùng sẽ tải lên 1 hình ảnh 
có chứa chữ Hán (chẳng hạn như hình chụp cổng chùa, đền, các bức tranh,...), hệ thống sẽ xử lý và trả về: chữ Hán, phiên 
âm Hán Việt, nghĩa tạm dịch. Project này:

* Sử dụng Spring Boot, một framework Java, để xây dựng backend, bao gồm các API endpoint xử lý các request từ browser
* Sử dụng Thymeleaf để trả về các trang HTML tối giản hiển thị phía browser
* Hình ảnh được thu nhỏ để kích thước giảm còn dưới 1 MB và lưu trong thư mục trên hệ thống, đồng thời lưu id trong database H2
* Hình ảnh sau đó gửi qua API của [OCR Space](https://ocr.space/) để trích
Hán tự
* Các Hán tự sau đó được xử lý và hệ thống gửi request đến [Từ điển Thi Viện](https://hvdic.thivien.net/) để lấy phiên âm Hán Việt.
* Các Hán tự cũng được gửi đến API của [MyMemory](https://mymemory.translated.net/) để dịch nghĩa từ Trung sang Việt.

Kết quả OCR và dịch thuật có thể vẫn chưa được chính xác hoàn toàn, vì dự án sử dụng hoàn toàn là API miễn phí. Và vì 
là API miễn phí nên quá trình OCR có thể tốn đến 60s, thậm chí timeout.


## Cách chạy code

### Chạy local

* Nếu sử dụng Linux:
    ```shell
    ./gradlew bootRun
    ```
  
* Nếu sử dụng Windows:
    ```shell
    .\gradlew.bat bootRun
    ```
* Hoặc có thể sử dụng GUI của VSCode, Intellij,...

Sau đó truy cập http://localhost:8080/

### Dùng Docker

1. Cài đặt Docker Desktop

2. Build image:

    ```shell
    docker build -t hanviet .
    ```
3. Run container:

    ```shell
    docker run -p 8080:8080 hanviet
    ```

4. Sau đó truy cập http://localhost:8080/