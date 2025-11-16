package com.nyy.gmail.cloud.common.configuration;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.nyy.gmail.cloud.common.pagination.MongoPageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.*;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Configuration
public class MongoConfig extends AbstractMongoClientConfiguration {

    @Value("${mongo.db}")
    private String db;

    @Value("${mongo.host}")
    private String host;

    @Value("${mongo.port:27017}")
    private String port;

    @Override
    protected String getDatabaseName() {
        return db;
    }

    @Override
    public MongoClient mongoClient() {
        ConnectionString connectionString = new ConnectionString("mongodb://" + host +":"+port+"/" + db);

        MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .applyToConnectionPoolSettings(builder -> builder
                        .maxSize(30000)
                        .minSize(200)
                        .maxWaitTime(10, TimeUnit.SECONDS)
                )
                .applyToSocketSettings(builder -> builder
                        .connectTimeout(3, TimeUnit.SECONDS)
                        .readTimeout(5, TimeUnit.SECONDS)
                )
                .applyToClusterSettings(builder -> builder
                        .serverSelectionTimeout(3, TimeUnit.SECONDS)
                )
                .build();

        return MongoClients.create(mongoClientSettings);
    }

    private static final Pattern ISO_DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z$");

    @Bean
    public MongoPageHelper mongoPageHelper(MongoTemplate mongoTemplate) {
        return new MongoPageHelper(mongoTemplate);
    }

    @Bean
    public MappingMongoConverter mappingMongoConverter(MongoDatabaseFactory databaseFactory,
                                                       MongoCustomConversions customConversions, MongoMappingContext mappingContext) {
        DbRefResolver dbRefResolver = new DefaultDbRefResolver(databaseFactory);
        MappingMongoConverter converter = new MappingMongoConverter(dbRefResolver, mappingContext);
        //不保存 _class 属性到mongo
        converter.setTypeMapper(new DefaultMongoTypeMapper(null));

        return converter;
    }
}
