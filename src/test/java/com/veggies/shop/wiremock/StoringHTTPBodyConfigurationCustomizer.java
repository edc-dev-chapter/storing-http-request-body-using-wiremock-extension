package com.veggies.shop.wiremock;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.veggies.shop.wiremock.extension.AttachmentResponseTransformer;
import org.springframework.cloud.contract.wiremock.WireMockConfigurationCustomizer;
import org.springframework.stereotype.Component;

@Component
public class StoringHTTPBodyConfigurationCustomizer implements WireMockConfigurationCustomizer {

    @Override
    public void customize(WireMockConfiguration configuration) {
        configuration.extensions(AttachmentResponseTransformer.class);
    }
}
