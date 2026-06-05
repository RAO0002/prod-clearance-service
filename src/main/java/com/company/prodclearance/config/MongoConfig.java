package com.company.prodclearance.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * MongoDB configuration and repository scanning.
 */
@Configuration
@EnableMongoRepositories(basePackages = "com.company.prodclearance.repository")
public class MongoConfig {

}