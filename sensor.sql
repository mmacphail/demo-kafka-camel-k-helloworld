CREATE TABLE sensor(
  measure_id  INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
  sensor_id   VARCHAR NOT NULL,
  name        VARCHAR NOT NULL,
  temperature INTEGER not null,
  x           INTEGER not null,
  y           INTEGER not null
)