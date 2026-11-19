package com.jewelerp.config;

import com.jewelerp.pricing.JewelleryPricing;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PricingConfig {

    @Bean
    JewelleryPricing jewelleryPricing() {
        return new JewelleryPricing();
    }
}
