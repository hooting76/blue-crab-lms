-- Facility Reservation System Schema (v1.0)
-- Generated based on facility-reservation-final-plan.md (v2.1) and implementation-ready notes (v2.2)
-- Target DB: MySQL 8.0+
-- Character Set: utf8mb4
-- Collation: utf8mb4_general_ci
--
-- Prerequisites:
--   - Existing USER_TBL and ADMIN_TBL tables referenced by business logic (not created here)
--   - Application is responsible for enforcing enum constraints via ReservationStatus / FacilityType in code

SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================================
-- 1. FACILITY_TBL
-- ---------------------------------------------------------------------------
--  - Stores facility metadata (study rooms, seminar rooms, gym, etc.)
--  - facility_type is stored as VARCHAR to align with ReservationStatus enum
--  - is_active is treated as boolean flag (1 = active, 0 = inactive)
-- ============================================================================
DROP TABLE IF EXISTS FACILITY_TBL;
CREATE TABLE FACILITY_TBL (
    FACILITY_IDX        INT             GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    FACILITY_NAME       VARCHAR(100)    NOT NULL,
    FACILITY_TYPE       VARCHAR(20)     NOT NULL,
    FACILITY_DESC       TEXT,
    CAPACITY            INT,
    LOCATION            VARCHAR(200),
    DEFAULT_EQUIPMENT   TEXT,
    IS_ACTIVE           TINYINT         NOT NULL DEFAULT 1,
    CREATED_AT          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UPDATED_AT          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_facility_type_active ON FACILITY_TBL (FACILITY_TYPE, IS_ACTIVE);
CREATE INDEX idx_facility_name ON FACILITY_TBL (FACILITY_NAME);

-- ============================================================================
-- 2. FACILITY_RESERVATION_TBL
-- ---------------------------------------------------------------------------
--  - Stores reservation requests for each facility
--  - status is managed by ReservationStatus enum in the application layer
--  - approved_by references admin code (string)
-- ============================================================================
DROP TABLE IF EXISTS FACILITY_RESERVATION_TBL;
CREATE TABLE FACILITY_RESERVATION_TBL (
    RESERVATION_IDX     INT             GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    FACILITY_IDX        INT             NOT NULL,
    USER_CODE           VARCHAR(50)     NOT NULL,
    START_TIME          DATETIME        NOT NULL,
    END_TIME            DATETIME        NOT NULL,
    PARTY_SIZE          INT,
    PURPOSE             TEXT,
    REQUESTED_EQUIPMENT TEXT,
    STATUS              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    ADMIN_NOTE          TEXT,
    REJECTION_REASON    TEXT,
    APPROVED_BY         VARCHAR(50),
    APPROVED_AT         DATETIME,
    CREATED_AT          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UPDATED_AT          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reservation_facility FOREIGN KEY (FACILITY_IDX) REFERENCES FACILITY_TBL (FACILITY_IDX)
);

CREATE INDEX idx_reservation_facility_time ON FACILITY_RESERVATION_TBL (FACILITY_IDX, START_TIME, END_TIME);
CREATE INDEX idx_reservation_user_status ON FACILITY_RESERVATION_TBL (USER_CODE, STATUS);
CREATE INDEX idx_reservation_status_time ON FACILITY_RESERVATION_TBL (STATUS, START_TIME);
CREATE INDEX idx_reservation_created ON FACILITY_RESERVATION_TBL (CREATED_AT);

-- ============================================================================
-- 3. FACILITY_RESERVATION_LOG
-- ---------------------------------------------------------------------------
--  - Audit trail for reservation lifecycle (creation, approval, rejection, cancel, etc.)
--  - payload stores JSON snapshot or metadata about the event
-- ============================================================================
DROP TABLE IF EXISTS FACILITY_RESERVATION_LOG;
CREATE TABLE FACILITY_RESERVATION_LOG (
    LOG_IDX             INT             GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    RESERVATION_IDX     INT             NOT NULL,
    EVENT_TYPE          VARCHAR(50)     NOT NULL,
    ACTOR_TYPE          VARCHAR(20),
    ACTOR_CODE          VARCHAR(50),
    PAYLOAD             TEXT,
    CREATED_AT          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_log_reservation FOREIGN KEY (RESERVATION_IDX) REFERENCES FACILITY_RESERVATION_TBL (RESERVATION_IDX)
);

CREATE INDEX idx_reservationlog_reservation ON FACILITY_RESERVATION_LOG (RESERVATION_IDX);
CREATE INDEX idx_reservationlog_created ON FACILITY_RESERVATION_LOG (CREATED_AT);

SET FOREIGN_KEY_CHECKS = 1;
