package com.example.client_service.repository;

import com.example.client_service.model.Client;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.Optional;

/**
 * MongoDB has no equivalent of the pessimistic lock event-service takes on a package row:
 * there is no SELECT ... FOR UPDATE to hold between a read and a write. What it does
 * guarantee is that a single-document update is atomic, so the read-modify-write must be
 * expressed as one operation instead of three. $addToSet appends the ticket server-side —
 * and only if the array does not already contain it — which makes the call both safe under
 * concurrency and idempotent, without any locking.
 */
public class ClientRepositoryImpl implements ClientRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    public ClientRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Optional<Client> addTicket(String email, String ticketId) {
        return Optional.ofNullable(mongoTemplate.findAndModify(
                Query.query(Criteria.where("email").is(email)),
                new Update().addToSet("tickets", ticketId),
                FindAndModifyOptions.options().returnNew(true),
                Client.class));
    }
}
