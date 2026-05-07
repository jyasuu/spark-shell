drop table if exists core.target_table;
drop table if exists core.source_table;


CREATE TABLE core.target_table (
  id INT,
  name STRING,
  amount INT,
  update_ts TIMESTAMP
);
INSERT INTO core.target_table VALUES
(1, 'Alice', 100, timestamp('2026-05-01 10:00:00')),
(2, 'Bob',   200, timestamp('2026-05-01 10:00:00')),
(3, 'Charlie', 300, timestamp('2026-05-01 10:00:00'));
SELECT * FROM core.target_table ORDER BY id;


CREATE TABLE core.source_table (
  id INT,
  name STRING,
  amount INT,
  update_ts TIMESTAMP
)
;
INSERT INTO core.source_table VALUES
(3, 'Charlie', 300, timestamp('2026-04-30 09:00:00')), -- older (ignore)
(4, 'David', 400, timestamp('2026-05-02 12:00:00'));  -- new insert
SELECT * FROM core.source_table ORDER BY id;



MERGE INTO core.target_table t
USING core.source_table s
ON t.id = s.id
WHEN MATCHED AND s.update_ts > t.update_ts THEN
  UPDATE SET
    t.name = s.name,
    t.amount = s.amount,
    t.update_ts = s.update_ts
WHEN NOT MATCHED THEN
  INSERT (id, name, amount, update_ts)
  VALUES (s.id, s.name, s.amount, s.update_ts);
SELECT * FROM core.target_table ORDER BY id;


INSERT INTO core.source_table VALUES
(1, 'Alice', 150, timestamp('2026-05-03 09:00:00'));
MERGE INTO core.target_table t
USING core.source_table s
ON t.id = s.id
WHEN MATCHED AND s.update_ts > t.update_ts THEN
  UPDATE SET
    t.name = s.name,
    t.amount = s.amount,
    t.update_ts = s.update_ts
WHEN NOT MATCHED THEN
  INSERT (id, name, amount, update_ts)
  VALUES (s.id, s.name, s.amount, s.update_ts);
SELECT * FROM core.target_table ORDER BY id;


select * from core.source_table.files;
select * from core.source_table.snapshots;
select * from core.target_table.files;
select * from core.target_table.snapshots;
SELECT * FROM core.target_table.all_data_files
