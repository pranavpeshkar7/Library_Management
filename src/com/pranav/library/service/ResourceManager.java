package com.pranav.library.service;

import com.pranav.library.exceptions.*;
import com.pranav.library.model.*;
import com.pranav.library.repository.BorrowRecordRepository;
import com.pranav.library.repository.ResourceRepository;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The concurrency core of the project.
 *
 * Two kinds of lock are used:
 *  - a lock PER RESOURCE: protects the read-check-write on availableCopies, so two
 *    threads borrowing DIFFERENT books never block each other.
 *  - a lock PER USER: protects the "has this user reached their limit?" check.
 *    Without it, one user firing parallel requests for different books passes the
 *    limit check on every thread (each holds a different resource lock) and ends up
 *    over the limit.
 *
 * Lock ORDER is always user lock -> resource lock. returnResource / update / remove
 * only ever take a resource lock, so no thread can wait on a user lock while holding
 * a resource lock, which means no deadlock is possible.
 */
public class ResourceManager {

    private final ResourceRepository resourceRepository;
    private final BorrowRecordRepository borrowRecordRepository;

    private final Map<String, ReentrantLock> resourceLocks = new ConcurrentHashMap<>();
    private final Map<String, ReentrantLock> userLocks = new ConcurrentHashMap<>();

    public ResourceManager(ResourceRepository resourceRepository, BorrowRecordRepository borrowRecordRepository) {
        this.resourceRepository = resourceRepository;
        this.borrowRecordRepository = borrowRecordRepository;
    }

    private ReentrantLock lockFor(String resourceId) {
        return resourceLocks.computeIfAbsent(resourceId, id -> new ReentrantLock());
    }

    private ReentrantLock userLockFor(String userId) {
        return userLocks.computeIfAbsent(userId, id -> new ReentrantLock());
    }

    public BorrowRecord borrow(User user, String resourceId) throws Exception {
        if (user.getMaxBorrowLimit() == 0) {
            throw new AuthorizationException("This account type cannot borrow resources");
        }

        ReentrantLock userLock = userLockFor(user.getId());
        ReentrantLock lock = lockFor(resourceId);
        userLock.lock();
        try {
            lock.lock();
            try {
                LibraryResource resource = resourceRepository.findById(resourceId)
                        .orElseThrow(() -> new ResourceNotAvailableException(resourceId));

                if (resource.getAvailableCopies() <= 0) {
                    throw new ResourceNotAvailableException(resourceId);
                }

                if (borrowRecordRepository.findActiveByUserAndResource(user.getId(), resourceId).isPresent()) {
                    throw new AlreadyBorrowedException(resourceId);
                }

                int activeCount = borrowRecordRepository.countActiveByUser(user.getId());
                if (activeCount >= user.getMaxBorrowLimit()) {
                    throw new MaxBorrowLimitExceededException(user.getId(), user.getMaxBorrowLimit());
                }

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
                try {
                    return borrowRecordRepository.save(record);
                } catch (Exception e) {
                    // The copy count was already decremented; put it back so a failed
                    // insert doesn't permanently "lose" a copy.
                    resource.incrementAvailable();
                    resourceRepository.updateAvailableCopies(resourceId, resource.getAvailableCopies());
                    throw e;
                }
            } finally {
                lock.unlock();
            }
        } finally {
            userLock.unlock();
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

    /**
     * Librarian edit of a catalogue item. Runs under the same per-resource lock as borrow/return;
     * previously it did read-modify-write with no lock, so an edit could overwrite a concurrent
     * borrow's decrement and hand out a copy that was already lent.
     * Any field may be null = leave unchanged. Returns empty if the id doesn't exist.
     */
    public Optional<LibraryResource> updateResource(String id, String title, String author, Integer totalCopies) throws Exception {
        ReentrantLock lock = lockFor(id);
        lock.lock();
        try {
            Optional<LibraryResource> existing = resourceRepository.findById(id);
            if (!existing.isPresent()) return Optional.empty();
            LibraryResource resource = existing.get();
            if (title != null) resource.setTitle(title);
            if (author != null) resource.setAuthor(author);
            if (totalCopies != null) resource.setTotalCopies(totalCopies); // may throw IllegalArgumentException
            resourceRepository.save(resource);
            return Optional.of(resource);
        } finally {
            lock.unlock();
        }
    }

    /** Refuses to delete an item that still has copies on loan (the FK is ON DELETE CASCADE and would erase the loan history). */
    public void removeResource(String id) throws Exception {
        ReentrantLock lock = lockFor(id);
        lock.lock();
        try {
            int onLoan = borrowRecordRepository.countActiveByResource(id);
            if (onLoan > 0) throw new ResourceInUseException(id, onLoan);
            resourceRepository.deleteById(id);
        } finally {
            lock.unlock();
        }
    }
}
