FOODLOOP 3.0 - Food Waste Redistribution System


STACK
- Java 17+
- Maven
- Embedded Tomcat 10.1 / Jakarta Servlet
- SQLite
- BCrypt
- HTML/CSS/JavaScript

RUN
1. Open this folder in VS Code.
2. Make sure Java and Maven are installed.
3. Run: mvn clean compile
4. Run: mvn exec:java
5. Open: http://localhost:8080/

DEMO ACCOUNTS
Donor: donor@foodrescue.org / password123
Recipient: ngo@foodrescue.org / password123
Volunteer: volunteer@foodrescue.org / password123
Admin: admin@foodrescue.org / password123

FLOW
Donor publishes food -> Recipient claims -> Donor sees recipient details -> Donor accepts/rejects -> Donor chooses available volunteer -> enters distance and rate -> payout calculated -> Volunteer accepts/picks up/goes on way/delivers -> recipient sees delivery status.

VOLUNTEER PAYOUT
Payout = distance_km * rate_per_km. Default demo rate is Rs.15/km.

NOTES
- Uploaded images are saved under src/main/webapp/uploads.
- SQLite database file is foodloop.db in the project root.
- Delivery distance is entered manually for a reliable offline demo; this avoids requiring a maps API.
