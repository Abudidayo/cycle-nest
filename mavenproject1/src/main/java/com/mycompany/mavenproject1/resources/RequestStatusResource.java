package com.mycompany.mavenproject1.resources;

import com.cyclenest.db.MongoConnection;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Path("status")
public class RequestStatusResource {

    private static final String DB_NAME = "large_sample";
    private static final String COLLECTION_NAME = "Item_Requests";

    @POST
    @Path("toggle")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response toggleRequest(Map<String, String> payload) {
        String itemId = payload.get("item_id");
        
        if (itemId == null || itemId.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"item_id is required\"}").build();
        }

        try {
            MongoDatabase db = MongoConnection.getDatabase(DB_NAME);
            MongoCollection<Document> collection = db.getCollection(COLLECTION_NAME);

            // Use the item's unique hex ID as the primary key for the request
            Bson filter = Filters.eq("_id", itemId);
            Document existing = collection.find(filter).first();

            String newStatus;
            if (existing != null && "Pending".equals(existing.getString("status"))) {
                newStatus = "Cancelled";
            } else {
                newStatus = "Pending";
            }

            // Fetch item details just to get the correct 'item_id'
            MongoCollection<Document> itemsCol = db.getCollection("Cycle_nest_items");
            Document itemDetails = null;
            
            if (ObjectId.isValid(itemId)) {
                itemDetails = itemsCol.find(Filters.eq("_id", new ObjectId(itemId))).first();
            }
            if (itemDetails == null) {
                itemDetails = itemsCol.find(Filters.eq("ID", itemId)).first();
            }
            
            // Extract the readable item_id (e.g., "i00001") from the source document
            Object readableId = null;
            if (itemDetails != null) {
                readableId = itemDetails.get("item_id");
                if (readableId == null) readableId = itemDetails.get("ID");
            }

            // Prepare the update with status, timestamp, and the readable item_id
            List<Bson> updateStages = new ArrayList<>();
            updateStages.add(Updates.set("status", newStatus));
            updateStages.add(Updates.set("lastUpdated", new Date()));
            
            if (readableId != null) {
                updateStages.add(Updates.set("item_id", readableId));
            }

            collection.updateOne(filter, Updates.combine(updateStages), new UpdateOptions().upsert(true));

            return Response.ok("{\"status\": \"" + newStatus + "\", \"item_id\": \"" + itemId + "\"}").build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity("{\"error\": \"" + e.getMessage() + "\"}").build();
        }
    }

    @GET
    @Path("all")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllActiveRequests() {
        try {
            MongoDatabase db = MongoConnection.getDatabase(DB_NAME);
            MongoCollection<Document> collection = db.getCollection(COLLECTION_NAME);

            List<Document> results = new ArrayList<>();
            for (Document doc : collection.find()) {
                Document cleanDoc = new Document();
                cleanDoc.append("item_id", doc.get("_id"));
                cleanDoc.append("status", doc.getString("status"));
                results.add(cleanDoc);
            }

            return Response.ok(results).build();

        } catch (Exception e) {
            return Response.serverError().entity("{\"error\": \"" + e.getMessage() + "\"}").build();
        }
    }
}
