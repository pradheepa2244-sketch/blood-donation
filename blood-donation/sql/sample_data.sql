BEGIN;
INSERT INTO donors(name,age,contact,blood_group) VALUES
 ('Demo Asha',28,'asha@example.test','A+'),
 ('Demo Ravi',33,'ravi@example.test','O+'),
 ('Demo Mina',25,'mina@example.test','B+'),
 ('Demo Dev',36,'dev@example.test','AB-'),
 ('Demo Noor',29,'noor@example.test','O-');
SELECT record_donation(d.id,CURRENT_DATE - x.days_ago,x.units)
FROM (VALUES ('asha@example.test',75,2),('asha@example.test',8,1),
             ('ravi@example.test',64,2),('ravi@example.test',34,1),
             ('mina@example.test',48,1),('mina@example.test',4,1),
             ('dev@example.test',38,1)) AS x(contact,days_ago,units)
JOIN donors d ON d.contact=x.contact;
INSERT INTO hospital_requests(hospital,blood_group,units) VALUES
 ('Demo Hospital North','O+',2),('Demo Hospital South','AB-',5);
COMMIT;
