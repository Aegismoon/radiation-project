CREATE TABLE IF NOT EXISTS radiation_measurements (
  id           BIGSERIAL PRIMARY KEY,
  source_id    TEXT NOT NULL,
  value_µsv_h  DOUBLE PRECISION NOT NULL,
  ts_utc       TIMESTAMPTZ NOT NULL,
  severity     TEXT NOT NULL,
  meta         JSONB,
  UNIQUE (source_id, ts_utc)
);
CREATE TABLE IF NOT EXISTS radiation_doses (
  id BIGSERIAL PRIMARY KEY,
  source_id TEXT NOT NULL,
  cumulative_µsv DOUBLE PRECISION NOT NULL,
  ts_utc TIMESTAMPTZ NOT NULL,
  UNIQUE (source_id, ts_utc)
);