package com.ticket.service;

import com.ticket.domain.*;
import com.ticket.exception.InvalidSeatException;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class TicketingService {
    private final Map<String, Reservation>reservationTable = new ConcurrentHashMap<>();
    private final Map<String, ReservationSeat>seatAssignmentTable = new ConcurrentHashMap<>();
    private final Map<String, ReentrantLock>eventLocks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService backgroundSweeper;

    public TicketingService(){
        this.backgroundSweeper = Executors.newSingleThreadScheduledExecutor(runnable->{
            Thread thread = new Thread(runnable,"Hold-Sweeper-Thread");
            thread.setDaemon(true);
            return thread;
        });
        this.backgroundSweeper.scheduleAtFixedRate(this::purgeExpiredHolds,1,1,TimeUnit.SECONDS);
    }
    private ReentrantLock getLockForEvent(String eventId){
        return eventLocks.computeIfAbsent(eventId, k-> new ReentrantLock());
    }
    public Reservation placeHold(String eventId, Seat seat, String customerEmail,long ttlSeconds){
        ReentrantLock eventLock = getLockForEvent(eventId);
        eventLock.lock();
        try{
            boolean seatIsBlocked = seatAssignmentTable.values().stream()
                    .anyMatch(rs -> rs.seatId().equals(seat.id()) && isReservationActive(rs.reservationId()));

            if(seatIsBlocked) {
                throw new InvalidSeatException("Transaction conflic: Target seat code is currently reserved");
            };

            String resId = "RES-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            Instant now = Instant.now();
            Instant expiresAt = now.plusSeconds(ttlSeconds);

            Reservation hold = new Reservation(resId, eventId, customerEmail, ReservationStatus.HOLD, now, null, expiresAt);
            ReservationSeat holdSeat = new ReservationSeat(resId, seat.id(), Money.of(120.00), "NONE");

            reservationTable.put(resId, hold);
            seatAssignmentTable.put(resId, holdSeat);

            return hold;
        } finally {
            eventLock.unlock();
        }
    }

    private boolean isReservationActive(String reservationId){
        Reservation res = reservationTable.get(reservationId);
        if (res==null) return false;
        if (res.status()== ReservationStatus.CANCELLED) return false;
        return !res.isExpired();
    }
    private void purgeExpiredHolds() {
        reservationTable.values().stream()
                .filter(Reservation::isExpired)
                .forEach(expiredRes -> {
                    Reservation cancelledRes = new Reservation(
                            expiredRes.id(), expiredRes.eventId(), expiredRes.customerEmail(),
                            ReservationStatus.CANCELLED, expiredRes.createdAt(), null, expiredRes.holdExpiresAt()
                    );
                    reservationTable.put(expiredRes.id(), cancelledRes);
                    seatAssignmentTable.remove(expiredRes.id());
                });
    }
    public Map<String, Reservation> getReservationTable() {
        return Collections.unmodifiableMap(reservationTable);
    }
    public void shoutdown(){
        backgroundSweeper.shutdown();
    }
}

