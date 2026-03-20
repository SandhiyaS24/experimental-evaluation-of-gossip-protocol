package org.ncsu.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.logging.Logger;
import org.ncsu.entity.Action;
import org.ncsu.entity.ActionRecord;
import org.ncsu.respository.ActionRecordRepository;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class ActionService {

    @Inject
    ActionRecordRepository actionRecordRepository;

    private static final Logger LOG = Logger.getLogger(ActionService.class);


    public Boolean initiateAction(List<ActionRecord> actionRecords, Action action) {

        try {

            for (ActionRecord actionRecord : actionRecords) {
                NodeManagerService nodeManagerService = RestClientBuilder.newBuilder()
                        .baseUri(URI.create("http://" + actionRecord.getNodeAddress() + ":" + actionRecord.getManagerPort()))
                        .build(NodeManagerService.class);

                LOG.info("Initiate action record: " + actionRecord.getTopic());

                Response response = null;

                try {
                    if (action == Action.KILL) {
                        response = nodeManagerService.killNode(actionRecord.getActionPort());
                    } else {
                        response = nodeManagerService.startNode(actionRecord.getNodeAddress(), actionRecord.getActionPort(), actionRecord.getPeers(),
                                actionRecord.getKafkaBroker(), actionRecord.getTopic(), actionRecord.getStrategy().toString());
                    }
                } catch (WebApplicationException e) {
                    LOG.error(e);
                    response = e.getResponse();
                }
                LOG.info("1");
                if (response.getStatus() != Response.Status.OK.getStatusCode()) {

                    LOG.error("Error while trying to act on node: " + actionRecord.getNodeAddress() + ":" + actionRecord.getManagerPort()
                            + ", Action port: " + actionRecord.getActionPort() + ", Status: " + response.getStatus());
                }
                LOG.info("2");
                Map<String, Object> responseBody = response.readEntity(Map.class);
//                System.out.println("Raw Server Response: " + rawBody);
//                String rawJson = response.readEntity(String.class);
//                Map<String, String> responseBody = objectMapper.readValue(rawJson, new TypeReference<Map<String, String>>() {});
                LOG.info(responseBody);
                String timestampStr = (String) responseBody.get("timestamp");
                Instant instant = Instant.parse(timestampStr);
                actionRecord.setTimestamp(LocalDateTime.ofInstant(instant, ZoneId.systemDefault()));
                LOG.info("3");
                actionRecord.setStatus(response.getStatus());
                actionRecord.setAction(action);

                LOG.info("4");
                boolean isSaved = actionRecordRepository.save(actionRecord);

                if (!isSaved) {
                    LOG.error("Error while trying to save action on node: " + actionRecord.getNodeAddress() + ":"
                            + actionRecord.getManagerPort() + ", Action port: " + actionRecord.getActionPort()
                            + ", Status: " + response.getStatus() + ", Action: " + actionRecord.getAction());
                    return false;
                }
                LOG.info("Action record: " + actionRecord.getTopic());
            }

            return true;
        } catch (Exception e) {
            LOG.error("Exception: " + e);
            return false;
        }
    }
}
