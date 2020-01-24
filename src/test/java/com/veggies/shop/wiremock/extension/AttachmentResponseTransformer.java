package com.veggies.shop.wiremock.extension;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@Slf4j
public class AttachmentResponseTransformer extends ResponseDefinitionTransformer {

    private static final boolean APPLY_GLOBALLY = false;
    private static final String FILE_PATH = "target/";
    private static final String KEY = "uploadedAttachment";
    private static final String NAME = "attachment";

    @Override
    public boolean applyGlobally() {
        return APPLY_GLOBALLY;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public ResponseDefinition transform(Request request, ResponseDefinition response, FileSource files,
                                        Parameters parameters) {
        log.info("Saving uploaded attachment");
        saveUploadedAttachment(request);

        return new ResponseDefinitionBuilder().build();
    }

    private String filenameFrom(Request.Part part) {
        return ContentDisposition.parse(part.getHeader(HttpHeaders.CONTENT_DISPOSITION).firstValue()).getFilename();
    }

    private void saveUploadedAttachment(Request request) {
        Request.Part attachment = request.getPart(KEY);
        byte[] workbookBytes = attachment.getBody().asBytes();

        String generatedWorkbookPath = FILE_PATH + filenameFrom(attachment);
        File generatedWorkbook = new File(generatedWorkbookPath);

        try (FileOutputStream outputStream = new FileOutputStream(generatedWorkbook)) {
            log.info("Writing to file {}", generatedWorkbookPath);
            outputStream.write(workbookBytes);
        } catch (IOException ex) {
            log.warn("Exception during file writing", ex);
        }
    }
}
