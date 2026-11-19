BEGIN;
CREATE TABLE blood_groups (
    code text PRIMARY KEY
);
CREATE TABLE stock (
    blood_group text PRIMARY KEY REFERENCES blood_groups(code),
    units integer NOT NULL DEFAULT 0 CHECK (units >= 0)
);
CREATE TABLE donors (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name text NOT NULL CHECK (trim(name) <> ''),
    age integer NOT NULL CHECK (age > 0),
    contact text NOT NULL CHECK (trim(contact) <> ''),
    blood_group text NOT NULL REFERENCES blood_groups(code)
);
CREATE TABLE donations (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    donor_id bigint NOT NULL REFERENCES donors(id),
    blood_group text NOT NULL REFERENCES blood_groups(code),
    donated_on date NOT NULL,
    units integer NOT NULL CHECK (units > 0)
);
CREATE INDEX donations_donor_date_idx ON donations(donor_id, donated_on DESC);
CREATE TABLE hospital_requests (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    hospital text NOT NULL CHECK (trim(hospital) <> ''),
    blood_group text NOT NULL REFERENCES blood_groups(code),
    units integer NOT NULL CHECK (units > 0),
    requested_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status text NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','ISSUED','CANCELLED')),
    resolved_at timestamptz,
    CHECK ((status = 'PENDING') = (resolved_at IS NULL))
);
CREATE TABLE demo_settings (
    singleton boolean PRIMARY KEY DEFAULT true CHECK (singleton),
    interval_days integer NOT NULL CHECK (interval_days > 0)
);
INSERT INTO demo_settings(interval_days) VALUES (30);
INSERT INTO blood_groups(code) VALUES ('A+'),('A-'),('B+'),('B-'),('AB+'),('AB-'),('O+'),('O-');
INSERT INTO stock(blood_group) SELECT code FROM blood_groups;

CREATE FUNCTION record_donation(p_donor bigint, p_date date, p_units integer)
RETURNS bigint LANGUAGE plpgsql AS $$
DECLARE v_group text; v_id bigint;
BEGIN
    IF p_date > CURRENT_DATE THEN RAISE EXCEPTION 'Donation date cannot be in the future'; END IF;
    SELECT blood_group INTO STRICT v_group FROM donors WHERE id=p_donor FOR UPDATE;
    INSERT INTO donations(donor_id,blood_group,donated_on,units)
      VALUES(p_donor,v_group,p_date,p_units) RETURNING id INTO v_id;
    UPDATE stock SET units=units+p_units WHERE blood_group=v_group;
    IF NOT FOUND THEN RAISE EXCEPTION 'Stock row missing'; END IF;
    RETURN v_id;
END;
$$;

CREATE FUNCTION resolve_request(p_id bigint, p_issue boolean)
RETURNS text LANGUAGE plpgsql AS $$
DECLARE r hospital_requests%ROWTYPE;
BEGIN
    SELECT * INTO r FROM hospital_requests WHERE id=p_id FOR UPDATE;
    IF NOT FOUND THEN RETURN 'Request not found'; END IF;
    IF r.status <> 'PENDING' THEN RETURN 'Request already resolved: ' || r.status; END IF;
    IF p_issue THEN
        UPDATE stock SET units=units-r.units
          WHERE blood_group=r.blood_group AND units>=r.units;
        IF NOT FOUND THEN RETURN 'Insufficient stock. Request remains pending.'; END IF;
        UPDATE hospital_requests SET status='ISSUED', resolved_at=CURRENT_TIMESTAMP WHERE id=p_id;
        RETURN 'Issued ' || r.units || ' units';
    END IF;
    UPDATE hospital_requests SET status='CANCELLED', resolved_at=CURRENT_TIMESTAMP WHERE id=p_id;
    RETURN 'Request cancelled';
END;
$$;

CREATE VIEW donor_interval_report AS
SELECT d.id,d.name,d.blood_group,MAX(n.donated_on) AS last_donation,
       s.interval_days, MAX(n.donated_on)+s.interval_days AS demo_interval_end,
       CASE WHEN MAX(n.donated_on) IS NULL THEN 'NO HISTORY'
            WHEN MAX(n.donated_on)+s.interval_days <= CURRENT_DATE THEN 'INTERVAL MET'
            ELSE 'INTERVAL NOT MET' END AS interval_status
FROM donors d CROSS JOIN demo_settings s LEFT JOIN donations n ON n.donor_id=d.id
GROUP BY d.id,s.interval_days;
COMMIT;
