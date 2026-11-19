# Blood Donation Management and Donor Availability Analysis

Scenario 15 for register numbers 15, 45, 75 and 105. This classroom project uses synthetic records. The configurable 30-day interval is an arbitrary demo rule for checking date arithmetic. A matching record only meets that demo interval. Real donor eligibility and clinical compatibility are outside this project's scope.

## Check the tools on Windows

- Open pgAdmin and connect to PostgreSQL. If missing, use the [PostgreSQL Windows installer](https://www.postgresql.org/download/windows/) and keep PostgreSQL Server and pgAdmin selected. The project expects localhost and port 5432.
- In VS Code's terminal, run `javac -version`. If missing, install [Temurin JDK 21](https://adoptium.net/temurin/releases/?version=21), choose Add to PATH, and reopen VS Code. Under Extensions, install Microsoft's Extension Pack for Java if needed.
- Run `python --version`. If missing, install Python from [python.org](https://www.python.org/downloads/windows/) and reopen VS Code.

## Run the project

1. In pgAdmin, right-click Databases and create `blood_donation`. Open Query Tool under that database. Open and execute `sql/schema.sql`, then `sql/sample_data.sql`, once each. Use the folder icon to open each file. Run queries from `sql/reports.sql` individually to capture reports.
2. In VS Code, open the extracted `blood-donation` folder containing pom.xml. Let Maven import the project. Open `src/main/java/college/blood/BloodDonationApp.java` and click Run above main. Enter your PostgreSQL password in the integrated terminal. The password is hidden as you type.
3. Choose 5 to see stock, 9 for request IDs, and 10 for the demo interval report. Use the test plan in docs/Software_Engineering.docx to try issuing units, checking insufficient stock, and recording a new donation.
4. Exit with 0. In the VS Code terminal run:
   ```powershell
   python -m pip install -r analysis/requirements.txt
   python analysis/donation_charts.py
   ```
   Enter your database password. Three PNGs are saved in analysis/output.
5. Record actual tests in the Word document. Put SQL and Java screenshots in evidence. To submit, upload the extracted project files including charts and evidence. Exclude target and the ZIP. Keep passwords and real donor information out of the repository.

If your database username or port differs, adjust persistence.xml and donation_charts.py. Issuing uses the requested group and updates stock and request status together. Donation records store the group recorded at the time of donation.

## Included examples
docs/SRS.docx follows the Unit 2 structured form for each of the seven requirements. samples/stock_example.txt gives expected results for the initial SQL fixtures. The three included charts were generated with the packaged Python chart function from the sample SQL values using 18 November 2026 as the illustrative reference date. Running the SQL seed on another date can shift the monthly labels. Run the Java and SQL acceptance checks locally and record your actual results.
