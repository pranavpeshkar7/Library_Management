-- Dummy data for library_db. Run schema.sql first.
-- Login passwords (plaintext, for testing only):
--   Librarian : admin@library.com     / admin123
--   Student   : asha.mehta@student.spu.edu   / student123
--   Student   : rohan.iyer@student.spu.edu   / student123
--   Teacher   : priya.kapoor@faculty.spu.edu / teacher123

USE library_db;

INSERT INTO users (id, name, email, password_hash, role, active) VALUES
('u-admin-1',   'Meera Admin',   'admin@library.com',            'PkLTbiOTRl9MQPOMYlRDuQ==:Fm0G3x1N1Ohfl97wmXbA+cZnvMNF0ak0iP9V1uJMInA=', 'LIBRARIAN', TRUE),
('u-student-1', 'Asha Mehta',    'asha.mehta@student.spu.edu',   'nbSZumT3QzUSMObyEQqu3w==:TvUssxJzkFFAwm2hz1MKZek0XvKRXmZtycpWGWX8j2c=', 'STUDENT',   TRUE),
('u-student-2', 'Rohan Iyer',    'rohan.iyer@student.spu.edu',   'xLcdzIvDYgPvAZn58RUzHg==:znyExXwCxVoRbAXztN5LLGT9M55f5+OR5Z7TucikdcU=', 'STUDENT',   TRUE),
('u-teacher-1', 'Priya Kapoor',  'priya.kapoor@faculty.spu.edu', 'GdWGCqg/iN0XIbmVsxu+1A==:2+1DS4YdT2/vEGdjV4BbXKWy7xAedis8GqhUTvJYVAA=', 'TEACHER',   TRUE);

INSERT INTO resources (id, type, title, author, total_copies, available_copies) VALUES
('r-book-1', 'BOOK', 'Clean Code',                         'Robert C. Martin',   3, 2),
('r-book-2', 'BOOK', 'Effective Java',                      'Joshua Bloch',       2, 2),
('r-book-3', 'BOOK', 'Design Patterns',                     'Gang of Four',       2, 1),
('r-book-4', 'BOOK', 'Java Concurrency in Practice',        'Brian Goetz',        1, 1),
('r-book-5', 'BOOK', 'Introduction to Algorithms',          'Cormen et al.',      2, 2),
('r-dvd-1',  'DVD',  'The Social Network',                  'David Fincher',      1, 1),
('r-dvd-2',  'DVD',  'The Imitation Game',                  'Morten Tyldum',      1, 1);

-- One active borrow (Asha currently holds a copy of Clean Code)
INSERT INTO borrow_records (id, user_id, resource_id, borrowed_on, due_date, returned_on) VALUES
('br-1', 'u-student-1', 'r-book-1', CURDATE() - INTERVAL 3 DAY, CURDATE() + INTERVAL 11 DAY, NULL);

-- One completed borrow (Rohan already returned Design Patterns)
INSERT INTO borrow_records (id, user_id, resource_id, borrowed_on, due_date, returned_on) VALUES
('br-2', 'u-student-2', 'r-book-3', CURDATE() - INTERVAL 20 DAY, CURDATE() - INTERVAL 6 DAY, CURDATE() - INTERVAL 8 DAY);
