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

**Clearing** the database:
```bash
docker-compose down -v
```

**Viewing database**:
Use a app like PgAdmin or DBeaver to and enter the credentials as necessary.
If you don't have an app for viewing postgreSQL data, do the following:

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

To wipe the old compiled files (do this after adding new dependencies/libraries):
```bash
./mvnw clean
```

---

### Frontend

The frontend is a **Vue 3 + TypeScript + Pinia + PrimeVue** app located in `frontend/`.

It requires **Node.js `>=22.12.0`**. The packages will not install or run correctly on older versions (e.g. Node 20.13).

---

#### Step 1 — Install Node 22 via fnm

[fnm](https://github.com/Schniz/fnm) (Fast Node Manager) is the recommended way to manage Node versions on this project.

**Mac:**
```bash
brew install fnm
echo 'eval "$(fnm env --use-on-cd)"' >> ~/.zshrc
source ~/.zshrc
fnm install 22
fnm use 22
node --version   # should print v22.x.x
```

**Windows (PowerShell):**
```powershell
winget install Schniz.fnm
# Add to your PowerShell profile (~\Documents\PowerShell\Microsoft.PowerShell_profile.ps1):
fnm env --use-on-cd | Out-String | Invoke-Expression
# Then in a new terminal:
fnm install 22
fnm use 22
node --version   # should print v22.x.x
```

> If you're on Windows and PowerShell profiles are blocked, run `Set-ExecutionPolicy RemoteSigned -Scope CurrentUser` first.

---

#### Step 2 — Install dependencies

From the repo root or from inside `frontend/`:

```bash
cd frontend
npm install
```

No errors should appear. `EBADENGINE` warnings mean you are on the wrong Node version — go back to Step 1.

---

#### Step 3 — VS Code setup (one-time)

`.vscode/` is gitignored, so you need to create the settings file manually. This points VS Code at the workspace TypeScript inside `frontend/node_modules` so it can resolve `@vue/tsconfig`, `@types/node`, and `@types/jsdom` correctly.

Create `.vscode/settings.json` in the **repo root** with the following content:

```json
{
  "typescript.tsdk": "frontend/node_modules/typescript/lib",
  "typescript.enablePromptUseWorkspaceTsdk": true,
  "vue.server.hybridMode": true
}
```

Then select the workspace TypeScript version once:

1. Open the Command Palette: `Cmd+Shift+P` (Mac) / `Ctrl+Shift+P` (Windows)
2. Search for: **TypeScript: Select TypeScript Version**
3. Choose **Use Workspace Version**

Without this, VS Code will show errors in the tsconfig files even though the build and dev server work fine.

---

#### Running scripts

All scripts are run from inside the `frontend/` directory.

| Command | What it does |
|---|---|
| `npm run dev` | Start dev server at `http://localhost:5173` |
| `npm run build` | Type-check + production build (output in `dist/`) |
| `npm run preview` | Serve the production build locally |
| `npm run type-check` | Run `vue-tsc` type checking only |
| `npm run test:unit` | Run unit tests with Vitest |
| `npm run test:e2e` | Run end-to-end tests with Playwright |
| `npm run lint` | Lint and auto-fix with oxlint + eslint |
| `npm run format` | Format source files with Prettier |

---

#### Tech stack

| Package | Purpose |
|---|---|
| Vue 3 | UI framework |
| TypeScript | Type safety |
| Pinia | State management |
| Vue Router 5 | Client-side routing |
| PrimeVue 4 | Component library |
| `@primeuix/themes` | PrimeVue theme presets (Aura used) |
| primeicons | Icon set |
| Vite 8 | Dev server and bundler |
| Vitest | Unit testing |
| Playwright | End-to-end testing |

---

#### Troubleshooting

**`EBADENGINE` on `npm install`**  
Wrong Node version. Run `fnm use 22` and retry.

**`Cannot find native binding` / Rolldown error on `npm run dev`**  
`node_modules` was installed under the wrong Node version. Fix:
```bash
rm -rf node_modules package-lock.json
npm install
```

**VS Code still shows tsconfig errors after Step 3**  
Reload the VS Code window: `Cmd+Shift+P` → **Developer: Reload Window**.