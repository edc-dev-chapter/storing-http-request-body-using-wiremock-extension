package com.veggies.shop;

import lombok.EqualsAndHashCode;
import lombok.Getter;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@AutoConfigureWireMock(port = 9080)
@SpringBootTest
@TestInstance(PER_CLASS)
public class VeggiesShopIT {

    private static final String API_PARAM_ATTACHMENT_KEY = "attachmentStoreKey";
    private static final String API_PARAM_UPLOADED_ATTACHMENT = "uploadedAttachment";
    private static final String ATTACHMENT_FILE_DIRECTORY = "target/";
    private static final String ATTACHMENT_NAME = "Invoice_%s.txt";
    private static final String ATTACHMENT_SERVICE_ENDPOINT = "http://localhost:9080/api/invoices/";
    private static final String ORDER_NUMBER = "1234";
    private static final String TEMPLATE_FILE_PATH = "src/test/binaries/templates/veggies-shop.txt";

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
    void testGenerateInvoice() throws IOException {
        // Given
        String key = String.format(ATTACHMENT_NAME, ORDER_NUMBER);
        InvoiceWriter invoiceWriter = new InvoiceWriter(TEMPLATE_FILE_PATH);
        invoiceWriter.write(ORDER_NUMBER);
        byte[] invoiceBytes = Files.readAllBytes(Paths.get(TEMPLATE_FILE_PATH));

        // When
        postMultipartFileFor(invoiceBytes, ORDER_NUMBER, key);

        String generatedFilePath = ATTACHMENT_FILE_DIRECTORY + key;
        File generatedInvoice = new File(generatedFilePath);
        BufferedReader reader = new BufferedReader(new FileReader(generatedInvoice));
        String actualInvoiceContent = reader.readLine();
        String expectedInvoiceContent =
                "A new Invoice for the Order Number: 1234 has been successfully submitted.";

        // Then
        assertEquals(expectedInvoiceContent, actualInvoiceContent);

        generatedInvoice.delete();
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
