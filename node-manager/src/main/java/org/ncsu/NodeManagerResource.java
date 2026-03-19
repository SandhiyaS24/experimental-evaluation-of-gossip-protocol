package org.ncsu;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.ncsu.service.GossipProcessManager;

import java.time.Instant;
import java.util.Map;

@Path("/gossip")
public class NodeManagerResource {

    // Inject the process manager service we created above
    @Inject
    GossipProcessManager processManager;

    @POST
    @Path("/start")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response start(
            @HeaderParam("port") Integer port,
            @HeaderParam("peer") String peer,
            @HeaderParam("kafka-topic") String kafkaTopic,
            @HeaderParam("kafka-broker") String kafkaBroker
    ) {
        if (port == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "action-port header is required", "timestamp", Instant.now().toString()))
                    .build();
        }

        try {
            processManager.startNode(port, peer, kafkaTopic, kafkaBroker);

            return Response.ok(Map.of(
                    "status", "Gossip Node Started",
                    "nodeId", "Node-" + port,
                    "timestamp", Instant.now().toString()
            )).build();

        } catch (IllegalStateException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("error", e.getMessage(), "timestamp", Instant.now().toString()))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("status", "Error executing process", "error", e.getMessage(), "timestamp", Instant.now().toString()))
                    .build();
        }
    }

    @POST
    @Path("/kill")
    @Produces(MediaType.APPLICATION_JSON)
    public Response kill(@HeaderParam("port") Integer port) {
        if (port == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "action-port header is required", "timestamp", Instant.now().toString()))
                    .build();
        }

        boolean wasKilled = processManager.killNode(port);

        if (!wasKilled) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("status", "No active node found on port " + port, "timestamp", Instant.now().toString()))
                    .build();
        }

        return Response.ok(Map.of(
                "status", "Gossip Node Killed",
                "nodeId", "Node-" + port,
                "timestamp", Instant.now().toString()
        )).build();
    }
}