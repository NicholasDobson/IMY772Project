import { test, expect } from '@playwright/test'
import { ORGANISM_DB } from '../src/data/organisms'

/**
 * E2E tests for the Bacteria Detail page.
 *
 * All data is drawn from the static ORGANISM_DB, so tests run without
 * a backend.  We primarily test with Escherichia coli (the default /
 * most data-rich entry) and verify a second organism to confirm routing.
 */
test.describe('Bacteria Detail page — Escherichia coli', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/bacteria/Escherichia%20coli')
  })

  /* ── Header / identity ────────────────────────────────────────── */

  test('renders the organism name in the heading', async ({ page }) => {
    const name = page.locator('.organism-name')
    await expect(name).toContainText('Escherichia coli')
  })

  test('renders the gram stain meta pill', async ({ page }) => {
    const gram = page.locator('.gram-pill')
    await expect(gram).toHaveText('Gram-negative')
  })

  test('renders the common name meta pill', async ({ page }) => {
    const pills = page.locator('.meta-pill')
    // Wait for at least one pill to be visible before reading text contents
    await expect(pills.first()).toBeVisible()
    const texts = await pills.allTextContents()
    expect(texts.some((t) => t.includes('E. coli'))).toBe(true)
  })

  test('renders the organism description paragraph', async ({ page }) => {
    const desc = page.locator('.organism-desc')
    await expect(desc).toBeVisible()
    await expect(desc).not.toBeEmpty()
  })

  /* ── Back button ─────────────────────────────────────────────── */

  test('renders a back-to-Dashboard button', async ({ page }) => {
    const btn = page.locator('.back-btn')
    await expect(btn).toBeVisible()
    await expect(btn).toContainText('Dashboard')
  })

  test('back button navigates to the dashboard', async ({ page }) => {
    await page.locator('.back-btn').click()
    await expect(page).toHaveURL('/')
    await expect(page.locator('.page-title')).toHaveText('Dashboard')
  })

  /* ── KPI cards ────────────────────────────────────────────────── */

  test('renders 4 KPI cards', async ({ page }) => {
    const cards = page.locator('.kpi-card')
    await expect(cards).toHaveCount(4)
  })

  test('total detections KPI shows a number', async ({ page }) => {
    const firstValue = page.locator('.kpi-value').first()
    await expect(firstValue).not.toBeEmpty()
    // E. coli has 1,547 detections
    await expect(firstValue).toContainText('1,547')
  })

  test('MDRO rate KPI shows a percentage', async ({ page }) => {
    // Third KPI card (.kpi-value--red) is resistance rate
    const rateValue = page.locator('.kpi-value--red')
    await expect(rateValue).toContainText('%')
  })

  test('resistance profile KPI shows R / I / S pills', async ({ page }) => {
    await expect(page.locator('.ris-r')).toBeVisible()
    await expect(page.locator('.ris-i')).toBeVisible()
    await expect(page.locator('.ris-s')).toBeVisible()
  })

  test('R pill label starts with "R"', async ({ page }) => {
    const rPill = page.locator('.ris-r')
    const text = await rPill.textContent()
    expect(text?.trim().startsWith('R')).toBe(true)
  })

  /* ── Panel sections ───────────────────────────────────────────── */

  test('renders Detection Trend chart panel', async ({ page }) => {
    const title = page.locator('.panel-title', { hasText: 'Detection Trend' })
    await expect(title).toBeVisible()
  })

  test('renders Resistance by Antibiotic Class chart panel', async ({ page }) => {
    const title = page.locator('.panel-title', { hasText: 'Resistance by Antibiotic Class' })
    await expect(title).toBeVisible()
  })

  test('renders Antibiotic Resistance Profile section', async ({ page }) => {
    const title = page.locator('.panel-title', { hasText: 'Antibiotic Resistance Profile' })
    await expect(title).toBeVisible()
  })

  test('renders AMR Resistance Genes table panel', async ({ page }) => {
    const title = page.locator('.panel-title', { hasText: 'AMR Resistance Genes' })
    await expect(title).toBeVisible()
  })

  test('AMR genes subtitle shows correct gene count', async ({ page }) => {
    const ecoli = ORGANISM_DB['Escherichia coli']
    const subtitle = page.locator('.panel-subtitle', { hasText: 'genes detected' })
    await expect(subtitle).toContainText(`${ecoli.genes.length} genes detected`)
  })

  test('renders AMR gene symbol code elements', async ({ page }) => {
    const geneNames = page.locator('.gene-name')
    await expect(geneNames.first()).toBeVisible()
    await expect(geneNames.first()).toContainText('bla') // E. coli top gene starts with bla
  })

  test('renders Affected River Sites panel', async ({ page }) => {
    const title = page.locator('.panel-title', { hasText: 'Affected River Sites' })
    await expect(title).toBeVisible()
  })

  test('river site rows show site IDs', async ({ page }) => {
    const siteIds = page.locator('.site-id')
    await expect(siteIds.first()).toBeVisible()
  })

  test('renders WGS / Genomic Metrics panel', async ({ page }) => {
    const title = page.locator('.panel-title', { hasText: 'WGS / Genomic Metrics' })
    await expect(title).toBeVisible()
  })
})

/* ── Second organism — routing sanity ──────────────────────────── */
test.describe('Bacteria Detail page — Klebsiella pneumoniae', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/bacteria/Klebsiella%20pneumoniae')
  })

  test('renders organism name for Klebsiella pneumoniae', async ({ page }) => {
    const name = page.locator('.organism-name')
    await expect(name).toContainText('Klebsiella pneumoniae')
  })

  test('renders different detection count than E. coli', async ({ page }) => {
    const klebsiella = ORGANISM_DB['Klebsiella pneumoniae']
    const firstValue = page.locator('.kpi-value').first()
    await expect(firstValue).toContainText(klebsiella.detectionCount.toLocaleString())
  })
})

/* ── Fallback for unknown organism ─────────────────────────────── */
test.describe('Bacteria Detail page — unknown organism fallback', () => {
  test('falls back to E. coli data for an unrecognised organism name', async ({ page }) => {
    await page.goto('/bacteria/Unknown%20Organism%20XYZ')
    // The heading always shows the route param; but the description and gram
    // stain are drawn from the E. coli fallback in ORGANISM_DB.
    await expect(page.locator('.gram-pill')).toHaveText('Gram-negative')
    await expect(page.locator('.organism-desc')).toContainText('β-lactamase')
  })
})

/* ── Dashboard → Bacteria Detail navigation flow ───────────────── */
test.describe('Cross-page navigation', () => {
  test('navigating from dashboard organism row reaches bacteria detail', async ({ page }) => {
    await page.goto('/')

    // Click on the first organism name in the organisms table
    const firstOrg = page.locator('.org-name').first()
    await expect(firstOrg).toBeVisible()
    const orgName = await firstOrg.textContent()

    // Use .first() on the outer locator to avoid strict-mode violation
    // (all rows "have" a .org-name descendant when .first() is used inside has)
    const row = page.locator('tr', { has: page.locator('.org-name') }).first()
    await row.click()

    await expect(page).toHaveURL(/\/bacteria\//)
    // After navigation, the organism heading should match what was clicked
    await expect(page.locator('.organism-name')).toContainText(orgName?.trim() ?? '')
  })
})
