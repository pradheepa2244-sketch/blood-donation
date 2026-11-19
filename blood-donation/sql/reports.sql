SELECT blood_group,units FROM stock ORDER BY blood_group;
SELECT n.id,n.donor_id,d.name,n.blood_group,n.donated_on,n.units
FROM donations n JOIN donors d ON d.id=n.donor_id ORDER BY n.donated_on DESC,n.id;
SELECT g.code,COUNT(n.id) AS donations,COALESCE(SUM(n.units),0) AS units
FROM blood_groups g LEFT JOIN donations n ON n.blood_group=g.code
GROUP BY g.code ORDER BY g.code;
SELECT * FROM donor_interval_report ORDER BY id;
SELECT id,hospital,blood_group,units,status,requested_at,resolved_at FROM hospital_requests ORDER BY id;
