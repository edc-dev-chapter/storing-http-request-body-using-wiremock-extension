package com.veggies.shop;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class InvoiceWriter {

    private final String INVOICE_CONTENT_TEMPLATE =
            "A new Invoice for the Order Number: %s has been successfully submitted.";
    private boolean appendToFile = false;
    private String path;

    public InvoiceWriter(String path) {
        this.path = path;
    }

    public void write(String orderNumber) throws IOException {
        FileWriter fileWriter = new FileWriter(path, appendToFile);
        PrintWriter printWriter = new PrintWriter(fileWriter);
        printWriter.printf(INVOICE_CONTENT_TEMPLATE, orderNumber);
        printWriter.close();
    }
}
