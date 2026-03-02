package com.mycompany.mavenproject1.resources;

import com.cyclenest.db.MongoConnection;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Path("item-search")
public class ItemSearchResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchItems(
            @QueryParam("query") String query,
            @QueryParam("category") String category,
            @QueryParam("minPrice") Double minPrice,
            @QueryParam("maxPrice") Double maxPrice,
            @QueryParam("db") String dbName) {

        try {
            String targetDb = (dbName != null && !dbName.isEmpty()) ? dbName : "large_sample";
            MongoDatabase db = MongoConnection.getDatabase(targetDb);
            MongoCollection<Document> collection = db.getCollection("Cycle_nest_items");

            List<Bson> filters = new ArrayList<>();

            if (query != null && !query.trim().isEmpty()) {
                Pattern regex = Pattern.compile(query, Pattern.CASE_INSENSITIVE);
                filters.add(Filters.or(
                        Filters.regex("Name", regex),
                        Filters.regex("name", regex),
                        Filters.regex("Description", regex),
                        Filters.regex("description", regex)
                ));
            }

            if (category != null && !category.trim().isEmpty()) {
                Pattern catRegex = Pattern.compile("^" + category + "$", Pattern.CASE_INSENSITIVE);
                filters.add(Filters.or(
                        Filters.regex("Category", catRegex),
                        Filters.regex("category", catRegex)
                ));
            }

            if (minPrice != null) {
                filters.add(Filters.or(
                    Filters.gte("Daily_Rate", minPrice),
                    Filters.gte("daily_rate", minPrice)
                ));
            }
            
            if (maxPrice != null) {
                filters.add(Filters.or(
                    Filters.lte("Daily_Rate", maxPrice),
                    Filters.lte("daily_rate", maxPrice)
                ));
            }

            filters.add(Filters.or(
                Filters.eq("Available", true),
                Filters.eq("available", true),
                Filters.eq("Available", "true"),
                Filters.eq("available", "true")
            ));

            Bson finalFilter = filters.isEmpty() ? new Document() : Filters.and(filters);
            FindIterable<Document> docs = collection.find(finalFilter);

            List<Document> items = new ArrayList<>();
            for (Document doc : docs) {
                Object idObj = doc.get("_id");
                if (idObj instanceof ObjectId) {
                    doc.put("_id", ((ObjectId) idObj).toHexString());
                }
                items.add(doc);
            }

            return Response.ok(items).build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity("{\"error\": \"" + e.getMessage() + "\"}").build();
        }
    }
}
