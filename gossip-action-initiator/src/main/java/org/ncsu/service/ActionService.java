package org.ncsu.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.logging.Logger;
import org.ncsu.entity.Action;
import org.ncsu.entity.ActionRecord;
import org.ncsu.respository.ActionRecordRepository;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.List;

@ApplicationScoped
public class ActionService {

    @Inject
    ActionRecordRepository actionRecordRepository;

    private static final Logger LOG = Logger.getLogger(ActionService.class);


    public Boolean initiateAction(List<ActionRecord> actionRecords, Action action) {

        try {

            for (ActionRecord actionRecord : actionRecords) {
                NodeManagerService nodeManagerService = RestClientBuilder.newBuilder()
                        .baseUri(URI.create(actionRecord.getNodeAddress() + actionRecord.getManagerPort()))
                        .build(NodeManagerService.class);

                Response response = null;

                if(action == Action.KILL) {
                    response = nodeManagerService.killNode(actionRecord.getActionPort());
                } else {
                    response = nodeManagerService.startNode(actionRecord.getActionPort(), actionRecord.getPeer(),
                            actionRecord.getKafkaBroker(), actionRecord.getTopic());
                }

                if (response.getStatus() != Response.Status.OK.getStatusCode()) {
                    LOG.error("Error while trying to kill node: " + actionRecord.getNodeAddress() + ":" + actionRecord.getManagerPort()
                            + ", Action port: " + actionRecord.getActionPort() + ", Status: " + response.getStatus());
                    actionRecord.setTimestamp(null);
                }
                else {
                    actionRecord.setTimestamp(ZonedDateTime.parse(response.getHeaderString("timestamp")).toLocalDateTime());
                }
                actionRecord.setStatus(response.getStatus());
                actionRecord.setAction(action);


                boolean isSaved = actionRecordRepository.save(actionRecord);

                if (!isSaved) {
                    LOG.error("Error while trying to save action on node: " + actionRecord.getNodeAddress() + ":"
                            + actionRecord.getManagerPort() + ", Action port: " + actionRecord.getActionPort()
                            + ", Status: " + response.getStatus() + ", Action: " + actionRecord.getAction());
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            LOG.error(e);
            return false;
        }
    }
}
