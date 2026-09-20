package com.pranav.library.service;

import com.pranav.library.exceptions.*;
import com.pranav.library.model.*;
import com.pranav.library.repository.BorrowRecordRepository;
import com.pranav.library.repository.ResourceRepository;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The concurrency core of the project.
 *
 * Why a lock per resource ID rather than one lock for the whole manager:
 * two threads borrowing DIFFERENT books should never block each other —
 * only concurrent attempts on the SAME book need to be serialized.
 * A single global lock would be simpler but would turn every borrow
 * across the entire catalog into a queue of one, which is both wrong
 * (over-serializes unrelated operations) and a bad demo of the concept.
 *
 * The lock guards the read-check-write sequence:
 *   1. read availableCopies
 *   2. check > 0
 *   3. decrement + persist
 * Without the lock, two threads can both pass step 2 before either
 * reaches step 3 — the classic check-then-act race condition — and the
 * available count can go negative under real concurrent load.
 */
public class ResourceManager {

    private final ResourceRepository resourceRepository;
    private final BorrowRecordRepository borrowRecordRepository;

    // One lock per resource id, created lazily and kept for the process lifetime.
    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public ResourceManager(ResourceRepository resourceRepository, BorrowRecordRepository borrowRecordRepository) {
        this.resourceRepository = resourceRepository;
        this.borrowRecordRepository = borrowRecordRepository;
    }

    private ReentrantLock lockFor(String resourceId) {
        return locks.computeIfAbsent(resourceId, id -> new ReentrantLock());
    }

    public BorrowRecord borrow(User user, String resourceId) throws Exception {
        if (user.getMaxBorrowLimit() == 0) {
            throw new AuthorizationException("This account type cannot borrow resources");
        }

        ReentrantLock lock = lockFor(resourceId);
        lock.lock();
        try {
            LibraryResource resource = resourceRepository.findById(resourceId)
                    .orElseThrow(() -> new ResourceNotAvailableException(resourceId));

            if (resource.getAvailableCopies() <= 0) {
                throw new ResourceNotAvailableException(resourceId);
            }

            int activeCount = borrowRecordRepository.countActiveByUser(user.getId());
            if (activeCount >= user.getMaxBorrowLimit()) {
                throw new MaxBorrowLimitExceededException(user.getId(), user.getMaxBorrowLimit());
            }

            // Critical section: decrement in-memory, persist, THEN record the borrow.
            resource.decrementAvailable();
            resourceRepository.updateAvailableCopies(resourceId, resource.getAvailableCopies());

            LocalDate today = LocalDate.now();
            BorrowRecord record = new BorrowRecord(
                    UUID.randomUUID().toString(),
                    user.getId(),
                    resourceId,
                    today,
                    today.plusDays(resource.getBorrowDurationDays())
            );
            return borrowRecordRepository.save(record);
        } finally {
            lock.unlock();
        }
    }

    public BorrowRecord returnResource(User user, String resourceId) throws Exception {
        ReentrantLock lock = lockFor(resourceId);
        lock.lock();
        try {
            BorrowRecord record = borrowRecordRepository
                    .findActiveByUserAndResource(user.getId(), resourceId)
                    .orElseThrow(() -> new InvalidReturnException(user.getId(), resourceId));

            record.markReturned(LocalDate.now());
            borrowRecordRepository.save(record);

            LibraryResource resource = resourceRepository.findById(resourceId)
                    .orElseThrow(() -> new ResourceNotAvailableException(resourceId));
            resource.incrementAvailable();
            resourceRepository.updateAvailableCopies(resourceId, resource.getAvailableCopies());

            return record;
        } finally {
            lock.unlock();
        }
    }
}
