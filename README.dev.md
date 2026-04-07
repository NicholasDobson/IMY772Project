# A README for us devs 
---

## Running the application 
---

### Database 
Ensure:
- `docker-compose.yml` exists in root directory 
- `application.yml` exists in `src/main/resources/` 
DB credentials:
```bash
url: jdbc:postgresql://localhost:5432/amr_dashboard
container_name: amr-postgres
POSTGRES_DB: amr_dashboard
POSTGRES_USER / username: postgres
POSTGRES_PASSWORD / password: postgres
Port: 5432
```

**Running** the database:
```bash
docker-compose up -d
```

**Viewing database**:
1. go to `http://localhost:5050` 
2. Log in using credentials from docker:
```
user: admin@tuks.co.za
pass: admin
```
3. Click "Add new server" 
    - General tab: Name it "AMR Local" 
    - Connection tab: 
        - Host name/address: `postgres`
        - Port: 5432
        - Maintenance database: `amr_dashboard` 
        - Username: `postgres` 
        - Password: `postgres`
4. Save and expand the tree on the left `Servers > AMR Local > Databases > amr_dashboard > Schemas > public > Tables`
5. Right-clik a table -> View/Edit Data -> All Rows

### Backend
In `backend/` run:
```bash
./mvnw spring-boot:run
```
If on **Windows**:
```bash
mvnw.cmd spring-boot:run
```
*^I Think :/*

