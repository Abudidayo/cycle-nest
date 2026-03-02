package com.cyclenest.db;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MongoConnection {

    private static final Logger LOGGER = Logger.getLogger(MongoConnection.class.getName());
    
    // Environment variables for security
    private static final String CONNECTION_STRING = System.getenv("MONGO_CONNECTION_STRING") != null 
            ? System.getenv("MONGO_CONNECTION_STRING") 
            : "mongodb+srv://<username>:<password>@cluster0.example.net/?retryWrites=true&w=majority";
    
    private static final String DEFAULT_DB = System.getenv("MONGO_DATABASE") != null 
            ? System.getenv("MONGO_DATABASE") 
            : "items";
    
    private static MongoClient mongoClient;

    public static MongoDatabase getDatabase() {
        return getDatabase(DEFAULT_DB);
    }

    public static MongoDatabase getDatabase(String dbName) {
        if (mongoClient == null) {
            try {
                ConnectionString connString = new ConnectionString(CONNECTION_STRING);
                MongoClientSettings settings = MongoClientSettings.builder()
                        .applyConnectionString(connString)
                        .build();
                mongoClient = MongoClients.create(settings);
                LOGGER.info("Connected to MongoDB successfully.");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to connect to MongoDB", e);
                throw e;
            }
        }
        return mongoClient.getDatabase(dbName);
    }
    
    public static void close() {
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
        }
    }
}
