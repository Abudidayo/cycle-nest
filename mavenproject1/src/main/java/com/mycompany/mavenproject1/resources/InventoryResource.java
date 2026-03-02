package com.mycompany.mavenproject1.resources;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.cyclenest.db.MongoConnection;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

@Path("inventory")
public class InventoryResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllItems(
            @QueryParam("db") String dbName,
            @QueryParam("page") @javax.ws.rs.DefaultValue("1") int page) {
        try {
            final int PAGE_SIZE = 16;
            
            String targetDb = (dbName != null && !dbName.isEmpty()) ? dbName : "large_sample";
            
            MongoDatabase db = MongoConnection.getDatabase(targetDb);
            MongoCollection<Document> collection = db.getCollection("Cycle_nest_items");
            
            // Calculate pagination
            long total = collection.countDocuments();
            int totalPages = (int) Math.ceil((double) total / PAGE_SIZE);
            int skip = (page - 1) * PAGE_SIZE;
            
            List<Document> items = new ArrayList<>();
            FindIterable<Document> docs = collection.find().skip(skip).limit(PAGE_SIZE);
            
            for (Document doc : docs) {
                // Convert MongoDB ObjectId to its hex string representation to ensure correct JSON serialization
                Object idObj = doc.get("_id");
                if (idObj instanceof ObjectId) {
                    doc.put("_id", ((ObjectId) idObj).toHexString());
                }
                items.add(doc);
            }
            
            // Construct response object
            Document response = new Document();
            response.append("items", items);
            response.append("total", total);
            response.append("totalPages", totalPages);
            response.append("currentPage", page);
            
            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.serverError().entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }
}
