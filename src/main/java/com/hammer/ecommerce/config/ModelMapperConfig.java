package com.hammer.ecommerce.config;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapperConfig {


    // para converter/mapear objetos de um tipo em outro
    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }
}
