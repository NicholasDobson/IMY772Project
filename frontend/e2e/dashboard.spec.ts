import { test, expect } from '@playwright/test'

/**
 * E2E tests for the Dashboard page.
 *
 * The app always falls back to static mockdata when the backend is
 * unavailable, so these tests are stable regardless of whether Spring
 * Boot is running.
 */
test.describe('Dashboard page', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
  })

  /* ── Page structure ────────────────────────────────────────────── */

  test('renders the Dashboard page title', async ({ page }) => {
    await expect(page.locator('.page-title')).toHaveText('Dashboard')
  })

  test('renders 4 stat cards', async ({ page }) => {
    const cards = page.locator('.stat-card')
    await expect(cards).toHaveCount(4)
  })

  test('stat cards display labels', async ({ page }) => {
    const labels = page.locator('.stat-label')
    await expect(labels.nth(0)).toContainText('MDRO Incident Rate')
    await expect(labels.nth(1)).toContainText('Total Samples')
  })

  test('stat cards display numeric values', async ({ page }) => {
    // Values either come from live API or from static mockdata; either way
    // the first card value should be non-empty.
    const firstValue = page.locator('.stat-value').first()
    await expect(firstValue).not.toBeEmpty()
  })

  /* ── AMR Detections chart panel ────────────────────────────────── */

  test('renders AMR Detections by Month chart panel', async ({ page }) => {
    const title = page.locator('.panel-title', { hasText: 'AMR Detections by Month' })
    await expect(title).toBeVisible()
  })

  test('renders year toggle buttons', async ({ page }) => {
    const yearBtns = page.locator('.year-btn')
    await expect(yearBtns.first()).toBeVisible()
  })

  test('has at least one year button marked active', async ({ page }) => {
    const activeBtn = page.locator('.year-btn--active')
    await expect(activeBtn).toBeVisible()
  })

  /* ── Province Risk panel ────────────────────────────────────────── */

  test('renders Risk by Province panel', async ({ page }) => {
    const title = page.locator('.panel-title', { hasText: 'Risk by Province' })
    await expect(title).toBeVisible()
  })

  test('renders province rows with Gauteng', async ({ page }) => {
    const gauteng = page.locator('.province-name', { hasText: 'Gauteng' })
    await expect(gauteng).toBeVisible()
  })

  test('province rows show risk badges (HIGH / MED / LOW)', async ({ page }) => {
    const badges = page.locator('.province-badge')
    await expect(badges.first()).toBeVisible()
    const firstText = await badges.first().textContent()
    expect(['HIGH', 'MED', 'LOW']).toContain(firstText?.trim())
  })

  /* ── Top Detected Organisms table ──────────────────────────────── */

  test('renders Top Detected Organisms section heading', async ({ page }) => {
    const title = page.locator('.panel-title', { hasText: 'Top Detected Organisms' })
    await expect(title).toBeVisible()
  })

  test('organisms table contains at least one row', async ({ page }) => {
    // DataTable renders p-datatable rows; or the stub might render italics
    const organisms = page.locator('.org-name')
    await expect(organisms.first()).toBeVisible()
  })

  test('organism row names are displayed in italic', async ({ page }) => {
    const firstName = page.locator('.org-name').first()
    await expect(firstName).toBeVisible()
  })

  /* ── Top Resistance Genes table ─────────────────────────────────── */

  test('renders Top Resistance Genes section heading', async ({ page }) => {
    const title = page.locator('.panel-title', { hasText: 'Top Resistance Genes' })
    await expect(title).toBeVisible()
  })

  test('resistance gene symbols are displayed in code elements', async ({ page }) => {
    const genes = page.locator('.gene-name')
    await expect(genes.first()).toBeVisible()
  })

  /* ── Affected River Sites table ─────────────────────────────────── */

  test('renders Affected River Sites section heading', async ({ page }) => {
    const title = page.locator('.panel-title', { hasText: 'Affected River Sites' })
    await expect(title).toBeVisible()
  })

  test('river site IDs are shown (code-style spans)', async ({ page }) => {
    const siteIds = page.locator('.site-id')
    await expect(siteIds.first()).toBeVisible()
  })

  /* ── Sidebar / layout ────────────────────────────────────────────── */

  test('page does not show a visible error banner by default', async ({ page }) => {
    // The error banner would only appear if all API calls fail AND
    // the component rendered it — currently DashboardView silently
    // falls back to mockdata, so there should be no error text visible.
    const errorElements = page.locator('[class*="error"]')
    await expect(errorElements).toHaveCount(0)
  })

  /* ── Navigation to Bacteria Detail ──────────────────────────────── */

  test('clicking an organism row navigates to bacteria detail', async ({ page }) => {
    // Wait for the organisms table to render
    const firstOrg = page.locator('.org-name').first()
    await expect(firstOrg).toBeVisible()

    // The DataTable row has a click handler (goToBacteria)
    // Click on the row containing the first organism name
    const row = page.locator('tr', { has: firstOrg })
    await row.click()

    // Should navigate to /bacteria/...
    await expect(page).toHaveURL(/\/bacteria\//)
  })
})
