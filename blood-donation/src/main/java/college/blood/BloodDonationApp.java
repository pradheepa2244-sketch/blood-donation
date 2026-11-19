package college.blood;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Persistence;
import jakarta.persistence.Query;
import java.io.Console;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

public class BloodDonationApp {
    private static final String MENU = """
        1 List donors
        2 Add donor
        3 Update donor name, age and contact
        4 Record donation
        5 Show stock
        6 Create hospital request
        7 Issue units for request
        8 Cancel pending request
        9 List hospital requests
        10 Donor interval report
        11 Donation history
        12 Donations by blood group
        0 Exit
        """;

    public static void main(String[] args) {
        Console console = System.console();
        if (console == null) {
            System.err.println("Run in the VS Code integrated terminal.");
            return;
        }
        char[] password = console.readPassword("PostgreSQL password for postgres: ");
        if (password == null) return;
        try (var factory = Persistence.createEntityManagerFactory("blood-donation",
                Map.of("jakarta.persistence.jdbc.password", new String(password)))) {
            Arrays.fill(password, '\0');
            while (true) {
                System.out.println(MENU);
                String choice = console.readLine("Choice: ");
                if (choice == null || choice.strip().equals("0")) return;
                try (var db = factory.createEntityManager()) {
                    runChoice(db, console, choice.strip());
                } catch (RuntimeException ex) {
                    System.err.println("Operation failed: " + ex.getMessage());
                }
            }
        } catch (RuntimeException ex) {
            System.err.println("Could not start the app: " + ex.getMessage());
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private static void runChoice(EntityManager db, Console c, String choice) {
        switch (choice) {
            case "1" -> {
                List<Donor> donors = db.createQuery("SELECT d FROM Donor d ORDER BY d.id", Donor.class).getResultList();
                System.out.println("ID | Name | Age | Contact | Blood group");
                donors.forEach(System.out::println);
            }
            case "2" -> write(db, "INSERT INTO donors(name,age,contact,blood_group) VALUES (?1,?2,?3,?4)",
                    text(c,"Name"), count(c,"Age"),text(c,"Contact"),group(c));
            case "3" -> {
                long id = number(c,"Donor ID");
                write(db,"UPDATE donors SET name=?1,age=?2,contact=?3 WHERE id=?4",
                        text(c,"Name"),count(c,"Age"),text(c,"Contact"),id);
            }
            case "4" -> {
                long id = number(c,"Donor ID");
                LocalDate date = LocalDate.parse(text(c,"Donation date YYYY-MM-DD"));
                int units = count(c,"Units");
                Object result = transaction(db, () -> query(db,"SELECT record_donation(?1,CAST(?2 AS date),?3)",
                        id,date.toString(),units).getSingleResult());
                System.out.println("Donation recorded. ID: " + result);
            }
            case "5" -> report(db,"Group | Units","SELECT blood_group,units FROM stock ORDER BY blood_group");
            case "6" -> write(db,"INSERT INTO hospital_requests(hospital,blood_group,units) VALUES (?1,?2,?3)",
                    text(c,"Hospital"),group(c),count(c,"Units"));
            case "7", "8" -> {
                long id = number(c,"Request ID");
                Object result = transaction(db, () -> query(db,"SELECT resolve_request(?1,?2)",
                        id,choice.equals("7")).getSingleResult());
                System.out.println(result);
            }
            case "9" -> report(db,"ID | Hospital | Group | Units | Status",
                    "SELECT id,hospital,blood_group,units,status FROM hospital_requests ORDER BY id");
            case "10" -> report(db,"ID | Name | Group | Last date | Demo days | Interval end | Status",
                    "SELECT * FROM donor_interval_report ORDER BY id");
            case "11" -> report(db,"ID | Donor | Group | Date | Units","""
                    SELECT n.id,d.name,n.blood_group,n.donated_on,n.units FROM donations n
                    JOIN donors d ON d.id=n.donor_id ORDER BY n.donated_on DESC,n.id
                    """);
            case "12" -> report(db,"Group | Donation count | Units","""
                    SELECT g.code,COUNT(n.id),COALESCE(SUM(n.units),0)
                    FROM blood_groups g LEFT JOIN donations n ON n.blood_group=g.code
                    GROUP BY g.code ORDER BY g.code
                    """);
            default -> System.out.println("Enter a choice from 0 to 12.");
        }
    }

    private static Query query(EntityManager db,String sql,Object... args) {
        Query q = db.createNativeQuery(sql);
        for (int i=0;i<args.length;i++) q.setParameter(i+1,args[i]);
        return q;
    }

    private static <T> T transaction(EntityManager db, Supplier<T> work) {
        var tx = db.getTransaction();
        tx.begin();
        try {
            T result = work.get();
            tx.commit();
            return result;
        } catch (RuntimeException ex) {
            if (tx.isActive()) tx.rollback();
            throw ex;
        }
    }

    private static void write(EntityManager db,String sql,Object... args) {
        int changed = transaction(db, () -> query(db,sql,args).executeUpdate());
        System.out.println(changed == 0 ? "Record not found." : "Saved.");
    }

    private static void report(EntityManager db,String header,String sql) {
        System.out.println(header);
        var rows = query(db,sql).getResultList();
        if (rows.isEmpty()) System.out.println("No records.");
        for (Object row : rows) {
            System.out.println(String.join(" | ",Arrays.stream((Object[])row)
                    .map(v -> v == null ? "-" : v.toString()).toList()));
        }
    }

    private static String text(Console c,String label) {
        String value = c.readLine("%s: ",label);
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required.");
        return value.strip();
    }

    private static String group(Console c) { return text(c,"Blood group").toUpperCase(Locale.ROOT); }

    private static long number(Console c,String label) {
        long value = Long.parseLong(text(c,label));
        if (value <= 0) throw new IllegalArgumentException(label + " must be positive.");
        return value;
    }

    private static int count(Console c,String label) { return Math.toIntExact(number(c,label)); }
}
