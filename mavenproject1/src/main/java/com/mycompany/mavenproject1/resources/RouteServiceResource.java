package com.mycompany.mavenproject1.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.cyclenest.db.MongoConnection;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import static com.mongodb.client.model.Filters.eq;

@Path("route-service")
public class RouteServiceResource {

    public RouteServiceResource() {
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRoute(
            @QueryParam("startLon") String startLon,
            @QueryParam("startLat") String startLat,
            @QueryParam("endLon") String endLon,
            @QueryParam("endLat") String endLat) {

        if (startLon == null || startLat == null || endLon == null || endLat == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Please provide startLon, startLat, endLon, and endLat query parameters.\"}")
                    .build();
        }

        try {
            String jsonResponse = fetchRouteFromOsrm(startLon, startLat, endLon, endLat);
            if (jsonResponse != null) {
                return Response.ok(jsonResponse).build();
            } else {
                return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                        .entity("{\"error\": \"Failed to fetch route from OSRM API.\"}")
                        .build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    @GET
    @Path("calculate")
    @Produces(MediaType.APPLICATION_JSON)
    public Response calculateDistance(
            @QueryParam("userLat") String userLat,
            @QueryParam("userLon") String userLon,
            @QueryParam("itemId") String itemId,
            @QueryParam("db") String dbName) {

        if (userLat == null || userLon == null || itemId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Missing required parameters: userLat, userLon, itemId\"}")
                    .build();
        }

        try {
            // Establish connection to the specified MongoDB database (defaults to large_sample)
            String targetDb = (dbName != null && !dbName.isEmpty()) ? dbName : "large_sample";
            MongoDatabase db = MongoConnection.getDatabase(targetDb);
            MongoCollection<Document> collection = db.getCollection("Cycle_nest_items");

            // Look up the item using either the custom numeric 'ID' field or the standard MongoDB '_id'
            Document item = null;
            
            try {
                int idVal = Integer.parseInt(itemId);
                item = collection.find(eq("ID", idVal)).first();
            } catch (NumberFormatException e) {
                // If ID is not numeric, attempt string match
                item = collection.find(eq("ID", itemId)).first();
            }
            
            if (item == null && ObjectId.isValid(itemId)) {
                try {
                    item = collection.find(eq("_id", new ObjectId(itemId))).first();
                } catch (IllegalArgumentException e) {
                    // Invalid ObjectId format, ignore
                }
            }
            
            if (item == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\": \"Item not found with ID: " + itemId + "\"}")
                        .build();
            }

            Object itemLatObj = getCaseInsensitive(item, "Latitude", "latitude", "lat", "Lat");
            Object itemLonObj = getCaseInsensitive(item, "Longitude", "longitude", "lon", "Lon");
            
            if (itemLatObj == null || itemLonObj == null) {
                 return Response.status(Response.Status.BAD_REQUEST)
                         .entity("{\"error\": \"Item " + itemId + " does not have valid Latitude/Longitude in database.\"}")
                         .build();
            }

            String itemLat = String.valueOf(itemLatObj);
            String itemLon = String.valueOf(itemLonObj);

            // query OSRM service to calculate driving distance between user and item
            String jsonResponse = fetchRouteFromOsrm(userLon, userLat, itemLon, itemLat);
            
            if (jsonResponse != null) {
                return Response.ok(jsonResponse).build();
            } else {
                return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                        .entity("{\"error\": \"Failed to calculate route via OSRM.\"}")
                        .build();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity("{\"error\": \"" + e.getMessage() + "\"}").build();
        }
    }

    public static String fetchRouteFromOsrm(String startLon, String startLat, String endLon, String endLat) {
        Client client = ClientBuilder.newClient();
        String osrmUrl = String.format("http://router.project-osrm.org/route/v1/driving/%s,%s;%s,%s?overview=false",
                startLon, startLat, endLon, endLat);

        try {
            Response osrmResponse = client.target(osrmUrl)
                    .request(MediaType.APPLICATION_JSON)
                    .get();

            if (osrmResponse.getStatus() == 200) {
                return osrmResponse.readEntity(String.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            client.close();
        }
        return null;
    }

    private Object getCaseInsensitive(Document doc, String... keys) {
        for (String key : keys) {
            if (doc.containsKey(key)) {
                return doc.get(key);
            }
        }
        return null;
    }
}
