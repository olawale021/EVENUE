package com.example.evenue.models.tickets;



import com.example.evenue.models.tickets.TicketModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketDao extends JpaRepository<TicketModel, Long> {
    Optional<TicketModel> findByTicketCode(String ticketCode);

    List<TicketModel> findByEventId(Long eventId);

    List<TicketModel> findByUserId(int user_id);

    List<TicketModel> findByEventIdAndTicketTypeId(Long eventId, Long ticketTypeId);
}
