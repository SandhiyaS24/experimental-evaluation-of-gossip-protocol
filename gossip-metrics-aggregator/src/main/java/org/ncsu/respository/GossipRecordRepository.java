package org.ncsu.respository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.ncsu.entity.GossipRecord;

import java.util.List;

@ApplicationScoped
public class GossipRecordRepository implements PanacheRepository<GossipRecord> {

    @Transactional
    public boolean save(List<GossipRecord> gossipRecords) {
        persist(gossipRecords);
        return true;
    }

}
