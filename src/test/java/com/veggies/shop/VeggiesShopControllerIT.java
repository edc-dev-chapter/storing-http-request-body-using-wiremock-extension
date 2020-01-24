package com.veggies.shop;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@AutoConfigureWireMock(port = 9080)
@SpringBootTest
@TestInstance(PER_CLASS)
public class VeggiesShopControllerIT {

    private static final String API_PARAM_ATTACHMENT_KEY = "attachmentStoreKey";
    private static final String API_PARAM_UPLOADED_ATTACHMENT = "uploadedAttachment";
    private static final String ATTACHMENT_FILE_DIRECTORY = "target/";
    private static final String ATTACHMENT_NAME = "Invoice_%s.xlsx";
    private static final String ATTACHMENT_SERVICE_ENDPOINT = "http://localhost:9080/api/invoices/";
    private static final String ORDER_NUMBER = "9998889991";
    private static final int ORDER_NUMBER_CELL_INDEX = 1;
    private static final int ORDER_NUMBER_ROW_INDEX = 1;
    private static final String SHEET_NAME = "ORDER INFO";
    //todo change path to /templates/veggies-shop.xlsx -> currently - problems with project settings
    private static final String TEMPLATE_FILE_PATH = "src/test/binaries/templates/veggies-shop.xlsx";

    @TestConfiguration
    static class ControllerITConfiguration {

        @Bean
        public RestTemplate restTemplate() {
            return new RestTemplate();
        }
    }

    @Autowired
    private RestTemplate restTemplate;

    @Test
    void testGenerateInvoice() throws IOException, InvalidFormatException {
        // Given
        String key = String.format(ATTACHMENT_NAME, ORDER_NUMBER);
        File invoice = new File(TEMPLATE_FILE_PATH);
        OPCPackage invoiceOpcPackage = OPCPackage.openOrCreate(invoice);
        XSSFWorkbook invoiceWorkbook = new XSSFWorkbook(invoiceOpcPackage);
        writeInvoiceData(invoiceWorkbook);
        byte[] workbookBytes = asByteArray(invoiceWorkbook);

        //When
        postMultipartFileFor(workbookBytes, ORDER_NUMBER, key);

        String generatedFilePath = ATTACHMENT_FILE_DIRECTORY + key;
        File generatedWorkbook = new File(generatedFilePath);
        OPCPackage opcPackage = OPCPackage.openOrCreate(generatedWorkbook);
        XSSFSheet orderSheet;
        try (XSSFWorkbook workbook = new XSSFWorkbook(opcPackage)) {
            orderSheet = workbook.getSheet(SHEET_NAME);
        }
        String actualOrderCellValue =
                orderSheet.getRow(ORDER_NUMBER_CELL_INDEX).getCell(ORDER_NUMBER_CELL_INDEX).getStringCellValue();

        // Then
        assertEquals(ORDER_NUMBER, actualOrderCellValue);

        generatedWorkbook.delete();
    }

    private void postMultipartFileFor(byte[] attachmentBytes, String orderNumber, String key) throws IOException {
        MultiValueMap<String, Object> multiValueMap = new LinkedMultiValueMap<>();
        multiValueMap.add(API_PARAM_ATTACHMENT_KEY, key);
        multiValueMap.add(API_PARAM_UPLOADED_ATTACHMENT,
                          createByteArrayResource(multipartFileFor(key, attachmentBytes), key));

        URI url = UriComponentsBuilder.fromHttpUrl(ATTACHMENT_SERVICE_ENDPOINT + orderNumber)
                                      .build()
                                      .encode()
                                      .toUri();

        restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(multiValueMap), String.class);
    }

    private void writeInvoiceData(XSSFWorkbook workbook) {
        workbook.getSheet(SHEET_NAME)
                .getRow(ORDER_NUMBER_ROW_INDEX)
                .getCell(ORDER_NUMBER_CELL_INDEX)
                .setCellValue(ORDER_NUMBER);
    }

    private ByteArrayResource createByteArrayResource(MultipartFile uploadedAttachment, String attachmentKey)
            throws IOException {
        return new NamedByteArrayResource(uploadedAttachment.getBytes(), attachmentKey);
    }

    private InvoiceMultipartFile multipartFileFor(String key, byte[] bytes) {
        return InvoiceMultipartFile.builder()
                                   .name(key)
                                   .contentType(InvoiceMultipartFile.MEDIA_TYPE)
                                   .bytes(bytes)
                                   .build();
    }

    private byte[] asByteArray(XSSFWorkbook workbook) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            workbook.write(bos);
            return bos.toByteArray();
        }
    }

    @EqualsAndHashCode(callSuper = false)
    private class NamedByteArrayResource extends ByteArrayResource {

        @Getter
        private String filename;

        private NamedByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename;
        }
    }
}
